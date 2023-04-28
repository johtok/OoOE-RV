package ooo.modules.Execution

import chisel3._
import chisel3.util._
import ooo.Types.Word
import ooo.util._

class FU_input(val XLEN: Int) extends Bundle {
  val input_1 = Input(UInt(XLEN.W))
  val input_2 = Input(UInt(XLEN.W))
  val FU_fn = Input(UInt(4.W)) // instruction bit 31 & func3
}

class FU_output(val XLEN: Int) extends Bundle {
  val output = Output(UInt(XLEN.W))
}

class FUInterface(val XLEN: Int) extends Bundle {
  val in = new FU_input(XLEN)
  val out = new FU_output(XLEN)
}

object ALU {
  def apply(fn: UInt)(a: UInt, b: UInt): (UInt, Bool) = {
    val alu = Module(new ALU)
    alu.io.expand(
      _.ops(0) := a,
      _.ops(1) := b,
      _.fn := fn
    )
    (alu.io.res, alu.io.comp)
  }
}

class ALU extends Module {
  val io = IO(new Bundle {
    val ops = Input(Vec(2, Word()))
    val fn = Input(UInt(4.W))
    val res = Output(Word())
    val comp = Output(Bool())
  })

  val uop = io.ops
  val sop = io.ops.map(_.asSInt)
  val shamt = uop(1)(4,0)

  io.res := DontCare

  switch(io.fn) {
    is(0.U) {
      io.res := (sop(0) + sop(1)).asUInt // ADD - We have to do some checking due to negative + negative
    }
    is(8.U) {
      io.res := (sop(0) - sop(1)).asUInt // SUB - We have to do some checking due to negative - negative
    }
    is(1.U) {
      io.res := uop(0) << shamt // SLL
    }
    is(2.U) {
      io.res := sop(0) < sop(1) // SLT
    }
    is(3.U) {
      io.res := uop(0) < uop(1) // SLTU - hmm this seems weird.
    }
    is(4.U) {
      io.res := uop(0) ^ uop(1) // XOR
    }
    is(5.U) {
      io.res := uop(0) >> shamt // SRL
    }
    is(13.U) {
      io.res := (sop(0) >> shamt).asUInt // SRA
    }
    is(6.U) {
      io.res := uop(0) | uop(1) // OR
    }
    is(7.U) {
      io.res := uop(0) & uop(1) // AND
    }
  }

  io.comp := MuxLookup(io.fn(2,0), 0.B, Seq(
    "b000".U -> (uop(0) === uop(1)),
    "b001".U -> (uop(0) =/= uop(1)),
    "b100".U -> (sop(0) < sop(1)),
    "b101".U -> (sop(0) >= sop(1)),
    "b110".U -> (uop(0) < uop(1)),
    "b111".U -> (uop(0) >= uop(1))
  ))

  // AUIPC
  // ADD immidiate
  // AND Immidiate
  // OR  Immidiate
  // XOR Immidiate
  // ADD
  // SUB
  // AND
  // OR
  // XOR
  // SLL
  // SRL
  // SRA
  // SLT < Immidiate
  // SLT < Unsigned Immidiate
  // SLL Immidiate
  // SRL Immidiate
  // SRA Immidiate
  // SLT
  // SLT < Unsigned
}