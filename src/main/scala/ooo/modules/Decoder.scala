package ooo.modules

import chisel3._
import chisel3.util.{Decoupled, MuxCase, RegEnable, Valid}
import ooo.Configuration
import ooo.Types.EventType.CompletionWithValue
import ooo.Types.Immediate.InstructionFieldExtractor
import ooo.Types.{ArchRegisterId, Event, EventType, Immediate, InstructionPackage, InstructionType, IssuePackage, Opcode, PhysRegisterId}
import ooo.util.{BundleExpander, LookUp, PairConnector, SeqDataExtension}
import ooo.Types.EventType._
import ooo.Types.Opcode.{branch, store, system}

object Decoder {

  class RetirementPort(implicit c: Configuration) extends Bundle {
    val stateUpdate = Flipped(Valid(new Bundle {
      val pr = PhysRegisterId()
      val rd = ArchRegisterId()
    }))
    val allocationCheck = new Bundle {
      val pr = Input(PhysRegisterId())
      val isAllocated = Output(Bool())
    }
  }

}


class Decoder()(implicit c: Configuration) extends Module {

  val io = IO(new Bundle {
    val instructionStream = Flipped(Decoupled(new InstructionPackage))
    val issueStream = Decoupled(new IssuePackage)

    val allocationPorts = new Bundle {
      val physRegisterId = Flipped(new IdAllocator.AllocationPort(c.physRegisterIdWidth))
      val snapshotId = Flipped(new IdAllocator.AllocationPort(c.snapshotIdWidth))
    }
    val retirementPort = new Decoder.RetirementPort
    val robPort = Flipped(new ReorderBuffer.DecoderPort)
    val eventBus = Flipped(Valid(new Event))
  })

  val debug = if(c.simulation) Some(IO(Output(new Bundle {
    val state = Vec(32, PhysRegisterId())
    val spec = Vec(32, PhysRegisterId())
  }))) else None

  val mapSelector = Module(new MapSelector)
  val specArch2Phys = Module(new SpeculativeArch2PhysMap)
  val stateArch2Phys = Module(new StateArch2PhysMap)

  if(c.simulation) {
    debug.get.state := stateArch2Phys.debug.get
    debug.get.spec := specArch2Phys.debug.get
  }


  val instruction = io.instructionStream.bits.instruction
  val opcode = instruction.opcode
  val instructionType = instruction.instructionType
  val rs = VecInit(instruction(19, 15), instruction(24, 20))
  val rd = instruction(11, 7)
  val funct3 = instruction(14, 12)
  val funct7 = instruction(31, 25)

  val immediate = LookUp(instructionType, instruction.immediate.iType,
    InstructionType.U -> instruction.immediate.uType,
    InstructionType.S -> instruction.immediate.sType,
    InstructionType.B -> instruction.immediate.bType,
    // InstructionType.J -> instruction.immediate.jType -> direct jumps are already handled
  )

  val hasToStall = WireDefault(0.B)
  val insertBubble = WireDefault(0.B)
  val hasValidInstruction = io.instructionStream.valid
  val allowedToProgress = !(hasToStall || insertBubble) && hasValidInstruction

  val usesSnapshots = opcode.isOneOf(Opcode.branch, Opcode.jalr)

  mapSelector.io.read.rs := rs
  specArch2Phys.io.read.rs := rs
  stateArch2Phys.io.read.rs := rs

  specArch2Phys.io.write.expand(
    _.prd := io.allocationPorts.physRegisterId.id,
    _.rd := rd,
    _.write := allowedToProgress && !opcode.isOneOf(branch,store,system) && rd =/= 0.U
  )

  stateArch2Phys.io.write.expand(
    _.prd := io.retirementPort.stateUpdate.bits.pr,
    _.rd := io.retirementPort.stateUpdate.bits.rd,
    _.write := io.retirementPort.stateUpdate.valid && io.retirementPort.stateUpdate.bits.rd =/= 0.U
  )
  io.robPort.allocSetup.expand(
    _.update := allowedToProgress,
    _.prd := io.allocationPorts.physRegisterId.id,
    _.rd := rd,
    _.hasWriteBack := !opcode.isOneOf(branch,store,system)
  )
  io.retirementPort.allocationCheck.isAllocated := stateArch2Phys.io.allocationCheck.isAllocated(1)

  val prs = mapSelector.io.read.useSpec
    .zip(stateArch2Phys.io.read.prs)
    .zip(specArch2Phys.io.read.prs)
    .map { case ((useSpec, state), spec) => Mux(useSpec, spec, state) }

  val prsReg = RegEnable(prs.toVec, !hasToStall)

  io.robPort.prs.zip(Mux(hasToStall, prsReg, prs.toVec)).connectPairs()

  stateArch2Phys.io.allocationCheck.pr := VecInit(io.allocationPorts.physRegisterId.id, io.retirementPort.allocationCheck.pr)
  val doubleAllocation = stateArch2Phys.io.allocationCheck.isAllocated(0)

  io.allocationPorts.physRegisterId.take := Mux(doubleAllocation, 1.B, allowedToProgress) // when double allocation we want to take the id no matter what
  io.allocationPorts.snapshotId.take := usesSnapshots && allowedToProgress

  insertBubble := MuxCase(0.B, Seq(
    (io.eventBus.valid && io.eventBus.bits.eventType.isOneOf(Branch, Jump, Exception)) -> 1.B,
    !io.allocationPorts.physRegisterId.offer -> 1.B, // no more phys ids to allocate
    doubleAllocation -> 1.B, // the currently available id is already allocated
    (!io.allocationPorts.snapshotId.offer && usesSnapshots) -> 1.B,
  ))

  specArch2Phys.io.save.expand(
    _.snapshotId := io.allocationPorts.snapshotId.id,
    _.takeSnapshot := usesSnapshots && allowedToProgress
  )

  specArch2Phys.io.restore.expand(
    _.snapshotId := io.eventBus.bits.snapshotId,
    _.restoreSnapshot := io.eventBus.valid && ((io.eventBus.bits.eventType.isOneOf(EventType.Branch) && io.eventBus.bits.misprediction) || io.eventBus.bits.eventType.isOneOf(EventType.Jump))
  )

  mapSelector.io.clear := io.eventBus.valid && io.eventBus.bits.eventType === EventType.Exception
  mapSelector.io.update.expand(
    _.rd := rd,
    _.markAsSpec := allowedToProgress && rd =/= 0.U
  )

  val outReg = Reg(chiselTypeOf(io.issueStream.bits))
  val validReg = RegInit(0.B)

  hasToStall := validReg && !io.issueStream.ready


  when(!hasToStall) {
    validReg := Mux(insertBubble, 0.B, hasValidInstruction)

    outReg.prs.map(_.id).zip(prs).connectPairs()

    outReg.expand(
      _.opcode := opcode,
      _.func := funct7(5) ## funct3,
      _.prd := io.allocationPorts.physRegisterId.id,
      _.immediate := immediate.asUInt,
      _.pc := io.instructionStream.bits.pc,
      _.snapshotId := io.allocationPorts.snapshotId.id,
      _.branchPrediction := io.instructionStream.bits.branchPrediction
    )
  }

  val useSpecMapReg = RegEnable(mapSelector.io.read.useSpec, VecInit(0.B, 0.B), !hasToStall)

  io.issueStream.bits := outReg
  io.issueStream.bits.prs(0).ready := Mux(RegNext(opcode.isOneOf(Opcode.jal, Opcode.lui, Opcode.auipc), 0.B), 1.B, !useSpecMapReg(0) || (useSpecMapReg(0) && io.robPort.ready(0)))
  io.issueStream.bits.prs(1).ready := Mux(RegNext(opcode.isOneOf(Opcode.load, Opcode.immediate, Opcode.auipc, Opcode.lui, Opcode.jalr, Opcode.jal), 0.B), 1.B, !useSpecMapReg(1) || (useSpecMapReg(1) && io.robPort.ready(1)))

  io.instructionStream.ready := allowedToProgress
  io.issueStream.valid := validReg
}