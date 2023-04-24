package ooo.modules

import chisel3._
import chisel3.internal.firrtl.Width
import ooo.Types._
import ooo.Types.EventType._
import ooo.Configuration
import chisel3.util._


class EventArbiter()(implicit c: Configuration) extends Module {
  val io = IO(new Bundle {
    //val LoadWrite = Flipped(Decoupled(new WritePort))
    //val Writeback = Flipped(Decoupled(new WritePort))

    val ExecuteIn = Flipped(Decoupled(new Event))
    val MemIn = Flipped(Valid(new Event))
    val EventOut = Valid(new Event)
  })

  io.ExecuteIn.ready := !io.MemIn.valid
  io.EventOut.valid := false.B

  io.EventOut.bits := io.MemIn.bits

  when(io.MemIn.valid){
    io.EventOut.valid := true.B
  }.elsewhen(io.ExecuteIn.valid){
    io.EventOut.bits := io.ExecuteIn.bits
    io.EventOut.valid := true.B
  }
}
