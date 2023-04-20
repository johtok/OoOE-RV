package ooo.modules

import chisel3._
import chisel3.internal.firrtl.Width
//import chisel3.util.{Fill, log2Ceil, MixedVec}
import ooo.Types._
import ooo.Types.EventType._

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
    val Write = Decoupled(new WritePort)
    val MemPort = Decoupled(new MemPort)
    val MemPortReadData = Flipped(Valid(Word())) // I tried making this a part of the MemPort but it didnt work
    val Retire = Flipped(Decoupled(PhysRegisterId()))

    val event = Flipped(Valid(new Event))
  })

  io.MemPort.valid := false.B
  io.MemPort.bits.WriteEn := false.B

  io.Retire.ready := io.MemPort.ready

  io.Write.bits.Address := 0.U
  io.Write.bits.WriteData := 0.U
  io.Write.valid := false.B

  io.MemPort.bits.Address := 0.U
  io.MemPort.bits.WriteData := 0.U
  io.MemPort.bits.WriteEn := false.B



  val MemQueue = Reg(Vec(c.memQueueSize, new QueueElementPort()))

  io.Package.ready := VecInit(Seq.tabulate(c.memQueueSize)(n => MemQueue(n).empty)).reduceTree(_ | _)


  val Kill = Wire(Bool())
  Kill := io.event.valid && io.event.bits.eventType === Branch


  val WriteCarry = Wire(Vec(c.memQueueSize + 1, Bool()))
  WriteCarry(0) := false.B

  for(i <- 0 until c.memQueueSize + 1){
    WriteCarry(i) := false.B
  }

  // Add kill logic. 

  when(io.Package.valid && !(Kill && io.Package.bits.prd > io.event.bits.pr)){
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
      when(MemQueue(i).In.prd > io.event.bits.pr){
        MemQueue(i).empty := true.B
      }
    }
  }





  //Transmit data

  //val ExpectData = RegInit(0.B)

  val ReadData = Reg(new Bundle { val Expect = Bool(); val id = PhysRegisterId() })

  when(io.MemPort.ready){
    io.Retire.ready := true.B

    when(io.Retire.valid){
      for(i <- 0 until c.memQueueSize){
        when(MemQueue(i).In.prd === io.Retire.bits){
          io.MemPort.valid := true.B

          io.MemPort.bits.Address := MemQueue(i).In.Address
          io.MemPort.bits.WriteData := MemQueue(i).In.prd

          when(MemQueue(i).In.func){ // Write
            io.MemPort.bits.WriteEn := true.B
          }.otherwise{ // Read
            io.MemPort.bits.WriteEn := false.B
            ReadData.Expect := true.B // Indicates that the system should expect readdata soon
            ReadData.id := MemQueue(i).In.prd
          }

          MemQueue(i).empty := true.B

        }
      }
    }    
  }

  when(ReadData.Expect){
    //when(io.MemPort.bits.ReadData.valid && io.Write.ready){
    //when(io.MemPort.bits.ReadData.ReadData.valid && io.Write.ready){
    when(io.MemPortReadData.valid && io.Write.ready){

      io.Write.bits.Address := ReadData.id
      io.Write.bits.WriteData := io.MemPortReadData.bits 
      io.Write.valid := true.B

      ReadData.Expect := false.B
    }
  }

  
}


object penis extends App { emitVerilog(new MemQueue()(Configuration.random()))}