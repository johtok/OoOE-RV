package ooo.modules

import chisel3._
import ooo.Configuration
import ooo.Types.{ArchRegisterId, PhysRegisterId}

object Arch2PhysMap {

  class ReadPort(implicit c: Configuration) extends Bundle {
    val rs = Input(Vec(2, ArchRegisterId()))
    val prs = Output(Vec(2, PhysRegisterId()))
  }

  class WritePort(implicit c: Configuration) extends Bundle {
    val write = Input(Bool())
    val prd = Input(PhysRegisterId())
    val rd = Input(ArchRegisterId())
  }

}
