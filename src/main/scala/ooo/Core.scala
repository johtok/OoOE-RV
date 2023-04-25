package ooo


import chisel3._
import ooo.Core.CoreIO
import ooo.Types.MemPort
import ooo.modules.{Decoder, Execute, IdAllocator, InstructionStreamer, IssueQueue, MemQueue, OperandFetch, ReorderBuffer, Retirement, EventArbiter}
import ooo.util.Program
import ooo.util._

object Core {

  def main(args: Array[String]): Unit = emitVerilog(new Core(Program.random(), Configuration.default()))

  class CoreIO()(implicit c: Configuration) extends Bundle {
    val mem = new MemPort
  }

}

class Core(program: Program, config: Configuration) extends Module {
  implicit val c = config

  val io = IO(new CoreIO)

  val rob = Module(new ReorderBuffer)

  object Stage {
    val instrStreamer = Module(new InstructionStreamer(program))
    val decoder = Module(new Decoder)
    val issueQueue = Module(new IssueQueue)
    val operandFetch = Module(new OperandFetch)
    val exe = Module(new Execute)
    val retirement = Module(new Retirement)
    val memQueue = Module(new MemQueue)
    val eventArbiter = Module(new EventArbiter)
  }

  object Alloc {
    val physRegId = IdAllocator(c.reorderBufferSize)
    val snapshotId = IdAllocator(c.numOfSnapshots)
  }

  Stage.instrStreamer.io.expand(
    _.eventBus <> Stage.eventArbiter.io.EventOut
  )

  Stage.decoder.io.expand(
    _.instructionStream <> Stage.instrStreamer.io.instructionStream,
    _.robPort <> rob.io.decoderPort,
    _.allocationPorts.expand(
      _.physRegisterId <> Alloc.physRegId.io.alloc,
      _.snapshotId <> Alloc.snapshotId.io.alloc,
    ),
    _.stateUpdate <> Stage.retirement.io.stateUpdate,
    _.eventBus <> Stage.eventArbiter.io.EventOut
  )

  Stage.issueQueue.io.expand(
    _.Alloc <> Stage.decoder.io.issueStream,
    _.event <> Stage.eventArbiter.io.EventOut,
    _.Dealloc <> Alloc.physRegId.io.dealloc,
    //_.StatePort <> Stage.retirement.io.stateUpdate
    _.StatePort <> Alloc.physRegId.io.state
  )

  Stage.operandFetch.io.expand(
    _.In <> Stage.issueQueue.io.Issue,
    _.ROBPort <> rob.io.data
  )

  Stage.exe.io.expand(
    _.Instruction <> Stage.operandFetch.io.Issue,
  )

  Stage.retirement.io.expand(
    _.eventBus <> Stage.eventArbiter.io.EventOut,
    _.robPort <> rob.io.retirementPort,
    _.allocPushBack <> Alloc.physRegId.io.pushBack,
    _.dealloc <> Alloc.physRegId.io.dealloc
  )

  Stage.memQueue.io.expand(
    _.event <> Stage.eventArbiter.io.EventOut,
    _.Dealloc <> Alloc.physRegId.io.dealloc,
    _.StatePort <> Alloc.physRegId.io.state
  )

  Stage.eventArbiter.io.expand(
    _.MemIn <> Stage.memQueue.io.EventOut,
    _.ExecuteIn <> Stage.exe.io.eventBus,
  )

  io.mem <> Stage.memQueue.io.MemPort

}
