package ooo.modules

import chisel3._
import ooo.Configuration
import chisel3.util._
import ooo.Types.EventType._
import ooo.Types.{ArchRegisterId, Event, EventType, PhysRegisterId, ReadPort, Word, WritePort}
import ooo.modules.ReorderBuffer.{DecoderPort, RetirementPort}
import ooo.util.SeqDataExtension


object ReorderBuffer {
  class DecoderPort(implicit c: Configuration) extends Bundle {
    val prs = Input(Vec(2, PhysRegisterId()))
    val ready = Output(Vec(2, Bool()))
    val allocSetup = Input(new Bundle {
      val prd = PhysRegisterId()
      val rd = ArchRegisterId()
      val hasWriteBack = Bool()
      val update = Bool()
    })
  }

  class RetirementPort(implicit c: Configuration) extends Bundle {
    val pr = Input(PhysRegisterId())
    val ready = Output(Bool())
    val hadException = Output(Bool())
    val hasWriteBack = Output(Bool())
    val rd = Output(ArchRegisterId())
  }
}

class ReorderBuffer(implicit c: Configuration) extends Module {

  val io = IO(new Bundle {

    val data = Flipped(new ReadPort)

    val eventBus = Flipped(Valid(new Event))

    val decoderPort = new DecoderPort

    val retirementPort = new RetirementPort

  })

  val debug = if(c.simulation) Some(IO(Output(Vec(c.reorderBufferSize, Word())))) else None

  val dataMem = SyncReadMem(c.reorderBufferSize, Word())
  val readyMem = RegInit(Seq.fill(c.reorderBufferSize)(1.B).toVec)
  val exceptions = RegInit(Seq.fill(c.reorderBufferSize)(0.B).toVec)
  val withWriteBack = RegInit(Seq.fill(c.reorderBufferSize)(0.B).toVec)
  val destMem = SyncReadMem(c.reorderBufferSize, ArchRegisterId())

  val dataShadow = if(c.simulation) Some(RegInit(Seq.fill(c.reorderBufferSize)(0.U(32.W)).toVec)) else None

  io.data.ReadData := io.data.Address.map(dataMem.read(_)).toVec

  val hasWriteBack = io.eventBus.valid && io.eventBus.bits.eventType.isOneOf(CompletionWithValue, Jump)

  when(hasWriteBack) {
    dataMem.write(io.eventBus.bits.pr, io.eventBus.bits.writeBackValue)
  }

  when(io.eventBus.valid && io.eventBus.bits.eventType === EventType.Exception) {
    exceptions(io.eventBus.bits.pr) := 1.B
  }

  if(c.simulation) {
    when(hasWriteBack) { dataShadow.get.apply(io.eventBus.bits.pr) := io.eventBus.bits.writeBackValue }
    debug.get := dataShadow.get
  }


  io.decoderPort.ready := io.decoderPort.prs.map { pr =>
    val forwardNow = RegNext(pr) === io.eventBus.bits.pr && io.eventBus.valid
    val forwardDuringRead = RegNext(pr === io.eventBus.bits.pr && io.eventBus.valid, 0.B)
    Mux(forwardDuringRead || forwardNow, 1.B, RegNext(readyMem(pr)))
  }

  val markAsReady = io.eventBus.valid

  when(markAsReady) {
    readyMem(io.eventBus.bits.pr) := 1.B
  }

  when(io.decoderPort.allocSetup.update) {
    readyMem(io.decoderPort.allocSetup.prd) := 0.B
    withWriteBack(io.decoderPort.allocSetup.prd) := io.decoderPort.allocSetup.hasWriteBack
    destMem.write(io.decoderPort.allocSetup.prd, io.decoderPort.allocSetup.rd)
  }



  io.retirementPort.rd := destMem.read(io.retirementPort.pr)
  io.retirementPort.ready := Mux(RegNext(io.retirementPort.pr === io.decoderPort.allocSetup.prd && io.decoderPort.allocSetup.update, 0.B), 0.B, RegNext(readyMem(io.retirementPort.pr)))
  io.retirementPort.hadException := RegNext(exceptions(io.retirementPort.pr))
  io.retirementPort.hasWriteBack := RegNext(withWriteBack(io.retirementPort.pr))

}
