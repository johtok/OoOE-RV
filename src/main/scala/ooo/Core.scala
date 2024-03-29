package ooo


import chisel3._
import ooo.Core.CoreIO
import ooo.Types.{MemPort, Word}
import ooo.modules.{Decoder, EventArbiter, Execute, IdAllocator, InstructionStreamer, IssueQueue, MemQueue, OperandFetch, ReorderBuffer, Retirement, DataMem}
import ooo.util.Program
import ooo.util._

object Core {

  def main(args: Array[String]): Unit = emitVerilog(new Core(Program.random(), Configuration.default()))

  class CoreIO()(implicit c: Configuration) extends Bundle {
    //val mem = new MemPort
  }

}

class Core(program: Program, config: Configuration) extends Module {
  implicit val c = config

  val io = IO(new CoreIO)

  val debug = if(config.simulation) Some(IO(Output(new Bundle {
    val regfile = Vec(32, Word())
    val specfile = Vec(32, Word())
    val ecall = Bool()
  }))) else None

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
    val DataMem = Module(new DataMem(0x100000, program.getBytes.map(_.litValue)))
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
    _.retirementPort <> Stage.retirement.io.decoderPort,
    _.eventBus <> Stage.eventArbiter.io.EventOut
  )

  Stage.issueQueue.io.expand(
    _.Alloc <> Stage.decoder.io.issueStream,
    _.event <> Stage.eventArbiter.io.EventOut,
    _.Dealloc <> Alloc.physRegId.io.dealloc,
    //_.StatePort <> Stage.retirement.io.stateUpdate
    _.StatePort <> Alloc.physRegId.io.state,
    _.MemQueueFull <> Stage.memQueue.io.Full
  )

  Stage.operandFetch.io.expand(
    _.In <> Stage.issueQueue.io.Issue,
    _.ROBPort <> rob.io.data,
    _.allocationInfo <> Alloc.physRegId.io.state,
    _.eventBus <> Stage.eventArbiter.io.EventOut
  )

  Stage.exe.io.expand(
    _.Instruction <> Stage.operandFetch.io.Issue,
    _.allocationInfo <> Alloc.physRegId.io.state,
    _.eventBusIn <> Stage.eventArbiter.io.EventOut
  )

  Stage.memQueue.io.expand(
    _.Package <> Stage.exe.io.MemPackage,
    _.event <> Stage.eventArbiter.io.EventOut,
    _.Dealloc <> Alloc.physRegId.io.dealloc,
    _.StatePort <> Alloc.physRegId.io.state
  )

  Stage.retirement.io.expand(
    _.eventBus <> Stage.eventArbiter.io.EventOut,
    _.robPort <> rob.io.retirementPort,
    _.allocPushBack <> Alloc.physRegId.io.pushBack,
    _.dealloc <> Alloc.physRegId.io.dealloc,
    _.snapDealloc <> Alloc.snapshotId.io.dealloc
  )

  Stage.eventArbiter.io.expand(
    _.MemIn <> Stage.memQueue.io.EventOut,
    _.ExecuteIn <> Stage.exe.io.eventBus,
  )

  //io.mem <> Stage.memQueue.io.MemPort

  Stage.DataMem.io.expand(
    _.MemPort <> Stage.memQueue.io.MemPort
  )

  rob.io.eventBus <> Stage.eventArbiter.io.EventOut

  Alloc.snapshotId.io.pushBack.expand(
    _.pushBackHead := 0.B,
    _.newHead := 0.U
  )

  if(c.simulation) {
    debug.get.regfile := Stage.decoder.debug.get.state.map { id =>
      rob.debug.get.apply(id)
    }.toVec
    debug.get.specfile := Stage.decoder.debug.get.spec.map { id =>
      rob.debug.get.apply(id)
    }.toVec
    debug.get.ecall := Stage.retirement.debug.get.exception
  }

}
