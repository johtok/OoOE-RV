package ooo.modules

import chisel3._
import chisel3.util._

class ALU_input(val XLEN: Int) extends Bundle {
        val input1 = Input(UInt(XLEN.W))
        val input2 = Input(UInt(XLEN.W))
        val func3 = Input(UInt(3.W))
        val func_mod = Input(Bool())              // func 7 == 0x20 (pass instruction bit 31)
}

class ALU_output(val XLEN: Int) extends Bundle {
        val output = Output(UInt(XLEN.W))
}
class ALUInterface(val XLEN: Int) extends Bundle {
        val in = new ALU_input(XLEN)
        val out = new ALU_output(XLEN)
}

class ALU(val XLEN: Int) extends Module {
    val io = IO(new ALUInterface(XLEN))

val func3 = io.in.func3
val func_mod = io.in.func_mod
val input1 = io.in.input1
// val input2 = io.in.input2
val output = UInt(XLEN.W)

val input2 = Mux(func_mod, ~io.in.input2, io.in.input2) // If only a solution for SRA and negated input could be found. Negating input1 when SRA.

val AND = input1 & input2
val XOR = input1 ^ input2

val out_add = input1 + input2 + func_mod                         // ADD & SUB (func_mod should be Cin on the first adder.)
val out_2 = Mux(func3.apply(0) === 1.U, AND, XOR | AND)          // AND, OR 
val out_3 = Mux(func3.apply(0) === 1.U, input1 >> input2, XOR)   // SRL & SRA
val out_lshift = input1 << input2.apply(5, 0)                    // SLL
val out_cmp = Mux(input1 < input2, 1.U, 0.U)                     // SLTU & SLTIU

io.out.output := Mux(func3.apply(2) === 1.U,                     // Should be able to be cleaner, but switch didn't work for me.
        Mux(func3.apply(1) === 1.U, out_2, out_3), 
        Mux(func3 === 0.U, out_add, Mux(func3.apply(1) === 1.U, out_cmp, out_lshift))) 
}