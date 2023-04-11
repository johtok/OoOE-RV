package ooo

import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util.{Fill, Valid}

object Types {

  object Word { def apply() = UInt(32.W) }
  object Byte { def apply() = UInt(8.W) }
  object ArchRegisterId { def apply() = UInt(5.W) }
  object PhysRegisterId { def apply()(implicit c: Configuration) = UInt(c.physRegisterIdWidth) }
  object BranchId { def apply()(implicit c: Configuration) = UInt(c.branchIdWidth) }
  object LoadId { def apply()(implicit c: Configuration) = UInt(c.loadIdWidth) }
  object StoreId { def apply()(implicit c: Configuration) = UInt(c.storeIdWidth) }


  object MicroOp extends ChiselEnum {
    val Load, Store, Branch, Jump, Register, Immediate = Value // TODO: more?
  }

  object BranchPrediction extends ChiselEnum {
    val NotTaken, Taken = Value
  }

  class IssuePackage(implicit c: Configuration) extends Bundle {
    val microOp = MicroOp()
    val func = UInt(4.W)
    val prs = Vec(2, new Bundle { val ready = Bool(); val id = PhysRegisterId() })
    val prd = PhysRegisterId()
    val immediate = Word()
    val pc = Word()
    val branchId = BranchId()
    val branchPrediction = BranchPrediction()
    val loadId = LoadId()
    val storeId = StoreId()
  }

  class InstructionPackage extends Bundle {
    val instruction = Word()
    val branchPrediction = BranchPrediction()
    val pc = Word()
  }

  object EventType extends ChiselEnum {
    val Branch, Jump, Exception = Value
  }
  class Event(implicit c: Configuration) extends Bundle {
    val eventType = EventType()
    val pr = PhysRegisterId()
    val pc = Word()
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

  object Immediate {

    implicit class InstructionFieldExtractor(i: UInt) {
      require(i.getWidth == 32)
      def opcode: Opcode.Type = Opcode.fromInstruction(i)
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
