package ooo

import chisel3._
import chiseltest._
import ooo.boards.Basys3
import org.scalatest.flatspec.AnyFlatSpec

class Basys3Sim extends AnyFlatSpec with ChiselScalatestTester {

  "Basys" should "Work" in {
    test(new Basys3(Configuration.default())).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

      dut.clock.step(500)

    }
  }

}
