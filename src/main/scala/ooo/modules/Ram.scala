package ooo.modules


import chisel3._
import ooo.Configuration
import ooo.Types.MemPort
import ooo.util.{DatatoByteVec, byteVecToUInt}

class RAM(
           size: Int
         )(implicit c: Configuration) extends Module {
  //--------------------------------------------------------------------------------------------------------------------
  require(size%4==0,"Memory size in bytes needs to be divisible by 4!")
  //--------------------------------------------------------------------------------------------------------------------

  val io = IO(Flipped(new MemPort()))

  // rename master inputs and split data into byte vec
  val wrData = DatatoByteVec(io.request.bits.WriteData)
  val strobes = io.request.bits.mask
  val addr = io.request.bits.Address

  /* row and column calculation */
  val offset = addr(1,0) // offset = addr % 4
  val rows = Wire(Vec(4,UInt((31-2).W))) // vector selecting the row in each bank
  rows := VecInit(Seq.tabulate(4){ i =>
    Mux(offset > i.U, addr(31,2) + 1.U, addr(31,2))
  })
  val wrColumns = Wire(Vec(4,UInt(2.W))) // due to wrapping, bank columns need to be translated to data (byte) columns
  wrColumns := VecInit(Seq.tabulate(4){ i =>
    i.U - offset // the whole byte vec is rotated to the left by the offset
  })
  val rdColumns = Wire(Vec(4,UInt(2.W))) // again due to wrapping, we need a translation from bank columns to read data columns
  rdColumns := VecInit(Seq.tabulate(4){ i =>
    RegNext(offset + i.U)  // the bytes from the read port need to be rotated to the right by the offset on the clock cycle after read rq
  })

  // enable signals
  val readEn = io.request.valid && !io.request.bits.isWrite
  val writeEn = io.request.valid && io.request.bits.isWrite


  // the 4 memory banks and read ports
  val memoryBanks = Seq.fill(4)(SyncReadMem(size/4, UInt(8.W)))
  val readPorts = VecInit(memoryBanks.zip(rows).map{ case (bank,row) =>
    bank.read(row)
  })

  // the write data and connected strobes are shuffled to fit endianness and wrapping
  val shuffledWriteData = dontTouch(VecInit(wrColumns.map { column => wrData(column) }))
  val shuffledStrobes = VecInit(wrColumns.map { column => strobes(column)})

  // write operation
  when(writeEn){
    memoryBanks.zip(rows).zip(shuffledWriteData).zip(shuffledStrobes).map { case (((bank,row),data),strobe) =>
      when(strobe) {
        bank.write(row, data)
      }
    }
  }

  // the data from the read ports needs to be shuffled to undo endianness and wrapping
  val shuffledReadPorts = dontTouch(VecInit(rdColumns.map{ column =>
    readPorts(column)
  }))

  io.response.bits.readData := byteVecToUInt(shuffledReadPorts)
  io.response.valid := RegNext(readEn)
  io.request.ready := 1.B

}
