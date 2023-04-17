package ooo.modules

import chisel3._
import chisel3.util.{Decoupled, MuxCase, Valid}
import ooo.Configuration
import ooo.Types.Immediate.InstructionFieldExtractor
import ooo.Types.{ArchRegisterId, Event, EventType, InstructionPackage, InstructionType, IssuePackage, MicroOp, Opcode, PhysRegisterId}
import ooo.util.{BundleExpander, LookUp, PairConnector, SeqDataExtension}


class Decoder()(implicit c: Configuration) extends Module {

  val io = IO(new Bundle {
    val instructionStream = Flipped(Decoupled(new InstructionPackage))
    val issueStream = Decoupled(new IssuePackage)
    val instructionStatus = Vec(2, new Bundle {
      val pr = Output(PhysRegisterId())
      val ready = Input(Bool())
    })
    val allocationPorts = new Bundle {
      val physRegisterId = Flipped(new IdAllocator.AllocationPort(c.physRegisterIdWidth))
      val branchId = Flipped(new IdAllocator.AllocationPort(c.branchIdWidth))
      val loadId = Flipped(new IdAllocator.AllocationPort(c.loadIdWidth))
      val storeId = Flipped(new IdAllocator.AllocationPort(c.storeIdWidth))
    }
    val retirement = Flipped(Valid(new Bundle {
      val pr = PhysRegisterId()
      val rd = ArchRegisterId()
    }))
    val robDestinations = Valid(new Bundle {
      val pr = PhysRegisterId()
      val rd = ArchRegisterId()
    })
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


  val isBranch = opcode === Opcode.branch
  val isJump = opcode === Opcode.jalr
  val isLoad = opcode === Opcode.load
  val isStore = opcode === Opcode.store
  val isImmediate = opcode === Opcode.immediate


  mapSelector.io.read.rs := rs
  specArch2Phys.io.read.rs := rs
  stateArch2Phys.io.read.rs := rs

  specArch2Phys.io.write.expand(
    _.prd := io.allocationPorts.physRegisterId.id,
    _.rd := rd,
    _.write := !hasToStall
  )

  stateArch2Phys.io.write.expand(
    _.prd := io.retirement.bits.pr,
    _.rd := io.retirement.bits.rd,
    _.write := io.retirement.valid
  )
  io.robDestinations.expand(
    _.valid := !hasToStall,
    _.bits.pr := io.allocationPorts.physRegisterId.id,
    _.bits.rd := rd
  )

  val prs = mapSelector.io.read.useSpec
    .zip(stateArch2Phys.io.read.prs)
    .zip(specArch2Phys.io.read.prs)
    .map { case ((useSpec, state), spec) => Mux(useSpec, spec, state) }

  io.instructionStatus.map(_.pr).zip(prs).connectPairs()

  stateArch2Phys.io.allocationCheck.newPid := io.allocationPorts.physRegisterId.id
  val doubleAllocation = stateArch2Phys.io.allocationCheck.isAllocated

  io.allocationPorts.physRegisterId.take := Mux(doubleAllocation, 1.B, !hasToStall) // when double allocation we want to take the id no matter what
  io.allocationPorts.branchId.take := (isBranch || isJump) && !hasToStall
  io.allocationPorts.loadId.take := isLoad && !hasToStall
  io.allocationPorts.storeId.take := isStore && !hasToStall



  hasToStall := MuxCase(0.B, Seq(
    !io.instructionStream.valid -> 1.B, // no new instruction coming
    io.eventBus.valid -> 1.B, // block pipeline when pc change or exception occurs
    !io.issueStream.ready -> 1.B, // next stage not ready
    !io.allocationPorts.physRegisterId.offer -> 1.B, // no more phys ids to allocate
    doubleAllocation -> 1.B, // the currently available id is already allocated
    (!io.allocationPorts.branchId.offer && isBranch) -> 1.B,
    (!io.allocationPorts.loadId.offer && isLoad) -> 1.B,
    (!io.allocationPorts.storeId.offer && isStore) -> 1.B
  ))



  specArch2Phys.io.save.expand(
    _.branchId := io.allocationPorts.branchId.id,
    _.takeSnapshot := isBranch && !hasToStall
  )

  specArch2Phys.io.restore.expand(
    _.branchId := io.eventBus.bits.branchId,
    _.restoreSnapshot := io.eventBus.valid && io.eventBus.bits.eventType.isOneOf(EventType.Branch, EventType.Jump)
  )

  mapSelector.io.clear := io.eventBus.valid && io.eventBus.bits.eventType === EventType.Exception
  mapSelector.io.update.expand(
    _.rd := rd,
    _.markAsSpec := !hasToStall
  )


  val outReg = Reg(chiselTypeOf(io.issueStream.bits))
  val validReg = RegNext(!hasToStall, 0.B)

  outReg.microOp := LookUp(opcode, MicroOp.Register,
    Opcode.load -> MicroOp.Load,
    Opcode.store -> MicroOp.Store,
    Opcode.branch -> MicroOp.Branch,
    Opcode.jal -> MicroOp.Jump,
    Opcode.immediate -> MicroOp.Immediate
  )

  outReg.prs.map(_.id).zip(prs).connectPairs()


  outReg.expand(
    _.func := funct7(5) ## funct3,
    _.prd := io.allocationPorts.physRegisterId.id,
    _.immediate := immediate.asUInt,
    _.pc := io.instructionStream.bits.pc,
    _.branchId := io.allocationPorts.branchId.id,
    _.branchPrediction := io.instructionStream.bits.branchPrediction,
    _.loadId := io.allocationPorts.loadId.id,
    _.storeId := io.allocationPorts.storeId.id
  )

  io.instructionStream.ready := !hasToStall
  io.issueStream.bits := outReg
  io.issueStream.bits.prs.map(_.ready).zip(io.instructionStatus.map(_.ready)).connectPairs() // WARNING: instruction status is assumed to come from sync mem
  io.issueStream.valid := validReg


}

object Decoder extends App {
  emitVerilog(new Decoder()(Configuration.default()))
}