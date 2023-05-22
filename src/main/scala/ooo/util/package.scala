package ooo

import chisel3._
import chisel3.internal.firrtl.Width
import chisel3.util.log2Ceil
import ooo.util.barrelRotate

import scala.annotation.tailrec
import scala.util.Random
package object util {

  implicit class UIntExtension(x: UInt) {
    def groupBits(n: Int): Seq[UInt] = x.asBools.grouped(n).map(VecInit(_).asUInt).toSeq
    def groupBytes: Seq[UInt] = groupBits(8)
  }

  implicit class UIntSeqExtension(xs: Seq[UInt]) {
    def concatenated: UInt =
      xs.tail.foldLeft(xs.head) { case (acc, part) => part ## acc }


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

    def nextBranchId()(implicit c: Configuration) = nextUInt(c.snapshotIdWidth)

  }

  def nextPow2(x: Int): Int = pow2(log2Ceil(x))

  def roundAt(p: Int)(n: Double): Double = {
    val s = math pow(10, p); (math round n * s) / s
  }


  object LookUp {
    import chisel3.util.MuxLookup
    def apply[T <: Data, P <: Data](key: T, default: P, cases: (T, P)*): P = {
      MuxLookup(key.asUInt, default, cases.map { case (k, v) => (k.asUInt, v) })
    }
  }

  def DatatoByteVec[T <: Bits](int: T): Vec[UInt] = {
    return VecInit(int(7, 0), int(15, 8), int(23, 16), int(31, 24))
  }

  def byteVecToUInt(bytes: Vec[UInt]): UInt = {
    return bytes(3) ## bytes(2) ## bytes(1) ## bytes(0)
  }


  @tailrec
  def shifted[T](seq: Seq[T], shiftIn: Seq[T], i: Int): Seq[T] = {
    if (i == 0) seq else shifted(shiftIn.last +: seq, shiftIn.dropRight(1), i - 1)
  }

  @tailrec
  def rotated[T](seq: Seq[T], i: Int): Seq[T] = {
    i match {
      case 0 => seq
      case _ => rotated(seq.last +: seq.dropRight(1), i - 1)
    }
  }

  object BarrelShifter {
    def apply[T <: Data](in: Vec[T], shiftIn: Vec[T], shamt: UInt): Vec[T] = {
      barrelShift(in, shiftIn, shamt.asBools, 1, 0.U.asTypeOf(in.head)) { case (d, l, r) =>
        Mux(d, l, r)
      }.toVec
    }
  }

  object BarrelRotater {
    def apply[T <: Data](in: Seq[T], shamt: UInt): Seq[T] = {
      barrelRotate(in, shamt.asBools, 1) { case (d, l, r) => Mux(d, l, r) }.toVec
    }
  }

  @tailrec
  def barrelShift[T, D](in: Seq[T], shiftIn: Seq[T], ds: Seq[D], i: Int, zero: T)(sel: (D, T, T) => T): Seq[T] = {

    if (ds.isEmpty) {
      in
    } else {
      val shift = shifted(in, shiftIn, i)
      val muxed = in.padTo(shift.length, zero).zip(shift).map { case (noShift, shift) =>
        sel(ds.head, shift, noShift)
      }
      barrelShift(muxed, shiftIn.dropRight(i), ds.tail, i << 1, zero)(sel)
    }

  }

  def barrelRotate[T, D](in: Seq[T], ds: Seq[D], i: Int)(sel: (D, T, T) => T): Seq[T] = {
    ds match {
      case Seq() => in
      case d :: _ =>
        val rot = rotated(in, i)
        val muxed = in.zip(rot).map { case (noShift, shift) => sel(d, shift, noShift) }
        barrelRotate(muxed, ds.tail, i << i)(sel)
    }
  }


}

object Teste extends App {
  val in = Seq(3,0,1,2).reverse
  val res = barrelRotate(in, Seq(true, false, false), 1) { case (d,l,r) => if(d) l else r }
  print(res.reverse)
}