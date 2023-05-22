package ooo.boards

import chisel3._
import chisel3.experimental.Analog
import chisel3.util.{Counter, Fill, log2Ceil}
import ooo.{Configuration, Core}
import ooo.boards.DE2_115.{SdramPort, SevenSegmentDigit, SramPort, UartPort}
import ooo.modules.{BufferedTx, RAM, Rx}
import ooo.util.{BundleExpander, Program, TickGen, TriStateDriver, UIntExtension}


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
    io.redLed := 0.U
    io.greenLed := 0.U

    implicit val c = Configuration.default()


    val core = Module(new Core(Program.load("src/test/programs/loop.bin"), c))

    val mem = Module(new RAM( 0x1000))
    mem.io.request.valid := 0.B
    mem.io.request.bits := DontCare
    mem.io.response.ready := 1.B

    val uartSender = Module(new BufferedTx(50000000, 9600))
    uartSender.io.channel.valid := 0.B
    uartSender.io.channel.bits := DontCare
    val uartReceiver = Module(new Rx(50000000, 9600))
    uartReceiver.io.channel.ready := 0.B
    io.uart.tx := uartSender.io.txd
    uartReceiver.io.rxd := io.uart.rx

    val digitReg = RegInit(0.U((8*4).W))

    val valid = core.io.mem.get.request.valid
    val isWrite = core.io.mem.get.request.bits.isWrite
    val writeData = core.io.mem.get.request.bits.WriteData
    val address = core.io.mem.get.request.bits.Address

    val ramAccess = address(31, 12) === 0.U
    val digitAccess = address(31, 12) === 1.U
    val uartAccess = address(31, 12) === 2.U

    when(digitAccess) {
      when(valid && isWrite) {
        digitReg := writeData
      }
    }.elsewhen(uartAccess) {
      when(valid && isWrite) {
        uartSender.io.channel.expand(
          _.valid := 1.B,
          _.bits := writeData(7,0)
        )
      }
    }.otherwise {
      mem.io.request <> core.io.mem.get.request
    }

    when(RegNext(digitAccess && !isWrite)) {
      core.io.mem.get.response.expand(
        _.valid := 1.B,
        _.bits.readData := digitReg
      )
    }.elsewhen(RegNext(uartAccess && !isWrite)) {
      val statusRead = RegNext(address(0).asBool)
      uartReceiver.io.channel.ready := !statusRead
      core.io.mem.get.response.expand(
        _.valid := 1.B,
        _.bits.readData := Mux(statusRead, uartReceiver.io.channel.valid.asUInt, uartReceiver.io.channel.bits)
      )
    }.elsewhen(RegNext(ramAccess && !isWrite)) {
      core.io.mem.get.response.expand(
        _.valid := 1.B,
        _.bits.readData := digitReg
      )
    }.otherwise {
      core.io.mem.get.response.expand(
        _.valid := 0.B,
        _.bits.readData := DontCare
      )
    }


    digitReg
      .groupBits(4)
      .zip(io.digits)
      .foreach { case (num, digit) => digit.drive(num) }

    core.io.mem.get.request.ready := 1.B

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
