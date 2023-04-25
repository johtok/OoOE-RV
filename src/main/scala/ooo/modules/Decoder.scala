package ooo.modules

import chisel3._
import chisel3.util.{Decoupled, MuxCase, Valid}
import ooo.Configuration
import ooo.Types.Immediate.InstructionFieldExtractor
import ooo.Types.{ArchRegisterId, Event, EventType, InstructionPackage, InstructionType, IssuePackage, MicroOp, Opcode, PhysRegisterId}
import ooo.modules.Retirement.StateUpdate
import ooo.util.{BundleExpander, LookUp, PairConnector}


class Decoder()(implicit c: Configuration) extends Module {

  val io = IO(new Bundle {
    val instructionStream = Flipped(Decoupled(new InstructionPackage))
    val issueStream = Decoupled(new IssuePackage)

    val allocationPorts = new Bundle {
      val physRegisterId = Flipped(new IdAllocator.AllocationPort(c.physRegisterIdWidth))
      val snapshotId = Flipped(new IdAllocator.AllocationPort(c.snapshotIdWidth))
    }
    val stateUpdate = Flipped(Valid(new StateUpdate))
    val robPort = Flipped(new ReorderBuffer.DecoderPort)
    val eventBus = Flipped(Valid(new Event))
  })

  val mapSelector = Module(new MapSelector)
  val specArch2Phys = Module(new SpeculativeArch2PhysMap)
  val stateArch2Phys = Module(new StateArch2PhysMap)


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

  val usesSnapshots = opcode.isOneOf(Opcode.branch, Opcode.jalr)

  mapSelector.io.read.rs := rs
  specArch2Phys.io.read.rs := rs
  stateArch2Phys.io.read.rs := rs

  specArch2Phys.io.write.expand(
    _.prd := io.allocationPorts.physRegisterId.id,
    _.rd := rd,
    _.write := !hasToStall
  )

  stateArch2Phys.io.write.expand(
    _.prd := io.stateUpdate.bits.pr,
    _.rd := io.stateUpdate.bits.rd,
    _.write := io.stateUpdate.valid
  )
  io.robPort.allocSetup.expand(
    _.update := !hasToStall,
    _.prd := io.allocationPorts.physRegisterId.id,
    _.rd := rd
  )

  val prs = mapSelector.io.read.useSpec
    .zip(stateArch2Phys.io.read.prs)
    .zip(specArch2Phys.io.read.prs)
    .map { case ((useSpec, state), spec) => Mux(useSpec, spec, state) }

  io.robPort.prs.zip(prs).connectPairs()

  stateArch2Phys.io.allocationCheck.newPid := io.allocationPorts.physRegisterId.id
  val doubleAllocation = stateArch2Phys.io.allocationCheck.isAllocated

  io.allocationPorts.physRegisterId.take := Mux(doubleAllocation, 1.B, !hasToStall) // when double allocation we want to take the id no matter what
  io.allocationPorts.snapshotId.take := usesSnapshots && !hasToStall


  hasToStall := MuxCase(0.B, Seq(
    !io.instructionStream.valid -> 1.B, // no new instruction coming
    io.eventBus.valid -> 1.B, // block pipeline when pc change or exception occurs
    !io.issueStream.ready -> 1.B, // next stage not ready
    !io.allocationPorts.physRegisterId.offer -> 1.B, // no more phys ids to allocate
    doubleAllocation -> 1.B, // the currently available id is already allocated
    (!io.allocationPorts.snapshotId.offer && usesSnapshots) -> 1.B,
  ))

  specArch2Phys.io.save.expand(
    _.snapshotId := io.allocationPorts.snapshotId.id,
    _.takeSnapshot := usesSnapshots && !hasToStall
  )

  specArch2Phys.io.restore.expand(
    _.snapshotId := io.eventBus.bits.snapshotId,
    _.restoreSnapshot := io.eventBus.valid && io.eventBus.bits.eventType.isOneOf(EventType.Branch, EventType.Jump)
  )

  mapSelector.io.clear := io.eventBus.valid && io.eventBus.bits.eventType === EventType.Exception
  mapSelector.io.update.expand(
    _.rd := rd,
    _.markAsSpec := !hasToStall
  )

  val outReg = Reg(chiselTypeOf(io.issueStream.bits))
  val validReg = RegNext(!hasToStall, 0.B)

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

  io.instructionStream.ready := !hasToStall
  io.issueStream.bits := outReg
  io.issueStream.bits.prs.map(_.ready).zip(io.robPort.ready).connectPairs() // WARNING: instruction status is assumed to come from sync mem
  io.issueStream.valid := validReg


}

object Decoder extends App {
  emitVerilog(new Decoder()(Configuration.default()))
}