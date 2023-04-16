package ooo.modules

import chisel3._
import chisel3.internal.firrtl.Width
//import chisel3.util.{Fill, log2Ceil, MixedVec}
import ooo.Types._
import ooo.Configuration
//import chisel3.util.{Decoupled, MuxCase, Valid}
import chisel3.util._
import ooo.modules.IssueQueue.{ ElementPort}
import ooo.util.BundleExpander

class ROBWriteArbiter()(implicit c: Configuration) extends Module {
  val io = IO(new Bundle {
    val LoadWrite = Flipped(Decoupled(new WritePort))
    val Writeback = Flipped(Decoupled(new WritePort))

    val Write = Valid(new WritePort)
    val event = Valid(new Event)
  })

  io.LoadWrite.ready := true.B
  io.Writeback.ready := !io.LoadWrite.valid
  io.Write.valid := false.B

  when(io.LoadWrite.valid){
    io.Write.bits := io.LoadWrite.bits
    io.Write.valid := true.B
  }.elsewhen(io.Writeback.valid){
    io.Write.bits := io.Writeback.bits
    io.Write.valid := true.B
  }
}

