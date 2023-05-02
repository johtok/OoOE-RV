package ooo.modules

import chisel3._
import chisel3.util._

class Branch_input(val XLEN: Int) extends Bundle {
        val input1 = Input(UInt(XLEN.W))
        val input2 = Input(UInt(XLEN.W))
        val imm = Input(UInt(XLEN.W))
        val PC = Input(UInt(XLEN.W))
        val func3 = Input(UInt(3.W))

}

class Branch_output() extends Bundle {
        val output = Output(Bool())
}
class BranchInterface(val XLEN: Int) extends Bundle {
        val in = new Branch_input(XLEN)
        val out = new Branch_output(XLEN)
}
//TODO: NOT TESTED
class Branch(val XLEN: Int) extends Module {
    val io = IO(new BranchInterface(XLEN))

val input1 = io.in.input1
val input2 = io.in.input2
val func3 = io.in.func3
val output = io.out.output

val BEQ = 0.U
val BNE = 1.U
val BLT = 4.U
val BGE = 5.U
val BLTU = 6.U
val BGEU = 7.U
switch(func3){
is(BEQ){when(input1 === input2){output := 1.B}}
is(BNE){when(input1 =/= input2){output := 1.B}}
is(BLT){when(input1.asSInt < input2.asSInt){output := 1.B}}
is(BGE){when(input1.asSInt >= input2.asSInt){output := 1.B}}
is(BLTU){when(input1 < input2){output := 1.B}}
is(BGEU){when(input1 >= input2){output := 1.B}}
}

}