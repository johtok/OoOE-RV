package ooo.modules

import chisel3._
import ooo.Configuration
import ooo.Types.{ArchRegisterId, PhysRegisterId, Word,ReadPort}


class ReorderBuffer(implicit c: Configuration) extends Module {

  val io = IO(new Bundle {

    val data = new Bundle {
      val read = Flipped(new ReadPort)
      val write = Input(new Bundle {
        val pr = PhysRegisterId()
        val data = Word()
        val write = Bool()
      })
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


  when(io.data.write.write) { dataMem.write(io.data.write.pr, io.data.write.data) }

  io.ready.read.foreach(r => r.isReady := readyMem.read(r.pr))
  when(io.ready.update.markAsReady) { readyMem.write(io.ready.update.pr, 1.B) }

  io.destination.read.rd := destMem.read(io.destination.read.pr)
  when(io.destination.write.write) { destMem.write(io.destination.write.pr, io.destination.write.rd) }


}
