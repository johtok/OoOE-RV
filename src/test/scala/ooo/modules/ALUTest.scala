package ooo.modules

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class ALUTest extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "ALU"

  val XLEN = 32

  it should "ADD 2 values together" in {
    val func3 = 0
    val func_mod = 0
    val input1 = 987234
    val input2 = 201294
    val output = input1 + input2
    test(new ALU(XLEN)) { 
      dut =>
        dut.io.in.input1.poke(input1)
        dut.io.in.input2.poke(input2)
        dut.io.in.func3.poke(func3)
        dut.io.in.func_mod.poke(func_mod)
        dut.clock.step()
        dut.io.out.output.expect(output)
    }
  }
  it should "SUBtract a value" in {
    val func3 = 0
    val func_mod = 1
    val input1 = 23451
    val input2 = 581923
    val output = {
      val res = input1 - input2
      if (res >= 0) res else 2L * Int.MaxValue + res + 2 // We have 0 twice, and the complement is + 1.
    };
    test(new ALU(XLEN)) { 
      dut =>
        dut.io.in.input1.poke(input1)
        dut.io.in.input2.poke(input2)
        dut.io.in.func3.poke(func3)
        dut.io.in.func_mod.poke(func_mod)
        dut.clock.step()
        dut.io.out.output.expect(output)
    }
  }
  it should "Compare logical 2 values" in {
    val func3 = 2
    val func_mod = 0
    val input1 = 81233
    val input2 = Int.MaxValue - 49124L
    val output = if (input1 < input2) 1 else 0
    test(new ALU(XLEN)) { 
      dut =>
        dut.io.in.input1.poke(input1)
        dut.io.in.input2.poke(input2)
        dut.io.in.func3.poke(func3)
        dut.io.in.func_mod.poke(func_mod)
        dut.clock.step()
        dut.io.out.output.expect(output)
    }
  }
  it should "Compare logical 2 values, zero-extend" in {
    val func3 = 3
    val func_mod = 0
    val input1 = 987234
    val input2 = 125121
    val output = if (input1 < input2) 1 else 0
    test(new ALU(XLEN)) { 
      dut =>
        dut.io.in.input1.poke(input1)
        dut.io.in.input2.poke(input2)
        dut.io.in.func3.poke(func3)
        dut.io.in.func_mod.poke(func_mod)
        dut.clock.step()
        dut.io.out.output.expect(output)
    }
  }
  it should "XOR 2 values bitwise" in {
    val func3 = 4
    val func_mod = 0
    val input1 = 987234
    val input2 = Int.MaxValue - 9124L
    val output = input1 ^ input2
    test(new ALU(XLEN)) { 
      dut =>
        dut.io.in.input1.poke(input1)
        dut.io.in.input2.poke(input2)
        dut.io.in.func3.poke(func3)
        dut.io.in.func_mod.poke(func_mod)
        dut.clock.step()
        dut.io.out.output.expect(output)
    }
  }
  it should "OR 2 values bitwise" in {
    val func3 = 6
    val func_mod = 0
    val input1 = 19532
    val input2 = 1092422
    val output = input1 | input2
    test(new ALU(XLEN)) { 
      dut =>
        dut.io.in.input1.poke(input1)
        dut.io.in.input2.poke(input2)
        dut.io.in.func3.poke(func3)
        dut.io.in.func_mod.poke(func_mod)
        dut.clock.step()
        dut.io.out.output.expect(output)
    }
  }
  it should "AND 2 values bitwise" in {
    val func3 = 7
    val func_mod = 0
    val input1 = 928751
    val input2 = 1249853
    val output = input1 & input2
    test(new ALU(XLEN)) { 
      dut =>
        dut.io.in.input1.poke(input1)
        dut.io.in.input2.poke(input2)
        dut.io.in.func3.poke(func3)
        dut.io.in.func_mod.poke(func_mod)
        dut.clock.step()
        dut.io.out.output.expect(output)
    }
  }
}
