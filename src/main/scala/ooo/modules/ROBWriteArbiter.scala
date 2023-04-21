package ooo.modules

import chisel3._
import chisel3.internal.firrtl.Width
import ooo.Types._
import ooo.Types.EventType._
import ooo.Configuration
import chisel3.util._


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

  io.event.bits.pc := 0.U
  io.event.bits.eventType := CompletionWithValue
  io.event.valid := false.B

  io.Write.bits := io.LoadWrite.bits
  io.event.bits.pr := io.LoadWrite.bits.Address


  when(io.LoadWrite.valid){
    io.Write.valid := true.B
    io.event.valid := true.B 
  }.elsewhen(io.Writeback.valid){
    io.Write.bits := io.Writeback.bits
    io.Write.valid := true.B
    io.event.bits.pr := io.Writeback.bits.Address
    io.event.valid := true.B
  }
}
