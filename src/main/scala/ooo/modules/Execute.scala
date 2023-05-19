package ooo.modules

import chisel3._
import ooo.Types._
import ooo.Configuration
import chisel3.util._
import ooo.Types.EventType._
import ooo.Types.Opcode.branch
import ooo.modules.Execution.ALU
import ooo.modules.IdAllocator.shouldBeKilled
import ooo.util.{BundleExpander, LookUp}

class Execute()(implicit c: Configuration) extends Module {
  val io = IO(new Bundle {
    val Instruction = Flipped(Decoupled(new ExecutePackage))
    val eventBus = Decoupled(new Event)
    val MemPackage = Decoupled(new MemPackage)
    val allocationInfo = Flipped(new IdAllocator.AllocatorStatePort(c.physRegisterIdWidth))
    val eventBusIn = Flipped(Valid(new Event))
  })

  val hasToStall = WireDefault(0.B)
  val insertBubble = WireDefault(0.B)

  val valid = io.Instruction.valid
  val instruction = io.Instruction.bits
  import instruction._

  val a = LookUp(opcode, operands(0),
    Opcode.auipc -> pc,
    Opcode.lui -> 0.U
  )
  val b = LookUp(opcode, immediate,
    Opcode.register -> operands(1),
    Opcode.branch -> operands(1)
  )

  // comp indicates whether branch condition was true

  val fn = LookUp(opcode, func,
    Opcode.immediate -> Mux(func(2,0) === "b101".U, func, func(2,0)),
    Opcode.branch -> func(2,0),
    Opcode.load -> 0.U,
    Opcode.store -> 0.U,
    Opcode.auipc -> 0.U,
    Opcode.lui -> 0.U,
    Opcode.jalr -> 0.U,
    Opcode.jal -> 0.U
  )

  val (res, comp) = ALU(fn)(a, b)

  val sendToMemQueue = opcode.isOneOf(Opcode.load, Opcode.store)

  val memPackageValid = RegInit(0.B)
  val memPackage = Reg(chiselTypeOf(io.MemPackage.bits))
  io.MemPackage.expand(
    _.valid := memPackageValid,
    _.bits := memPackage
  )



  val eventType = MuxCase(CompletionWithValue, Seq(
    (opcode === Opcode.branch) -> Branch,
    (opcode === Opcode.jalr) -> Jump,
    (opcode === Opcode.system) -> Exception
  ))

  val eventValid = RegInit(0.B)
  val event = Reg(chiselTypeOf(io.eventBus.bits))
  io.eventBus.expand(
    _.valid := eventValid,
    _.bits := event
  )

  val branchMisprediction = comp =/= (branchPrediction === BranchPrediction.Taken)
  val branchMispredictionRecoveryPC = Mux(io.Instruction.bits.branchPrediction === BranchPrediction.Taken, pc + 4.U, pc + immediate)

  val target = dontTouch(Mux(opcode.isOneOf(Opcode.branch), branchMispredictionRecoveryPC, res))

  insertBubble := !valid
  hasToStall := Mux(RegNext(sendToMemQueue), memPackageValid && !io.MemPackage.ready, eventValid && !io.eventBus.ready)

  when(!hasToStall) {

    memPackageValid := Mux(insertBubble, 0.B, valid && sendToMemQueue)
    eventValid := Mux(insertBubble, 0.B, valid && !sendToMemQueue)

    event.expand(
      _.eventType := eventType,
      _.pr := prd,
      _.writeBackValue := Mux(opcode.isOneOf(Opcode.jal, Opcode.jalr), pc + 4.U, res),
      _.pc := pc,
      _.target := target,
      _.snapshotId := snapshotId,
      _.misprediction := branchMisprediction
    )
    memPackage.expand(
      _.isWrite := opcode === Opcode.store,
      _.func := func(2, 0),
      _.prd := prd,
      _.Address := res,
      _.pc := pc,
      _.writeData := LookUp(func(2, 0), operands(1),
        "b000".U -> operands(1)(7, 0),
        "b001".U -> operands(1)(15, 0)
      )
    )
  }


  io.Instruction.ready := !hasToStall

  val allocatorPushBack = io.eventBusIn.valid && ((io.eventBusIn.bits.eventType.isOneOf(EventType.Branch) && io.eventBusIn.bits.misprediction) || io.eventBusIn.bits.eventType.isOneOf(EventType.Jump))
  val kill = shouldBeKilled(event.pr, io.eventBusIn.bits.pr, io.allocationInfo.oldest, io.allocationInfo.youngest, io.allocationInfo.wrapped)
  when(allocatorPushBack && kill) {
    memPackageValid := 0.B
    io.MemPackage.valid := 0.B
    io.Instruction.ready := 0.B
  }

}

