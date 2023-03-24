package ooo.boards

import chisel3._
import chisel3.experimental.Analog
import chisel3.util.{Fill, log2Ceil}
import ooo.TriStateDriver
import ooo.boards.DE2_115.{SdramPort, SevenSegmentDigit, SramPort, UartPort}



class DE2_115 extends Module {

  val io = IO(new Bundle {
    val button = Input(UInt(3.W))
    val switch = Input(UInt(18.W))
    val greenLed = Output(UInt(9.W))
    val redLed = Output(UInt(18.W))
    val digits = Output(Vec(8, new SevenSegmentDigit))
    val uart = new UartPort
    val sram = new SramPort
    val sdram = new SdramPort
  })

  withReset(!reset.asBool) {

    io.uart.init()
    io.sram.init()
    io.sdram.init()
    TriStateDriver(io.sram.data)(0.U, 1.B)
    TriStateDriver(io.sdram.data)(0.U, 1.B)

    val max = 50000000 / 4
    val timer = RegInit(0.U(log2Ceil(max).W))
    val tick = timer === max.U
    timer := Mux(tick, 0.U, timer + 1.U)

    val counter = RegInit(0.U(32.W))
    counter := Mux(tick, counter + 1.U, counter)

    val toggleReg = RegInit(0.B)
    toggleReg := Mux(tick, !toggleReg, toggleReg)

    io.greenLed := (toggleReg && io.button(2)) ## Fill(8, toggleReg && io.button(1))
    io.redLed := Fill(18, toggleReg && io.button(0))
    counter.asBools.grouped(4).map(VecInit(_).asUInt).zip(io.digits).foreach { case (num, digit) => digit.drive(num) }


  }
}

object DE2_115 extends App {
  emitVerilog(new DE2_115, Array("-td","build"))


  class SevenSegmentDigit extends Bundle {
    val segments = UInt(7.W)
    def drive(bits: UInt) = segments := table(bits)

    def table = VecInit(Seq(
      "1000000",
      "1111001",
      "0100100",
      "0110000",
      "0011001",
      "0010010",
      "0000010",
      "1111000",
      "0000000",
      "0010000",
      "0001000",
      "0000011",
      "1000110",
      "0100001",
      "0000110",
      "0001110"
    ).map("b"+_).map(_.U))
  }
  class UartPort extends Bundle {
    val rx = Input(Bool())
    val tx = Output(Bool())
    val requestToSend = Input(Bool())
    val clearToSend = Output(Bool())
    def init() = {
      tx := 0.B
      clearToSend := 0.B
    }
  }
  class SramPort extends Bundle {
    val address = Output(UInt(20.W))
    val data = Analog(16.W)
    val outputEnable = Output(Bool()) // active low
    val writeEnable = Output(Bool()) // active low
    val chipSelect = Output(Bool()) // active low
    val mask = Output(Vec(2, Bool())) // active low
    def init() = {
      address := DontCare
      outputEnable := 1.B
      writeEnable := 1.B
      chipSelect := 1.B
      mask := VecInit(1.B, 1.B)
    }
  }
  class SdramPort extends Bundle {
    val address = Output(UInt(13.W))
    val bankAddress = Output(UInt(2.W))
    val data = Analog(32.W)
    val mask = Output(Vec(4, Bool()))
    // row address strobe
    val ras = Output(Bool()) // active low
    // column address strobe
    val cas = Output(Bool()) // active low
    val clockEnable = Output(Bool())
    val clock = Output(Clock())
    val writeEnable = Output(Bool()) // active low
    val chipSelect = Output(Bool()) // active low
    def init() = {
      address := DontCare
      bankAddress := DontCare
      mask := VecInit(0.B, 0.B, 0.B, 0.B)
      ras := 1.B
      cas := 1.B
      clockEnable := 1.B
      clock := 0.B.asClock
      writeEnable := 1.B
      chipSelect := 1.B
    }
  }
}
