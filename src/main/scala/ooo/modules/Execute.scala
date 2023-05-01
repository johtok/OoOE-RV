package ooo.modules

import chisel3._
import ooo.Types._
import ooo.Configuration
import chisel3.util._
import ooo.Types.EventType._
import ooo.modules.Execution.ALU
import ooo.util.{BundleExpander, LookUp}

class Execute()(implicit c: Configuration) extends Module {
  val io = IO(new Bundle {
    val Instruction = Flipped(Decoupled(new ExecutePackage))
    val eventBus = Decoupled(new Event)
    val MemPackage = Decoupled(new MemPackage)
  })

  val ready = Wire(Bool())

  val valid = RegNext(io.Instruction.valid, 0.B)
  val instruction = RegEnable(io.Instruction.bits, ready)
  import instruction._

  val a = LookUp(opcode, operands(0),
    Opcode.auipc -> pc,
    Opcode.lui -> 0.U
  )
  val b = LookUp(opcode, immediate,
    Opcode.register -> operands(1),
    Opcode.branch -> operands(1),
    Opcode.jalr -> 4.U,
    Opcode.jal -> 4.U
  )

  // comp indicates whether branch condition was true
  val (res, comp) = ALU(func)(a, b)

  val sendToMemQueue = opcode.isOneOf(Opcode.load, Opcode.store)

  val memPackageValid = RegNext(valid && sendToMemQueue, 0.B)
  val memPackage = Reg(chiselTypeOf(io.MemPackage.bits))
  io.MemPackage.expand(
    _.valid := memPackageValid,
    _.bits := memPackage
  )

  memPackage.expand(
    _.isWrite := opcode === Opcode.store,
    _.func := func(2, 0),
    _.prd := prd,
    _.Address := res
  )

  val eventType = MuxCase(CompletionWithValue, Seq(
    (opcode === Opcode.branch) -> Branch,
    (opcode === Opcode.jalr) -> Jump
  ))

  val eventValid = RegNext(valid && !sendToMemQueue)
  val event = Reg(chiselTypeOf(io.eventBus.bits))
  io.eventBus.expand(
    _.valid := eventValid,
    _.bits := event
  )

  event.expand(
    _.eventType := eventType,
    _.pr := prd,
    _.writeBackValue := res,
    _.pc := pc,
    _.snapshotId := snapshotId,
    _.misprediction := comp && !(branchPrediction === BranchPrediction.Taken)
  )

  io.Instruction.ready := ready

  ready := !valid || (valid && (!sendToMemQueue || (sendToMemQueue && (!memPackageValid || (memPackageValid && io.MemPackage.ready)))))

}

