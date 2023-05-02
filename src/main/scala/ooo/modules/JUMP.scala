package ooo.modules

import chisel3._
import chisel3.util._

class Jump_input(val XLEN: Int) extends Bundle {
        
        val imm = Input(UInt(XLEN.W))
        val PC = Input(UInt(XLEN.W))
        val func3 = Input(UInt(3.W))
        val OP = Input(UInt(7.W))
        val input1 = Input(UInt(XLEN.W))

}

class Jump_output(val XLEN: Int) extends Bundle {
        val output = Output(UInt(XLEN.W))
        val PC = Output(UInt(XLEN.W))
}
class JumpInterface(val XLEN: Int) extends Bundle {
        val in = new Jump_input(XLEN)
        val out = new Jump_output(XLEN)
}
//TODO: NOT TESTED
class Jump(val XLEN: Int) extends Module {
    val io = IO(new JumpInterface(XLEN))

val func3 = io.in.func3
val rd = io.out.output
val imm = io.in.imm
val PC_IN = io.in.PC
val PC_OUT = io.out.PC
val OP = io.in.OP
val rs1 = io.in.input1
val JAL = 1101111.B 
val JALR = 1100111.B

switch(OP){
is(JAL){
        rd := (PC_IN.asSInt+4.S).asUInt
        PC_OUT := (imm.asSInt+PC_IN.asSInt).asUInt
}
is(JALR){
        rd := (rs1.asSInt+4.S).asUInt
        PC_OUT := (imm.asSInt+PC_IN.asSInt).asUInt
}

}

}