package ooo.modules

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

import ooo.util._

import scala.collection.mutable.ArrayBuffer
import scala.util.Random


class SRAMTest extends AnyFlatSpec with ChiselScalatestTester {

  "SRAM" should "store correct values at correct addresses" in {

    test(new SRAM(256)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

      val mem = ArrayBuffer.fill(1024)(0)

      for(i <- 0 until 256) {
        val data = Random.nextUInt(32.W)
        val Seq(b0, b1, b2, b3) = UIntToBytes(data)
        mem(i*4) = b0
        mem(i*4 + 1) = b1
        mem(i*4 + 2) = b2
        mem(i*4 + 3) = b3

        dut.io.request.bits.isWrite.poke(1.B)
        dut.io.request.bits.Address.poke((i * 4).U)
        dut.io.request.bits.mask.foreach(_.poke(1.B))
        dut.io.request.bits.WriteData.poke(data)

        timescope {
          dut.io.request.valid.poke(1.B)
          dut.clock.step()
        }

        dut.clock.step(Random.nextInt(10))

      }

      for(i <- 0 until 1000) {

        val a = Random.nextUInt(0 to 1020).litValue.toInt

        val expected = read(mem, a)

        dut.io.request.bits.isWrite.poke(0.B)
        dut.io.request.bits.Address.poke(a.U)

        timescope {
          dut.io.request.valid.poke(1.B)
          dut.clock.step()
        }

        dut.io.response.valid.expect(1.B)
        dut.io.response.bits.readData.expect(expected.U)

        dut.clock.step(Random.nextInt(10))

      }



    }

  }


  def UIntToBytes(x: UInt): Seq[Int] = {
    val v = x.litValue
    Seq(
      v & 0xFF,
      (v >> 8) & 0xFF,
      (v >> 16) & 0xFF,
      (v >> 24) & 0xFF
    ).map(_.toInt)
  }

  def read(mem: ArrayBuffer[Int], a: Int): BigInt = {

    val b0 = BigInt(mem(a))
    val b1 = BigInt(mem(a+1))
    val b2 = BigInt(mem(a+2))
    val b3 = BigInt(mem(a+3))

    (b3 << 24) | (b2 << 16) | (b1 << 8) | b0

  }


}
