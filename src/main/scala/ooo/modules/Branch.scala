package ooo.modules

import chisel3._
import chisel3.util._

class Branch_input(val XLEN: Int) extends Bundle {
        val input1 = Input(UInt(XLEN.W))
        val input2 = Input(UInt(XLEN.W))
        val func3 = Input(UInt(3.W))
}

class Branch_output(val XLEN: Int) extends Bundle {
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

val BEQ = input1 === input2
val BNE = input1 =/= input2
val BLT = input1.asSInt < input2.asSInt
val BGE = input1.asSInt >= input2.asSInt
val BLTU = input1 < input2
val BGEU = input1 >= input2
switch(func3){
is(0.U){output := BEQ}
is(1.U){output := BNE}
is(4.U){output := BLT}
is(5.U){output := BGE}
is(6.U){output := BLTU}
is(7.U){output := BGEU}
}

}