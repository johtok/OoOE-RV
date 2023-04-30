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

  import io.Instruction.bits._

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

  io.MemPackage.valid := io.Instruction.valid && sendToMemQueue
  io.MemPackage.bits.expand(
    _.isWrite := opcode === Opcode.store,
    _.func := func(2, 0),
    _.prd := prd,
    _.Address := res
  )

  val eventType = MuxCase(CompletionWithValue, Seq(
    (opcode === Opcode.branch) -> Branch,
    (opcode === Opcode.jalr) -> Jump
  ))

  io.eventBus.bits.elements.foreach(_._2 := DontCare)
  io.eventBus.valid := io.Instruction.valid && !sendToMemQueue
  io.eventBus.bits.expand(
    _.eventType := eventType,
    _.pr := prd,
    _.writeBackValue := res,
    _.pc := pc,
    _.snapshotId := snapshotId,
    _.misprediction := comp && !(branchPrediction === BranchPrediction.Taken)
  )

  io.Instruction.ready := (sendToMemQueue && !io.MemPackage.ready)

}

