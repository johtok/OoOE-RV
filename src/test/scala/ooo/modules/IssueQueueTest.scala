package ooo.modules

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

import scala.util.Random
import ooo.util.TestingUtils._

class IssueQueueTest extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "IssueQueue"

  it should "allocate n spaces sequentially" in {
    val n = Random.nextInt(2 until 32)
    //val n = 16

    test(new IssueQueue(n+2,n)) { dut =>
      for(id <- 0 until n) {

        /*
        dut.io.alloc.id.expect(id)
        dut.io.alloc.offer.expect(1.B)

        */
        dut.io.Alloc.ready.expect(1.B)
        dut.io.Alloc.bits.rdTag.poke(id.U)
        dut.io.Alloc.bits.rs1Tag.poke(id.U)
        dut.io.Alloc.bits.rs2Tag.poke((id+1).U)
        dut.io.Alloc.bits.OP.poke(id.U)
        dut.io.Alloc.bits.Offset_PC.poke(id.U)

        dut.io.Alloc.valid.poke(1.B)
        
        dut.clock.step()
        //println(id)
      }
      dut.io.Alloc.ready.expect(0.B)
    }
  }

  it should "deallocate n spaces sequentially" in {
    val n = Random.nextInt(2 until 64)
    //val n = 16

    println(n)

    test(new IssueQueue(n+10,n)) { dut =>
      for(id <- 0 until n) {

        /*
        dut.io.alloc.id.expect(id)
        dut.io.alloc.offer.expect(1.B)

        */
        dut.io.Alloc.ready.expect(1.B)
        dut.io.Alloc.bits.rdTag.poke(id.U)
        dut.io.Alloc.bits.rs1Tag.poke(id.U)
        dut.io.Alloc.bits.rs2Tag.poke((id+1).U)
        dut.io.Alloc.bits.OP.poke(id.U)
        dut.io.Alloc.bits.Offset_PC.poke(id.U)

        dut.io.Alloc.valid.poke(1.B)
        
        dut.clock.step()
        //println(id)
      }

      dut.io.Alloc.ready.expect(0.B)

      dut.io.Event.poke(true.B)
      dut.io.EventTag.poke(0.U)
      dut.io.Issue.ready.poke(true.B)
      dut.io.Issue.valid.expect(false.B)

      println("1")


      dut.clock.step()

      dut.io.Event.poke(true.B)
      dut.io.EventTag.poke(1.U)
      dut.io.Issue.ready.poke(true.B)
      dut.io.Issue.valid.expect(false.B)

      println("2")


      dut.clock.step()


      for(tag <- 0 until n){

        dut.io.Event.poke(true.B)
        dut.io.EventTag.poke((tag + 2).U)
        dut.io.Issue.ready.poke(true.B)
        dut.io.Issue.valid.expect(true.B)

        dut.io.Issue.bits.rdTag.expect(tag.U)

        dut.clock.step()
      }

      dut.io.Issue.ready.poke(true.B)
      dut.io.Issue.valid.expect(false.B)

    }

  }
  /*

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

  */
}
