package ooo.modules

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import ooo.Configuration
import ooo.Types.EventType._



import scala.util.Random
import ooo.util.TestingUtils._

class IssueQueueTest extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "IssueQueue"

  val n = 2 + Random.nextInt(30)

  implicit val Config = Configuration.default().copy(issueQueueSize = n, simulation = true)

  it should "allocate n spaces sequentially" in {
    //val n = 2 + Random.nextInt(30)
    //val n = 16

    test(new IssueQueue()) { dut =>
      for(id <- 0 until n) {

        /*
        dut.io.alloc.id.expect(id)
        dut.io.alloc.offer.expect(1.B)

        */
        dut.io.Alloc.ready.expect(1.B)
        dut.io.Alloc.bits.prd.poke(id.U)

        dut.io.Alloc.bits.prs(0).ready.poke(false.B)
        dut.io.Alloc.bits.prs(1).ready.poke(false.B)

        dut.io.Alloc.bits.prs(0).id.poke(id.U)
        dut.io.Alloc.bits.prs(1).id.poke((id+1).U)

        /*

        dut.io.Alloc.bits.opcode := 0.U
        dut.io.Alloc.bits.func := 0.U


        dut.io.Alloc.bits.immediate := 0.U
        dut.io.Alloc.bits.pc := 0.U


        dut.io.Alloc.bits.snapshotId := 0.U
        dut.io.Alloc.bits.branchPrediction := false.B

        //dut.io.Alloc.bits.OP.poke(id.U)
        //dut.io.Alloc.bits.Offset_PC.poke(id.U)

        */

        dut.io.Alloc.valid.poke(1.B)
        
        dut.clock.step()
        //println(id)
      }
      dut.io.Alloc.ready.expect(0.B)
    }
  }

  

  it should "deallocate n spaces sequentially" in {

    test(new IssueQueue()) { dut =>
      for(id <- 0 until n) {

        /*
        dut.io.alloc.id.expect(id)
        dut.io.alloc.offer.expect(1.B)

        */
        dut.io.Alloc.ready.expect(1.B)
        dut.io.Alloc.bits.prd.poke(id.U)

        dut.io.Alloc.bits.prs(0).ready.poke(false.B)
        dut.io.Alloc.bits.prs(1).ready.poke(false.B)

        dut.io.Alloc.bits.prs(0).id.poke(id.U)
        dut.io.Alloc.bits.prs(1).id.poke((id+1).U)

        dut.io.Alloc.valid.poke(1.B)
        
        dut.clock.step()
        //println(id)
      }
      
      dut.io.Alloc.valid.poke(0.B)
      dut.io.Alloc.ready.expect(0.B)

      //dut.io.Event.poke(true.B)
      //dut.io.EventTag.poke(0.U)

      dut.io.event.valid.poke(true.B)
      dut.io.event.bits.eventType.poke(CompletionWithValue)
      dut.io.event.bits.pr.poke(0.U)
      dut.io.event.bits.writeBackValue.poke(0.U)

      dut.io.Issue.ready.poke(true.B)
      dut.io.Issue.valid.expect(false.B)

      //println("1")


      dut.clock.step()

      dut.io.event.valid.poke(true.B)
      dut.io.event.bits.eventType.poke(CompletionWithValue)
      dut.io.event.bits.pr.poke(1.U)
      dut.io.event.bits.writeBackValue.poke(0.U)

      dut.io.Issue.ready.poke(true.B)
      dut.io.Issue.valid.expect(false.B)

      //println("2")


      dut.clock.step()


      for(tag <- 0 until n){

        /*

        dut.io.Event.poke(true.B)
        dut.io.EventTag.poke((tag + 2).U)

        */

        dut.io.event.valid.poke(true.B)
        dut.io.event.bits.eventType.poke(CompletionWithValue)
        dut.io.event.bits.pr.poke((tag+2).U)
        dut.io.event.bits.writeBackValue.poke(0.U)


        dut.io.Issue.ready.poke(true.B)
        dut.io.Issue.valid.expect(true.B)

        dut.io.Issue.bits.prd.expect(tag.U)

        dut.clock.step()
      }

      dut.io.Issue.ready.poke(true.B)
      dut.io.Issue.valid.expect(false.B)

    }

  }



  /*

  it should "release all n ids" in {
    val n = 2 + Random.nextInt(62)
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
