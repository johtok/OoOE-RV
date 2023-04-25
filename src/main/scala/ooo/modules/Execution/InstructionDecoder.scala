package ooo.modules.Execution

import chisel3._
import chisel3.util._

class InstructionDecoder(val XLEN: Int,val INSTRLEN: Int, val DTAGLEN: Int,val TAGLEN: Int) extends Module {
    val io = IO(new Bundle {
        val in = new Execution_Inputs(XLEN,DTAGLEN,INSTRLEN)
        val out = chisel3.Flipped(new FU_input(XLEN))        
    })
}

// logic converting input format to RISC32 format 