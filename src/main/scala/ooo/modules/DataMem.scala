package ooo.modules

import chisel3._
import chisel3.experimental.BundleLiterals.AddBundleLiteralConstructor
import chisel3.experimental.{ChiselAnnotation, annotate}
import chisel3.internal.firrtl.Width
import firrtl.annotations.MemoryArrayInitAnnotation
import ooo.util.SeqDataExtension
//import chisel3.util.{Fill, log2Ceil, MixedVec}
import ooo.Types._
import ooo.Types.EventType._

import ooo.modules.IdAllocator.{DeallocationPort,AllocatorStatePort,shouldBeKilled}

import ooo.Configuration
//import chisel3.util.{Decoupled, MuxCase, Valid}
import chisel3.util._
import ooo.modules.MemQueue.{QueueElementPort}

import ooo.util.BundleExpander


class DataMem(init: Seq[BigInt])(implicit c: Configuration) extends Module {
  val io = IO(new Bundle {
    val MemPort = Flipped(new MemPort)
  })

  io.MemPort.request.ready := true.B
  io.MemPort.response.valid := false.B

  val mem = SyncReadMem (4*1024 , Byte())

  annotate(new ChiselAnnotation {
    override def toFirrtl = MemoryArrayInitAnnotation(mem.toTarget, init.padTo(4*1024, BigInt(0)))
  })

  val ReadReg = RegInit(0.B)

  val addresses = Seq.tabulate(4)(i => io.MemPort.request.bits.Address + i.U)
  val readBytes = addresses.map(mem.read(_))

  io.MemPort.response.bits.readData := readBytes(3) ## readBytes(2) ## readBytes(1) ## readBytes(0)

  when(io.MemPort.request.valid){
    when(io.MemPort.request.bits.isWrite){
      val wd = io.MemPort.request.bits.WriteData
      val writeBytes = Seq(wd(7,0), wd(15,8), wd(23,16), wd(31,24))
      addresses.lazyZip(io.MemPort.request.bits.mask).lazyZip(writeBytes).foreach { case (a,en,data) =>
        when(en) {
          mem.write(a, data)
        }
      }
      io.MemPort.response.valid := true.B
    }.otherwise{
      ReadReg := true.B
    }
  }

  when(ReadReg && io.MemPort.response.ready){
    io.MemPort.response.valid := true.B
    ReadReg := false.B
  }
  
}

// object Datapenis extends App { emitVerilog(new DataMem()(Configuration.random()))}