package ooo.modules

import chisel3._
import ooo.Configuration
import chisel3.util._
import ooo.Types.EventType._
import ooo.Types.{ArchRegisterId, Event, PhysRegisterId, ReadPort, Word, WritePort}
import ooo.modules.ReorderBuffer.{DecoderPort, RetirementPort}
import ooo.util.SeqDataExtension


object ReorderBuffer {
  class DecoderPort(implicit c: Configuration) extends Bundle {
    val prs = Input(Vec(2, PhysRegisterId()))
    val ready = Output(Vec(2, Bool()))
    val allocSetup = Input(new Bundle {
      val prd = PhysRegisterId()
      val rd = ArchRegisterId()
      val update = Bool()
    })
  }

  class RetirementPort(implicit c: Configuration) extends Bundle {
    val pr = Input(PhysRegisterId())
    val ready = Output(Bool())
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

  val dataMem = SyncReadMem(c.reorderBufferSize, Word())
  val readyMem = RegInit(Seq.fill(c.reorderBufferSize)(0.B).toVec)
  val destMem = SyncReadMem(c.reorderBufferSize, ArchRegisterId())

  io.data.ReadData := io.data.Address.map(dataMem.read(_)).toVec

  val hasWriteBack = io.eventBus.valid && io.eventBus.bits.eventType.isOneOf(CompletionWithValue, Jump)

  when(hasWriteBack) { dataMem.write(io.eventBus.bits.pr, io.eventBus.bits.writeBackValue) }

  io.decoderPort.ready := io.decoderPort.prs.map(readyMem.apply(_))

  val markAsReady = io.eventBus.valid && io.eventBus.bits.eventType.isOneOf(CompletionWithValue, Completion, Branch, Jump)

  when(io.decoderPort.allocSetup.update) {
    readyMem(io.decoderPort.allocSetup.prd) := 0.B
    destMem.write(io.decoderPort.allocSetup.prd, io.decoderPort.allocSetup.rd)
  }

  when(markAsReady) {
    readyMem(io.eventBus.bits.pr) := 1.B
  }

  io.retirementPort.rd := destMem.read(io.retirementPort.pr)
  io.retirementPort.ready := readyMem(io.retirementPort.pr)

}


object Test extends App { emitVerilog(new ReorderBuffer()(Configuration.default()))}