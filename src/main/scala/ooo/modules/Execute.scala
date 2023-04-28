package ooo.modules

import chisel3._
import chisel3.internal.firrtl.Width
import ooo.Types._
import ooo.Configuration
import chisel3.util._
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

  io.MemPackage.valid := io.Instruction.valid && opcode.isOneOf(Opcode.load, Opcode.store)
  io.MemPackage.bits.expand(
    _.isWrite := opcode === Opcode.store,
    _.func := func(2, 0),
    _.prd := prd,
    _.Address := res
  )

  io.eventBus.bits.elements.foreach(_._2 := DontCare)
  io.eventBus.valid := 0.B // TODO
  io.eventBus.bits.expand(
    // TODO
  )

  io.Instruction.ready := 1.B // TODO

}

