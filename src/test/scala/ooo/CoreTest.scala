package ooo

import chisel3._
import chiseltest._
import ooo.util.Program
import org.scalatest.flatspec.AnyFlatSpec
import ooo.util.Program._

class CoreTest extends AnyFlatSpec with ChiselScalatestTester {

  val path = "src/test/programs"
  getBinFiles(path).foreach { bin =>
    val program = Program.load(s"$path/$bin")
    "Core" should s"execute $bin correctly" in {
      test(new Core(program, Configuration.default().copy(issueQueueSize = 2, simulation = true))).withAnnotations(Seq(VerilatorBackendAnnotation,WriteVcdAnnotation)) { dut =>
        println(s"$bin...")
        while(!dut.debug.get.ecall.peekBoolean()) dut.clock.step()
        dut.clock.step()
        dut.debug.get.regfile.zip(program.result).foreach { case (reg, value) => reg.expect(value.U) }
        println(s"$bin ✓")
      }
    }
  }


}