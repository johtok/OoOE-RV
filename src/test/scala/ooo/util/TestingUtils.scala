package ooo.util

import chisel3._
import chisel3.internal.firrtl.Width
import chisel3.util.log2Ceil
import chiseltest._
import ooo.Configuration

import scala.util.Random

object TestingUtils {

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

}
