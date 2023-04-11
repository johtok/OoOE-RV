package ooo.modules

import chisel3._
import chisel3.internal.firrtl.Width
//import chisel3.util.{Fill, log2Ceil, MixedVec}
import ooo.Types._
import ooo.Configuration
//import chisel3.util.{Decoupled, MuxCase, Valid}
import chisel3.util._
import ooo.modules.IssueQueue.{ ElementPort}
import ooo.util.BundleExpander

object IssueQueue {
  class ElementPort(implicit c: Configuration) extends Bundle{
    val In = Flipped(Decoupled(new IssuePackage))
    val Issue = Decoupled(new IssuePackage)
    val IssueReady = Output(Bool())
    val Age = Output(UInt(5.W))
    val eventBus = Flipped(Valid(new Event))
  }

  def apply(tagCount: Int): IdAllocator = Module(new IdAllocator(tagCount))

}
class IssueQueue()(implicit c: Configuration) extends Module {
  val positions = c.issueQueueSize
  val w = c.physRegisterIdWidth

  val PosIndex = log2Ceil(positions - 1).W

  val io = IO(new Bundle {
    val Alloc = Flipped(Decoupled(new IssuePackage))
    val Issue = Decoupled(new IssuePackage)

    val eventBus = Flipped(Valid(new Event))
  })


  io.Issue.valid := false.B
  io.Alloc.ready := false.B

  // Instansiate issue queue

  val QueueVec = Wire(Vec(positions, new ElementPort))

  for(i <- 0 until positions){
    val Element = Module(new IssueElement())

    QueueVec(i) <> Element.io.Port

    QueueVec(i).eventBus <> io.eventBus

    QueueVec(i).In.bits <> io.Alloc.bits
    QueueVec(i).Issue.bits <> io.Issue.bits

    QueueVec(i).In.valid := false.B
    QueueVec(i).Issue.ready := false.B
  }

  // Checks if any positions are available 

  io.Alloc.ready := VecInit(Seq.tabulate(positions)(n => QueueVec(n).In.ready)).reduceTree(_ | _)

  //Write instruction to queue

  val WriteCarry = Wire(Vec(positions + 1, Bool()))
  WriteCarry(0) := false.B

  for(i <- 0 until positions + 1){
    WriteCarry(i) := false.B
  }

  when(io.Alloc.valid){
    for(i <- 0 until positions){
      when(!WriteCarry(i)){
        when(QueueVec(i).In.ready){
          //QueueVec(i).In.bits := io.Alloc.bits 
          QueueVec(i).In.valid := true.B
          WriteCarry(i+1) := true.B
        }.otherwise{
          WriteCarry(i+1) := false.B
        }
      }.otherwise{
        WriteCarry(i+1) := true.B
      }
    }
  } 

  

  val AgeVec = VecInit(Seq.tabulate(positions)(n => QueueVec(n).Age))

  val scalaVector = AgeVec.zipWithIndex
  .map ((x) => MixedVecInit (x._1 , x._2.U(8.W)))
  val resFun2 = VecInit ( scalaVector )
  . reduceTree ((x, y) => Mux(x(0) >= y(0) , x, y))
  val maxVal = resFun2 (0)
  val maxIdx = resFun2 (1)

  io.Issue.bits <> QueueVec(maxIdx).Issue.bits

  when(io.Issue.ready && (maxVal > 0.U)){
    QueueVec(maxIdx).Issue.ready := true.B
    io.Issue.valid := true.B
  }

}

class IssueElement()(implicit c: Configuration) extends Module{
  val io = IO(new Bundle {
    val Port = new ElementPort()
  })

  io.Port.Issue.valid := false.B
  io.Port.In.ready := false.B

  io.Port.IssueReady := false.B
  io.Port.Age := 0.U


  val valueReg = Reg(new IssuePackage())

  val emptyReg = RegInit(1.B)
  val AgeReg = RegInit(0.U(8.W))

  when(io.Port.eventBus.valid && !emptyReg){
    when(io.Port.eventBus.bits.pr === valueReg.prs(0).id){
      valueReg.prs(0).ready := true.B
    }

    when(io.Port.eventBus.bits.pr === valueReg.prs(1).id){
      valueReg.prs(1).ready := true.B
    }
  }


  when(valueReg.prs(0).ready && valueReg.prs(1).ready){
    io.Port.IssueReady := true.B
    io.Port.Age := AgeReg

    when(io.Port.Issue.ready){
      io.Port.Issue.valid := true.B
      valueReg.prs(0).ready := false.B
      valueReg.prs(1).ready := false.B

      emptyReg := true.B
      AgeReg := 0.U
    } 
  }

  when(!emptyReg){
    AgeReg := AgeReg + 1.U
  }.otherwise{
    io.Port.In.ready := true.B

    when(io.Port.In.valid){
      valueReg := io.Port.In.bits
      emptyReg := false.B
      AgeReg := 1.U
    }
  }

  io.Port.Issue.bits := valueReg

}

