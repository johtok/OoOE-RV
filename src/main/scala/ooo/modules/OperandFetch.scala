package ooo.modules

import chisel3._
import chisel3.internal.firrtl.Width
//import chisel3.util.{Fill, log2Ceil, MixedVec}
import ooo.Types._
import ooo.Configuration
//import chisel3.util.{Decoupled, MuxCase, Valid}
import chisel3.util._
import ooo.util.BundleExpander


class OperandFetch()(implicit c: Configuration) extends Module {
  val io = IO(new Bundle {
    val In = Flipped(Decoupled(new IssuePackage))
    val Issue = Decoupled(new ExecutePackage)
    val ROBPort = new ReadPort
  })

  io.In.ready := io.Issue.ready
  io.Issue.valid := false.B

  val valueReg = Reg(new IssuePackage())
  val delayReg = RegInit(0.B)

  io.Issue.bits.microOp := valueReg.microOp
  io.Issue.bits.func := valueReg.func
  io.Issue.bits.operands := io.ROBPort.ReadData 
  io.Issue.bits.prd := valueReg.prd
  io.Issue.bits.immediate := valueReg.immediate
  io.Issue.bits.pc := valueReg.pc
  io.Issue.bits.branchId := valueReg.branchId
  io.Issue.bits.branchPrediction := valueReg.branchPrediction
  io.Issue.bits.loadId := valueReg.loadId
  io.Issue.bits.storeId := valueReg.storeId

  io.ROBPort.Address := io.In.bits.prs

  when(io.In.valid){
    valueReg := io.In.bits
    delayReg := true.B
  }

  when(delayReg && io.Issue.ready){
    //io.Issue.bits <> valueReg

    io.Issue.valid := true.B
  }
}

