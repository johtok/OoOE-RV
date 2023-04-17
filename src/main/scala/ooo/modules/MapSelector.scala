package ooo.modules

import chisel3._
import ooo.Configuration
import ooo.Types.ArchRegisterId
import ooo.util.SeqDataExtension


class MapSelector()(implicit c: Configuration) extends Module {


  val io = IO(new Bundle {
    val read = new Bundle {
      val rs = Input(Vec(2, ArchRegisterId()))
      val useSpec = Output(Vec(2, Bool()))
    }
    val update = Input(new Bundle {
      val rd = Input(ArchRegisterId())
      val markAsSpec = Bool()
    })
    val clear = Input(Bool())
  })

  val regs = RegInit(Seq.fill(32)(0.B).toVec)


  io.read.useSpec := io.read.rs.map(regs(_))

  when(io.clear) {
    regs.foreach(_ := 0.B)
  }.elsewhen(io.update.markAsSpec) {
    regs(io.update.rd) := 1.B
  }

}
