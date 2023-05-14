package ooo.modules

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import ooo.Configuration

import scala.collection.mutable.ArrayBuffer
import scala.util.Random
import ooo.util._

import ooo.util.TestingUtils._

class StateArch2PhysMapTest extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "StateArch2PhysMap"

  it should "store the correct mapping" in {
    implicit val c = Configuration.default()
    test(new StateArch2PhysMap()).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

      val map = ArrayBuffer.fill(32)(0)

      for(i <- 0 until 32) {
        val pid = Random.nextPhysRegister()
        map(i) = pid.litValue.toInt
        dut.io.write.poke(
          _.rd -> i.U,
          _.prd -> pid,
          _.write -> 1.B
        )
        dut.clock.step()
      }
      dut.io.write.write.poke(0.B)

      for (i <- 0 until 32 by 2) {
        dut.io.read.poke(
          _.rs(0) -> i.U,
          _.rs(1) -> (i+1).U
        )
        dut.io.read.expect(
          _.prs(0) -> map(i).U,
          _.prs(1) -> map(i+1).U
        )
      }

    }
  }

  it should "mark the correct pids as allocated" in {
    implicit val c = Configuration.default()
    test(new StateArch2PhysMap()).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

      val map = ArrayBuffer.fill(32)(0)

      for (i <- 0 until 32) {
        val pid = Random.nextPhysRegister()
        map(i) = pid.litValue.toInt
        dut.io.write.poke(
          _.rd -> i.U,
          _.prd -> pid,
          _.write -> 1.B
        )
        dut.clock.step()
      }
      dut.io.write.write.poke(0.B)

      for (i <- 0 until c.reorderBufferSize) {
        dut.io.allocationCheck.pr(0).poke(i.U)
        dut.io.allocationCheck.isAllocated(0).expect(
          map.contains(i).B
        )
        dut.clock.step()
      }

    }
  }



}
