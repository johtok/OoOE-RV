package ooo

import chisel3._
import chisel3.util.log2Ceil
package object util {

  implicit class UIntSeqExtension(x: UInt) {
    def groupBits(n: Int): Seq[UInt] = x.asBools.grouped(n).map(VecInit(_).asUInt).toSeq
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



}
