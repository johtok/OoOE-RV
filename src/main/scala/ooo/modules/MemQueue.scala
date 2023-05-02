package ooo.modules

import chisel3._
import chisel3.experimental.BundleLiterals.AddBundleLiteralConstructor
import chisel3.internal.firrtl.Width
import ooo.util.SeqDataExtension
//import chisel3.util.{Fill, log2Ceil, MixedVec}
import ooo.Types._
import ooo.Types.EventType._

import ooo.modules.IdAllocator.{DeallocationPort,AllocatorStatePort,shouldBeKilled}

import ooo.Configuration
//import chisel3.util.{Decoupled, MuxCase, Valid}
import chisel3.util._
import ooo.modules.MemQueue.{QueueElementPort}

import ooo.util.BundleExpander


object MemQueue {
  class QueueElementPort(implicit c: Configuration) extends Bundle{
    val In = new MemPackage
    val empty = Bool()
  }
}
class MemQueue()(implicit c: Configuration) extends Module {
  val io = IO(new Bundle {
    val Package = Flipped(Decoupled(new MemPackage))
    //val Write = Decoupled(new WritePort)
    val EventOut = Valid(new Event)
    val MemPort = new MemPort
    //val Retire = Flipped(Decoupled(PhysRegisterId()))

    val event = Flipped(Valid(new Event))

    val Dealloc = Flipped(new DeallocationPort(c.physRegisterIdWidth))
    val StatePort = Flipped(new AllocatorStatePort(c.physRegisterIdWidth))

    val Full = Output(Bool())
  })

  val Fullsize = c.memQueueSize - 2

  //io.Retire.ready := io.MemPort.ready

  io.MemPort.request.bits := DontCare

  io.MemPort.request.valid := false.B
  io.MemPort.response.ready := false.B

  io.EventOut.bits := DontCare
  io.EventOut.valid := false.B

  io.Dealloc.release := DontCare

  val MemQueue = RegInit(Vec(c.memQueueSize, new QueueElementPort()), Seq.fill(c.memQueueSize) {
    new QueueElementPort().Lit(
      _.empty -> 1.B
    )
  }.toVec)

  io.Package.ready := VecInit(Seq.tabulate(c.memQueueSize)(n => MemQueue(n).empty)).reduceTree(_ | _)

  val FullCnt = Wire(UInt(log2Ceil(c.memQueueSize).W))
  
  FullCnt := PopCount(Seq.tabulate(c.memQueueSize)(n => !MemQueue(n).empty)) 

  io.Full := (FullCnt >= Fullsize.U).asBool

  val Kill = Wire(Bool())
  Kill := io.event.valid && io.event.bits.eventType === Branch


  val WriteCarry = Wire(Vec(c.memQueueSize + 1, Bool()))
  WriteCarry(0) := false.B

  for(i <- 0 until c.memQueueSize + 1){
    WriteCarry(i) := false.B
  }



  // Add kill logic. 

  when(io.Package.valid && !(io.event.bits.eventType.isOneOf(EventType.Jump, EventType.Branch) && shouldBeKilled(io.Package.bits.prd, io.event.bits.pr, io.StatePort.oldest, io.StatePort.youngest, io.StatePort.wrapped))){
    for(i <- 0 until c.memQueueSize){
      when(!WriteCarry(i)){
        when(MemQueue(i).empty){
          MemQueue(i).In := io.Package.bits 
          MemQueue(i).empty := false.B
          WriteCarry(i+1) := true.B
        }.otherwise{
          WriteCarry(i+1) := false.B
        }
      }.otherwise{
        WriteCarry(i+1) := true.B
      }
    }
  } 

  // Kill 

  when(Kill){
    for(i <- 0 until c.memQueueSize){
      when(shouldBeKilled(MemQueue(i).In.prd, io.event.bits.pr, io.StatePort.oldest, io.StatePort.youngest, io.StatePort.wrapped)){
        MemQueue(i).empty := true.B
      }
    }
  }


  //Transmit data

  //val ExpectData = RegInit(0.B)

  val ReadData = Reg(new Bundle { val Expect = Bool(); val id = PhysRegisterId() })

  when(io.MemPort.request.ready){
    //io.Retire.ready := true.B

    when(io.Dealloc.noAllocations){
      for(i <- 0 until c.memQueueSize){
        when(MemQueue(i).In.prd === io.Dealloc.oldestAllocatedId){
          io.MemPort.request.valid := true.B

          io.MemPort.request.bits.Address := MemQueue(i).In.Address
          io.MemPort.request.bits.WriteData := MemQueue(i).In.prd

          when(io.MemPort.request.ready){
            when(MemQueue(i).In.isWrite){ // Write
              io.MemPort.request.bits.isWrite := true.B

              io.EventOut.valid := true.B
              io.EventOut.bits.eventType := CompletionWithValue

            }.otherwise{ // Read
              io.MemPort.request.bits.isWrite := false.B
              ReadData.Expect := true.B // Indicates that the system should expect readdata soon
              ReadData.id := MemQueue(i).In.prd
            }

            MemQueue(i).empty := true.B
          }
        }
      }
    }    
  }

  when(ReadData.Expect){

    io.MemPort.response.ready := true.B
    //when(io.MemPort.bits.ReadData.valid && io.Write.ready){
    //when(io.MemPort.bits.ReadData.ReadData.valid && io.Write.ready){
    when(io.MemPort.response.valid){

      /*
      io.Write.bits.Address := ReadData.id
      io.Write.bits.WriteData := io.MemPortReadData.bits 
      io.Write.valid := true.B
      */

      io.EventOut.bits.pr := ReadData.id
      io.EventOut.bits.writeBackValue := io.MemPort.response.bits.readData
      io.EventOut.valid := true.B

      io.EventOut.bits.eventType := CompletionWithValue
    
      ReadData.Expect := false.B
    }
  }

  
}


object penis extends App { emitVerilog(new MemQueue()(Configuration.random()))}