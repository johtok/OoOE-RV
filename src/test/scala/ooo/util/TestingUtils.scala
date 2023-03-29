package ooo.util

import chisel3._
import chisel3.internal.firrtl.Width
import chisel3.util.log2Ceil
import chiseltest._
import ooo.Configuration

import scala.util.Random

object TestingUtils {

  def pow2(x: Int): Int = math.pow(2, x).toInt
  implicit class RandomExtension(r: Random.type) {
    def nextInt(range: Range): Int = range.start + Random.nextInt(range.size)
    def nextPow2(range: Range): Int = {
      val max = log2Ceil(range.end)
      val min = log2Ceil(range.start)
      pow2(nextInt(min to max))
    }
    def nextUInt(range: Range): UInt = nextInt(range).U
    def nextUInt(width: Width): UInt = {
      val max = pow2(width.get.toInt)
      nextUInt(0 until max)
    }
    def nextArchRegister(): UInt = nextUInt(5.W)
    def nextPhysRegister()(implicit c: Configuration) = nextUInt(c.physRegisterIdWidth)
    def nextBranchId()(implicit c: Configuration) = nextUInt(c.branchIdWidth)
    def nextLoadId()(implicit c: Configuration) = nextUInt(c.loadIdWidth)
    def nextStoreId()(implicit c: Configuration) = nextUInt(c.storeIdWidth)
  }


  implicit class SteppingExtension(c: Clock) {
    def stepUntil(p: => Boolean): Unit = while(!p) c.step()
  }

  implicit class PokeExpectExtension[T <: Bundle](x: T) {
    def poke(ps: T => (Data, Data)*): Unit = {
      ps.map(_(x)).foreach { case (field, value) => field.poke(value) }
    }
    def expect(es: T => (Data, Data)*): Unit = {
      es.map(_(x)).foreach { case (field, expected) => field.expect(expected) }
    }
  }

  object RandomConfiguration {
    def apply(): ooo.Configuration = {
      val robSize = Random.nextPow2(32 to 1024)
      ooo.Configuration(
        robSize,
        Random.nextPow2(16 to 64),
        Random.nextPow2(4 to 32),
        Random.nextPow2(4 to 32),
        Random.nextPow2(2 to 8),
        Seq.fill(32)(Random.nextInt(0 until robSize))
      )
    }
  }

}
