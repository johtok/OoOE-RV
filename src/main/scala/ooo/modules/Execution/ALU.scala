package ooo.modules.Execution

import chisel3._
import chisel3.util._

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


class ALU(val XLEN: Int) extends Module {
  val io = IO(new FUInterface(XLEN))

  val input_1 = io.in.input_1
  val input_2 = io.in.input_2
  val fn = io.in.FU_fn
  val output = UInt(XLEN.W)

  switch(fn) {
    is(0.U) {
      output := input_1 + input_2 // ADD - We have to do some checking due to negative + negative
    }
    is(8.U) {
      output := input_1 - input_2 // SUB - We have to do some checking due to negative - negative
    }
    is(1.U) {
      output := input_1 << input_2 // SLL
    }
    is(2.U) {
      output := input_1 < input_2 // SLT
    }
    is(3.U) {
      output := input_1 < input_2 // SLTU - hmm this seems weird.
    }
    is(4.U) {
      output := input_1 ^ input_2 // XOR
    }
    is(5.U) {
      output := input_1 >> input_2 // SRL
    }
    is(13.U) {
      output := input_1 >> input_2 // SRA
    }
    is(6.U) {
      output := input_1 | input_2 // OR
    }
    is(7.U) {
      output := input_1 & input_2 // AND
    }
  }

  io.out.output := output

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