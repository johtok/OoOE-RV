package ooo

import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util.{Fill, Valid, Decoupled}
import ooo.util.LookUp

object Types {

  object Word { def apply() = UInt(32.W) }
  object Byte { def apply() = UInt(8.W) }
  object ArchRegisterId { def apply() = UInt(5.W) }
  object PhysRegisterId { def apply()(implicit c: Configuration) = UInt(c.physRegisterIdWidth) }
  object SnapshotId { def apply()(implicit c: Configuration) = UInt(c.snapshotIdWidth) }

  object BranchPrediction extends ChiselEnum {
    val NotTaken, Taken = Value
  }

  class IssuePackage(implicit c: Configuration) extends Bundle {
    val opcode = Opcode()
    val func = UInt(4.W)
    val prs = Vec(2, new Bundle { val ready = Bool(); val id = PhysRegisterId() })
    val prd = PhysRegisterId()
    val immediate = Word()
    val pc = Word()
    val snapshotId = SnapshotId()
    val branchPrediction = BranchPrediction()
  }

  class ExecutePackage(implicit c: Configuration) extends Bundle {
    val opcode = Opcode()
    val func = UInt(4.W)
    val operands = Vec(2, Word())
    val prd = PhysRegisterId()
    val immediate = Word()
    val pc = Word()
    val snapshotId = SnapshotId()
    val branchPrediction = BranchPrediction()
  }

  class InstructionPackage extends Bundle {
    val instruction = Word()
    val branchPrediction = BranchPrediction()
    val pc = Word()
  }

  class ReadPort(implicit c: Configuration) extends Bundle{
    val Address  = Output(Vec(2, PhysRegisterId()))
    val ReadData = Input(Vec(2,Word()))
  }

  class WritePort(implicit c: Configuration) extends Bundle{
    val Address = PhysRegisterId()
    val WriteData = Word()
  }

  object EventType extends ChiselEnum {
    val CompletionWithValue, Completion, Branch, Jump, Exception = Value
  }
  
  class Event(implicit c: Configuration) extends Bundle {
    val eventType = EventType()
    val pr = PhysRegisterId()
    val writeBackValue = Word()
    val pc = Word()
    val snapshotId = SnapshotId()
    val misprediction = Bool()
    val target = Word()
  }

  class MemPackage(implicit c: Configuration) extends Bundle {
    val isWrite = Bool() // 0 = read, 1 = write
    val func = UInt(3.W)
    val prd = PhysRegisterId()
    val writeData = Word()
    val Address = Word()
    val pc = Word()
    //val storeId = StoreId()
  }

  object Opcode extends ChiselEnum {
    val load = Value(0x03.U) // I-type | 0x03
    val miscMem = Value(0x0F.U) // I-type | 0x0F
    val immediate = Value(0x13.U) // I-type | 0x13
    val auipc = Value(0x17.U) // U-type | 0x17
    val store = Value(0x23.U) // S-type | 0x23
    val register = Value(0x33.U) // R-type | 0x33
    val lui = Value(0x37.U) // U-type | 0x37
    val branch = Value(0x63.U) // B-type | 0x63
    val jalr = Value(0x67.U) // I-type | 0x67
    val jal = Value(0x6F.U) // J-Type | 0x6F
    val system = Value(0x73.U) // I-type | 0x73

    def fromInstruction(instr: UInt): Opcode.Type = Opcode.safe(instr(6, 0))._1
    def safeFromInstruction(instr: UInt): (Opcode.Type, Bool) = Opcode.safe(instr(6, 0))
  }


  class MemPort extends Bundle{
    val request = Decoupled(new Bundle {
      val Address = Word()
      val WriteData = Word()
      val isWrite = Bool()
      val mask = Vec(4, Bool())
    })
    val response = Flipped(Decoupled(new Bundle {
      val readData = Word()
    }))
  }

  object InstructionType extends ChiselEnum {
    val R, I, S, B, U, J = Value

    def fromOpcode(opcode: Opcode.Type): InstructionType.Type = {
      LookUp(opcode, R,
        Opcode.load -> I,
        Opcode.miscMem -> I,
        Opcode.immediate -> I,
        Opcode.auipc -> U,
        Opcode.store -> S,
        Opcode.register -> R,
        Opcode.lui -> U,
        Opcode.branch -> B,
        Opcode.jalr -> I,
        Opcode.jal -> J,
        Opcode.system -> I
      )
    }
  }

  object Immediate {

    implicit class InstructionFieldExtractor(i: UInt) {
      require(i.getWidth == 32)
      def opcode: Opcode.Type = Opcode.fromInstruction(i)

      def instructionType: InstructionType.Type = InstructionType.fromOpcode(opcode)
      object immediate {
        def iType: SInt = (Fill(21, i(31)) ## i(30, 25) ## i(24, 21) ## i(20)).asSInt

        def sType: SInt = (Fill(21, i(31)) ## i(30, 25) ## i(11, 8) ## i(7)).asSInt

        def bType: SInt = (Fill(20, i(31)) ## i(7) ## i(30, 25) ## i(11, 8) ## 0.B).asSInt

        def uType: SInt = (i(31, 12) ## Fill(12, 0.B)).asSInt

        def jType: SInt = (Fill(10, i(31)) ## i(19, 12) ## i(20) ## i(30, 25) ## i(24, 21) ## 0.B).asSInt
      }

    }

  }

}
