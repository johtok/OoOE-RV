package ooo.modules

import chisel3._
import chisel3.internal.firrtl.Width
//import chisel3.util.{Fill, log2Ceil, MixedVec}
import chisel3.util._
import chisel3.util.Decoupled
import ooo.modules.IssueQueue.{InstPort, IssuePort, ElementPort}
import ooo.util.BundleExpander

object IssueQueue {

  class InstPort(tagWidth: Width) extends Bundle {
    val rdTag = Input(UInt(tagWidth))
    val rs1Tag = Input(UInt(tagWidth))
    val rs2Tag = Input(UInt(tagWidth))
    val OP = Input(UInt(6.W))
    val Offset_PC = Input(UInt(32.W))
  }
  class IssuePort(tagWidth: Width) extends Bundle {
    val rdTag = Output(UInt(tagWidth))
    val rs1 = Output(UInt(32.W))
    val rs2 = Output(UInt(32.W))
    val OP = Output(UInt(5.W))
    val Offset_PC = Output(UInt(32.W))
  }
  class ElementPort(tagWidth: Width) extends Bundle{
    val In = Decoupled(new InstPort(tagWidth))
    val Issue = Flipped(Decoupled(new InstPort(tagWidth)))
    val IssueReady = Output(Bool())
    val Age = Output(UInt(5.W))
    val EventTag = Input(UInt(tagWidth))
    val reset = Input(Bool())
  }

  def apply(tagCount: Int): IdAllocator = Module(new IdAllocator(tagCount))

}
class IssueQueue(tagCount: Int, positions: Int) extends Module {

  val w = log2Ceil(tagCount - 1).W
  def Id() = UInt(w)

  val PosIndex = log2Ceil(positions - 1).W

  val io = IO(new Bundle {
    val Alloc = Decoupled(new InstPort(w))
    val Issue = Decoupled(new IssuePort(w))
    val EventTag = Input(UInt(w))
  })

  // Instansiate issue queue

  val QueueVec = Wire(Vec(positions, new ElementPort(w)))

  for(i <- 0 until positions){
    val Element = Module(new IssueElement(w))
    Element.io <> QueueVec(i)
  }

  // Check wether queue is full 

  val Carry = Wire(Vec(positions + 1, Bool()))
  Carry(0) := false.B

  val Full = Wire(Bool())
  
  for(i <- 0 until positions){ // Checks if Queue is full 
    Carry(i+1) := Mux(Carry(i),true.B,QueueVec(i).In.ready)
  }

  //Full := !Carry(positions+1) 

  io.Alloc.ready := Carry(positions+1)

  when(io.Alloc.valid){
    for(i <- 0 until positions){
      when(QueueVec(i).In.ready){
        QueueVec(i).In <> io.Alloc
      } 
    }
  }

  //Write instruction to queue

  val WriteCarry = Wire(Vec(positions + 1, Bool()))
  WriteCarry(0) := false.B

  when(io.Alloc.valid){
    for(i <- 0 until positions){
      when(!WriteCarry(i)){
        QueueVec(i).In <> io.Alloc
        QueueVec(i).In.valid := true.B
      }
    }
  } 

  // Issue instruction, Iterates from bottom of issue queue.

  /*

  val IssueCarry = Wire(Vec(positions + 1, Bool()))
  IssueCarry(positions) := false.B

  for(i <- 0 until positions){
    IssueCarry(i+1) := Mux(IssueCarry(i),true.B,QueueVec(i).IssueReady) 
  }

  */

  val AgeVec = Wire(Vec(positions,UInt(5.W)))

  for(i <- 0 until positions){
    AgeVec(i) := QueueVec(i).Age
  }

  val scalaVector = AgeVec.zipWithIndex
  .map ((x) => MixedVecInit (x._1 , x._2.U(8.W)))
  val resFun2 = VecInit ( scalaVector )
  . reduceTree ((x, y) => Mux(x(0) > y(0) , x, y))
  val maxVal = resFun2 (0)
  val maxIdx = resFun2 (1)

  when(io.Issue.ready && (maxVal > 0.U)){
    io.Issue <> QueueVec(maxIdx).Issue
  }
}



class IssueElement(tagWidth: Width) extends Module{
  val io = IO(new Bundle {
    val Port = new ElementPort(tagWidth)
  })

  io.Port.IssueReady := false.B

  val valueReg = RegInit(0.U.asTypeOf(new InstPort(tagWidth)))

  val emptyReg = RegInit(0.B)
  val AgeReg = RegInit(0.U(5.W))

  val rs1_ready = RegInit(0.B)
  val rs2_ready = RegInit(0.B)

  when(io.Port.EventTag === valueReg.rs1Tag){
    rs1_ready := true.B
  }

  when(io.Port.EventTag === valueReg.rs1Tag){
    rs2_ready := true.B
  }

  when(rs1_ready && rs2_ready){
    io.Port.IssueReady := true.B

    when(io.Port.Issue.ready){
       io.Port.Issue.valid := true.B
       io.Port.Issue.bits := valueReg
       emptyReg := true.B
       AgeReg := 0.U
    } 
  }

  when(io.Port.In.valid){
    valueReg := io.Port.In
    emptyReg := false.B
    AgeReg := 1.U
  }

  when(!emptyReg){
    AgeReg := AgeReg + 1.U
  }

  io.Port.Age := AgeReg


}
