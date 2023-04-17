package ooo

import chisel3._
import chisel3.internal.firrtl.Width
import chisel3.util.log2Ceil

import scala.util.Random
package object util {

  implicit class UIntExtension(x: UInt) {
    def groupBits(n: Int): Seq[UInt] = x.asBools.grouped(n).map(VecInit(_).asUInt).toSeq
  }

  implicit class UIntSeqExtension(xs: Seq[UInt]) {
    def concatenated: UInt = xs match {
      case head::tail => tail.foldLeft(head) { case (acc, part) => part ## acc }
    }
  }

  implicit class PairConnector[T <: Data](ps: Seq[(T,T)]) {
    def connectPairs(): Unit = ps.foreach { case (port, value) => port := value }
    def bulkConnectPairs(): Unit = ps.foreach { case (port, value) => port <> value }
  }

  implicit class SIntExtension(x: SInt) {
    def sign: Bool = x(x.getWidth - 1)
  }



  object TickGen {
    def apply(clockFrequency: Int, tickFrequency: Int): Bool = {
      val maxCount = clockFrequency / tickFrequency

      val counter = RegInit(maxCount.U)
      counter := counter - 1.U
      val tick = counter === 0.U
      when(tick) { counter := maxCount.U }

      tick
    }
  }

  object ToggleReg {
    def apply(init: Bool, event: Bool = 1.B): Bool = {
      val toggleReg = RegInit(init)
      when(event) {
        toggleReg := !toggleReg
      }
      toggleReg
    }
  }

  implicit class BundleExpander[T <: Bundle](b: T) {
    def expand(assignments: T => Any*): T = {
      assignments.foreach(f => f(b))
      b
    }
  }

  implicit class SeqDataExtension[T <: Data](x: Seq[T]) {
    def toVec: Vec[T] = VecInit(x)
  }

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


  object LookUp {
    import chisel3.util.MuxLookup
    def apply[T <: Data, P <: Data](key: T, default: P, cases: (T, P)*): P = {
      MuxLookup(key.asUInt, default, cases.map { case (k, v) => (k.asUInt, v) })
    }
  }



}
