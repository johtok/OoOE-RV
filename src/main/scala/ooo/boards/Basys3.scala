package ooo.boards

import chisel3._
import chisel3.util.UIntToOH
import ooo.{Configuration, Core}
import ooo.boards.Basys3.SevenSegmentDisplay
import ooo.modules.{BufferedTx, Rx, SRAM}
import ooo.util.Xilinx.MMCME2_ADV
import ooo.util.{BundleExpander, Program, SeqDataExtension, TickGen, UIntExtension}

class Basys3(c: Configuration) extends Module {

  implicit val conf = c

  val io = IO(new Bundle {
    val digits = new Basys3.SevenSegmentDisplay
    val uart = new Basys3.UartPort
  })


  val freq = 80000000

  withClock(MMCME2_ADV(clock, reset, 100.0 -> freq/1000000)) {

    val core = Module(new Core(Program.load("src/test/programs/demo.bin"), c))

    val mem = Module(new SRAM(0x1000, Some(Program.load("src/test/programs/demo.bin").getBytes.map(_.litValue))))
    mem.io.request.valid := 0.B
    mem.io.request.bits := DontCare
    mem.io.response.ready := 1.B

    val uartSender = Module(new BufferedTx(freq, 9600))
    uartSender.io.channel.valid := 0.B
    uartSender.io.channel.bits := DontCare
    val uartReceiver = Module(new Rx(freq, 9600))
    uartReceiver.io.channel.ready := 0.B
    io.uart.tx := uartSender.io.txd
    uartReceiver.io.rxd := io.uart.rx

    val digitReg = RegInit(0.U(16.W))

    val valid = core.io.mem.get.request.valid
    val isWrite = core.io.mem.get.request.bits.isWrite
    val writeData = core.io.mem.get.request.bits.WriteData
    val address = core.io.mem.get.request.bits.Address

    val ramAccess = address(31, 12) === 0.U
    val digitAccess = address(31, 12) === 2.U
    val uartAccess = address(31, 12) === 3.U

    when(ramAccess) {
      mem.io.request <> core.io.mem.get.request
    }.elsewhen(digitAccess) {
      when(valid && isWrite) {
        digitReg := writeData
      }
    }.elsewhen(uartAccess) {
      when(valid && isWrite) {
        uartSender.io.channel.expand(
          _.valid := 1.B,
          _.bits := writeData(7, 0)
        )
      }
    }.otherwise {

    }

    when(RegNext(ramAccess && !isWrite)) {
      core.io.mem.get.response <> mem.io.response
    }.elsewhen(RegNext(digitAccess && !isWrite)) {
      core.io.mem.get.response.expand(
        _.valid := 1.B,
        _.bits.readData := digitReg
      )
    }.elsewhen(RegNext(uartAccess && !isWrite)) {
      val statusRead = RegNext(address(0).asBool)
      uartReceiver.io.channel.ready := !statusRead
      core.io.mem.get.response.expand(
        _.valid := 1.B,
        _.bits.readData := Mux(statusRead, uartSender.io.channel.ready.asUInt ## uartReceiver.io.channel.valid.asUInt, uartReceiver.io.channel.bits)
      )
    }.otherwise {
      core.io.mem.get.response.expand(
        _.valid := 0.B,
        _.bits.readData := DontCare
      )
    }

    core.io.mem.get.request.ready := 1.B

    io.digits.drive(digitReg, freq)
  }


}

object Basys3 {

  class SevenSegmentDisplay extends Bundle {
    val segments = Output(UInt(7.W)) // active low
    val digitEnable = Output(UInt(4.W)) // active low

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
    ).map("b" + _).map(_.U))

    def drive(bits: UInt, freq: Int) = {
      val tick = TickGen(freq, 1000)
      val digitSelect = RegInit(0.U(2.W))
      when(tick) { digitSelect := digitSelect + 1.U }
      segments := table(bits.groupBits(4).toVec.apply(digitSelect))
      digitEnable := UIntToOH(digitSelect, 4)
    }
  }

  class UartPort extends Bundle {
    val rx = Input(Bool())
    val tx = Output(Bool())
  }

  def main(args: Array[String]): Unit = emitVerilog(new Basys3(Configuration.default()))


}
