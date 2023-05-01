package ooo.modules

import chisel3._
import chisel3.internal.firrtl.Width
import ooo.Types._
import ooo.Configuration
import chisel3.util._
import ooo.util.{BundleExpander, SeqDataExtension}


class OperandFetch()(implicit c: Configuration) extends Module {
  val io = IO(new Bundle {
    val In = Flipped(Decoupled(new IssuePackage))
    val Issue = Decoupled(new ExecutePackage)
    val ROBPort = new ReadPort
  })




  val valueReg = Reg(new IssuePackage())
  val delayReg = RegInit(0.B)
  io.Issue.valid := delayReg

  io.In.ready := !delayReg || (delayReg && io.Issue.ready)

  io.Issue.bits.opcode := valueReg.opcode
  io.Issue.bits.func := valueReg.func
  io.Issue.bits.operands := io.ROBPort.ReadData 
  io.Issue.bits.prd := valueReg.prd
  io.Issue.bits.immediate := valueReg.immediate
  io.Issue.bits.pc := valueReg.pc
  io.Issue.bits.snapshotId := valueReg.snapshotId
  io.Issue.bits.branchPrediction := valueReg.branchPrediction



  //io.ROBPort.Address := io.In.bits.prs.id

  val hasToStall = !io.Issue.ready

  io.ROBPort.Address := Mux(hasToStall, valueReg.prs.map(_.id).toVec, io.In.bits.prs.map(_.id).toVec)



  when(!hasToStall){
    valueReg := io.In.bits
    delayReg := io.In.valid
  }
}

