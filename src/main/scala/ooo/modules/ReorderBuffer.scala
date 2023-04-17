package ooo.modules

import chisel3._
import ooo.Configuration
import chisel3.util._
import ooo.Types.{ArchRegisterId, PhysRegisterId, Word,ReadPort,WritePort}


class ReorderBuffer(implicit c: Configuration) extends Module {

  val io = IO(new Bundle {

    val data = new Bundle {
      val read = Flipped(new ReadPort)
      val write = Flipped(Valid(new WritePort))
    }

    val ready = new Bundle {
      val read = Vec(3, new Bundle {
        val pr = Input(PhysRegisterId())
        val isReady = Output(Bool())
      })
      val update = Input(new Bundle {
        val pr = PhysRegisterId()
        val markAsReady = Bool()
      })
    }

    val destination = new Bundle {
      val read = new Bundle {
        val pr = Input(PhysRegisterId())
        val rd = Output(ArchRegisterId())
      }
      val write = Input(new Bundle {
        val pr = PhysRegisterId()
        val rd = ArchRegisterId()
        val write = Bool()
      })
    }

  })


  val dataMem = SyncReadMem(c.reorderBufferSize, Word())
  val readyMem = SyncReadMem(c.reorderBufferSize, Bool())
  val destMem = SyncReadMem(c.reorderBufferSize, ArchRegisterId())

  //io.data.read.foreach(r => r.ReadData := dataMem.read(r.Address))

  io.data.read.ReadData(0) := dataMem.read(io.data.read.Address(0).id)
  io.data.read.ReadData(1) := dataMem.read(io.data.read.Address(1).id)


  when(io.data.write.valid) { dataMem.write(io.data.write.bits.Address, io.data.write.bits.WriteData) }

  io.ready.read.foreach(r => r.isReady := readyMem.read(r.pr))
  when(io.ready.update.markAsReady) { readyMem.write(io.ready.update.pr, 1.B) } // TODO: this creates wrong hardware (no write enable)

  io.destination.read.rd := destMem.read(io.destination.read.pr)
  when(io.destination.write.write) { destMem.write(io.destination.write.pr, io.destination.write.rd) }


}

object ReorderBuffer extends App { emitVerilog(new ReorderBuffer()(Configuration.default()))}