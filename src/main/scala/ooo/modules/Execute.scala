package ooo.modules

import chisel3._
import ooo.Types._
import ooo.Configuration
import chisel3.util._
import ooo.Types.EventType._
import ooo.Types.Opcode.branch
import ooo.modules.Execution.ALU
import ooo.util.{BundleExpander, LookUp}

class Execute()(implicit c: Configuration) extends Module {
  val io = IO(new Bundle {
    val Instruction = Flipped(Decoupled(new ExecutePackage))
    val eventBus = Decoupled(new Event)
    val MemPackage = Decoupled(new MemPackage)
  })

  // TODO: kill instructions

  val ready = Wire(Bool())

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
    Opcode.immediate -> func(2,0),
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
    (opcode === Opcode.jalr) -> Jump,
    (opcode === Opcode.system) -> Exception
  ))

  val eventValid = RegNext(valid && !sendToMemQueue)
  val event = Reg(chiselTypeOf(io.eventBus.bits))
  io.eventBus.expand(
    _.valid := eventValid,
    _.bits := event
  )

  val branchMisprediction = comp =/= (branchPrediction === BranchPrediction.Taken)
  val branchMispredictionRecoveryPC = Mux(io.Instruction.bits.branchPrediction === BranchPrediction.Taken, pc + 4.U, res)

  val target = Mux(opcode.isOneOf(Opcode.branch), branchMispredictionRecoveryPC, res)

  event.expand(
    _.eventType := eventType,
    _.pr := prd,
    _.writeBackValue := Mux(opcode.isOneOf(Opcode.jal, Opcode.jalr), pc + 4.U, res),
    _.pc := pc,
    _.target := target,
    _.snapshotId := snapshotId,
    _.misprediction := branchMisprediction
  )

  io.Instruction.ready := ready

  ready := !valid || (valid && Mux(sendToMemQueue, Mux(memPackageValid, io.MemPackage.ready, 1.B), Mux(eventValid, io.eventBus.ready, 1.B)))

}

