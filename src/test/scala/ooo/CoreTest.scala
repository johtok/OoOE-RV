package ooo

import chisel3._
import chiseltest._
import ooo.util.Program
import org.scalatest.flatspec.AnyFlatSpec
import ooo.util.Program._

class CoreTest extends AnyFlatSpec with ChiselScalatestTester {

  val path = "src/test/programs"
  getBinFiles(path).filter(_ == "addlarge.bin").foreach { bin =>
    "Core" should s"execute $bin correctly" in {
      test(new Core(Program.load(s"$path/$bin"), Configuration.default())).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
        print(bin)
        dut.clock.step(30)
        println(s" ✓")
      }
    }
  }


}