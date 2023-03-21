import chisel3._


// Hmm how to split up a vector...
class ALU(val instruction_width: Int) extends Module {
    val io = IO(new Bundle {
        val Instruction = Input(Wire(Vec(instruction_width, UInt(1.W))))
        val result = Output(UInt(32.W))
    })

// Wire definitions
val fn7 = UInt(7.W)        //io.Instruction(31:25)
val fn3 = 3.W        //io.Instruction(14:12)
val rs1_id = 5.W     //io.Instruction(19:15)
val rs2_id = 5.W     //io.Instruction(24:20)
val immidiate = 12.W //io.Instruction(31:20)
val rd_id = 5.W      //io.Instruction(31:20)

// fn7 := io.Instruction(31->25)

// Default output
val result = UInt(32.W)
result := 0.U

// AUIPC, Should be done in the upper stages.

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