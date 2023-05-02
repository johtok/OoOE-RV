package ooo.modules

import chisel3._
import chisel3.experimental.BundleLiterals.AddBundleLiteralConstructor
import chisel3.internal.firrtl.Width
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


class DataMem()(implicit c: Configuration) extends Module {
  val io = IO(new Bundle {
    val MemPort = Flipped(new MemPort)
  })

  io.MemPort.request.ready := false.B
  io.MemPort.response.valid := false.B

  val mem = SyncReadMem (1024 , Word())

  val ReadReg = RegInit(0.B)


  io.MemPort.response.bits.readData := mem.read(io.MemPort.request.bits.Address)

  when(io.MemPort.request.valid){
    when(io.MemPort.request.bits.isWrite){
      mem.write(io.MemPort.request.bits.Address, io.MemPort.request.bits.WriteData)
    }.otherwise{
      ReadReg := true.B
    }
  }

  when(ReadReg && io.MemPort.response.ready){
    io.MemPort.response.valid
    ReadReg := false.B
  }
  
}

//object Datapenis extends App { emitVerilog(new DataMem()(Configuration.random()))}