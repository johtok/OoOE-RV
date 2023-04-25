package ooo.modules.Execution

import chisel3._

class DataBus(val XLEN: Int, val DTAGLEN: Int) extends Bundle {
  val dest_tag = Output(UInt(DTAGLEN.W))
  val result = Output(UInt(XLEN.W))
}

class JumpBus(val XLEN: Int, val TAGLEN: Int) extends Bundle {
  val tag = Output(UInt(TAGLEN.W))
  val PC = Output(UInt(XLEN.W))
}

class StoreBus(val XLEN: Int) extends Bundle {
  val adress = Output(UInt(XLEN.W))
  val value = Output(UInt(XLEN.W))
}

class LoadBus(val XLEN: Int) extends Bundle {
  val adress = Output(UInt(XLEN.W))
}


class Execution_Inputs(val XLEN: Int, val DTAGLEN: Int, val INSTRLEN: Int) extends Bundle {
  val dest_tag = Input(UInt(DTAGLEN.W))
  val instr_code = Input(UInt(INSTRLEN.W))
  val rs1 = Input(UInt(XLEN.W))
  val rs2 = Input(UInt(XLEN.W))
  val PC = Input(UInt(XLEN.W)) // instruction bit 31 & func3
}

class Execution_Outputs(val XLEN: Int, val DTAGLEN: Int, val TAGLEN: Int) extends Bundle {
  val DataBus2 = new DataBus(XLEN, DTAGLEN)
  val JumpBus = new JumpBus(XLEN, TAGLEN)
  val StoreBus = new StoreBus(XLEN)
  val LoadBus = new LoadBus(XLEN)
}


class Execution(val XLEN: Int) extends Module {
  // Constants subject to change
  val DTAGLEN = 4
  val INSTRLEN = 8
  val TAGLEN = DTAGLEN

  val io = IO(new Bundle {
    val in = new Execution_Inputs(XLEN, DTAGLEN, INSTRLEN)
    val out = new Execution_Outputs(XLEN, DTAGLEN, TAGLEN)
  })

  // vals

  // modules
  val FU_ALU = Module(new ALU(XLEN))
  val InstructionDecoder = Module(new InstructionDecoder(XLEN,INSTRLEN, DTAGLEN, TAGLEN))

  // Connect modules


  // succes

}
