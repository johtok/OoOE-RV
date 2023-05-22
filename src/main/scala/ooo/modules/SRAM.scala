package ooo.modules

import chisel3._
import chisel3.experimental.{ChiselAnnotation, annotate}
import chisel3.util.{HasBlackBoxPath, UIntToOH, log2Ceil}
import firrtl.annotations.MemoryArrayInitAnnotation
import ooo.Configuration
import ooo.Types.MemPort
import ooo.util._

class SRAM(wordSize: Int, init: Option[Seq[BigInt]] = None) extends Module {

  val io = IO(Flipped(new MemPort))

  val banks = Seq.fill(4)(SyncReadMem(wordSize, UInt(8.W)))

  init match {
    case Some(state) =>
      val bankInits = state.padTo(wordSize * 4, BigInt(0)).grouped(4).toSeq.transpose

      banks.zip(bankInits).foreach { case (bank, init) =>
        annotate(new ChiselAnnotation {
          override def toFirrtl = MemoryArrayInitAnnotation(bank.toTarget, init)
        })
      }
    case None => ;
  }




  val writeData = io.request.bits.WriteData.groupBytes
  val address = io.request.bits.Address
  val strobes = io.request.bits.mask
  val isWrite = io.request.bits.isWrite
  val valid = io.request.valid

  val offset = address(1, 0)

  val index = Seq.tabulate(4)(i => Mux(offset > i.U, address(31, 2) + 1.U, address(31, 2)))

  val alignedWriteData = BarrelRotater(writeData, offset)
  val alignedStrobes = BarrelRotater(strobes, offset)

  val write = valid && isWrite

  banks.lazyZip(index).lazyZip(alignedWriteData).lazyZip(alignedStrobes).foreach { case (bank, index, data, en) =>
    when(write && en) { bank.write(index, data) }
  }

  val readData = banks.zip(index).map { case (bank, index) => bank.read(index) }

  val offsetReg = Reg(UInt(2.W))
  offsetReg := offset
  val alignedReadData = BarrelRotater(readData.reverse, offsetReg).reverse.concatenated

  io.request.ready := 1.B
  io.response.expand(
    _.valid := RegNext(valid && !isWrite, 0.B),
    _.bits.readData := alignedReadData
  )


}
