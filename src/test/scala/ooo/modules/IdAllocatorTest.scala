package ooo.modules

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

import scala.util.Random
import ooo.util.TestingUtils._
import ooo.util._

class IdAllocatorTest extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "IdAllocator"

  it should "allocate n ids sequentially" in {
    val n = Random.nextInt(2 until 64)
    test(new IdAllocator(n)) { dut =>
      for(id <- 0 until n) {
        dut.io.alloc.id.expect(id)
        dut.io.alloc.offer.expect(1.B)
        dut.io.alloc.take.poke(1.B)
        dut.clock.step()
      }
      dut.io.alloc.offer.expect(0.B)
    }
  }

  it should "release all n ids" in {
    val n = Random.nextInt(2 until 64)
    test(new IdAllocator(n)) { dut =>
      dut.io.alloc.take.poke(1.B)
      dut.clock.step(n)
      dut.io.alloc.take.poke(0.B)

      dut.io.alloc.offer.expect(0.B)

      for(id <- 0 until n) {
        dut.io.dealloc.oldestAllocatedId.expect(id)
        dut.io.dealloc.nextOldestAllocatedId.expect((id+1) % n)
        dut.io.dealloc.noAllocations.expect(0.B)
        dut.io.dealloc.release.poke(1.B)
        dut.clock.step()
      }

      dut.io.dealloc.noAllocations.expect(1.B)
    }
  }


  it should "allocate 4n ids" in {
    val n = Random.nextInt(16 until 64)
    test(new IdAllocator(n)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.clock.setTimeout(0)

      fork {
        for (id <- 0 until 4 * n) {
          dut.clock.stepUntil(dut.io.alloc.offer.peekBoolean())
          dut.io.alloc.id.expect(id % n)
          dut.io.alloc.take.poke(1.B)
          dut.clock.step()
          dut.io.alloc.take.poke(0.B)
        }
      }.fork {
        dut.clock.step(40)
        for (id <- 0 until 4 * n) {
          dut.io.dealloc.oldestAllocatedId.expect(id % n)
          dut.io.dealloc.nextOldestAllocatedId.expect((id + 1) % n)
          dut.clock.stepUntil(!dut.io.dealloc.noAllocations.peekBoolean())
          dut.io.dealloc.release.poke(1.B)
          dut.clock.step()
          dut.io.dealloc.release.poke(0.B)
          dut.clock.step(Random.nextInt(0 until 5))
        }
      }.join()

    }
  }

}
