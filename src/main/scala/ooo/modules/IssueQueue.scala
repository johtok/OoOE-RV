package ooo.modules

import chisel3._
import chisel3.internal.firrtl.Width
import chisel3.util.{Fill, log2Ceil}
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
    val Ready = Output(Bool())
    val EventTag = Input(UInt(tagWidth))
    val empty = Output(Bool()) 
    val reset = Input(Bool())
  }

  def apply(tagCount: Int): IdAllocator = Module(new IdAllocator(tagCount))

}
class IssueQueue(tagCount: Int, positions: Int) extends Module {

  val w = log2Ceil(tagCount - 1).W
  def Id() = UInt(w)

  val PosIndex = log2Ceil(positions - 1).W

  val io = IO(new Bundle {
    val Alloc = new InstPort(w)
    val Issue = new IssuePort(w)
    val EventTag = Input(UInt(w))
  })

  val QueueVec = Wire(Vec(positions, new ElementPort(w)))

  for(i <- 0 until positions){
    val Element = Module(new IssueElement(w))
    Element.io <> QueueVec(i)
  }

}



class IssueElement(tagWidth: Width) extends Module{
  val io = IO(new Bundle {
    val Port = new ElementPort(tagWidth)
  })

  io.Port.Ready := false.B

  val valueReg = RegInit(0.U.asTypeOf(new InstPort(tagWidth)))

  val emptyReg = RegInit(0.B)

  val rs1_ready = RegInit(0.B)
  val rs2_ready = RegInit(0.B)

  when(io.Port.EventTag === valueReg.rs1Tag){
    rs1_ready := true.B
  }

  when(io.Port.EventTag === valueReg.rs1Tag){
    rs2_ready := true.B
  }

  when(rs1_ready && rs2_ready){
    io.Port.Ready := true.B

    when(io.Port.Issue.ready){
       io.Port.Issue.valid := true.B
       io.Port.Issue.bits := valueReg
    } 
  }

  when(io.Port.In.valid){
    valueReg := io.Port.In
  }


}
