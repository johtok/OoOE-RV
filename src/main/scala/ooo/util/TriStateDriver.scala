package ooo.util

import chisel3._
import chisel3.experimental.{Analog, attach}
import chisel3.internal.firrtl.Width
import chisel3.util.HasBlackBoxInline

object TriStateDriver {
  def apply(wire: Analog)(driveData: UInt, drive: Bool): UInt = {
    val mod = Module(new TriStateDriver(wire.getWidth.W))
    attach(wire, mod.io.bus)
    mod.io.drive := drive
    mod.io.driveData := driveData
    mod.io.busData
  }
}

class TriStateDriver(width: Width) extends BlackBox(Map("w" -> width.get.toInt)) with HasBlackBoxInline {
  val io = IO(new Bundle{
    val busData =     Output(UInt(width))   // data on the bus
    val driveData =   Input(UInt(width))    // data put on the bus if io.drive is asserted
    val bus =         Analog(width)         // the tri-state bus
    val drive =       Input(Bool())           // when asserted the module drives the bus
  })

  setInline("TriStateDriver.v",
    s"""
       |module TriStateDriver #(parameter w = 1)(
       |    output [w-1:0] busData,
       |    input [w-1:0] driveData,
       |    inout [w-1:0] bus,
       |    input drive);
       |
       |    assign bus = drive ? driveData : {w{1'bz}};
       |    assign busData = bus;
       |endmodule
       |""".stripMargin
  )
}
