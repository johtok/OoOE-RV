package ooo


import chisel3._
import ooo.Core.CoreIO
import ooo.Types.MemPort
import ooo.modules.{Decoder, Execute, IdAllocator, InstructionStreamer, IssueQueue, ReorderBuffer}
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
    val exe = Module(new Execute)
  }

  object Alloc {
    val physRegId = IdAllocator(c.reorderBufferSize)
    val branchId = IdAllocator(c.numOfSnapshots)
  }

  Stage.instrStreamer.io.expand(
    _.eventBus <> Stage.exe.io.eventBus
  )
/*
  Stage.decoder.io.expand(
    _.instructionStream <> Stage.instrStreamer.io.instructionStream,
    _.instructionStatus.map(_.ready).zip(rob.io.ready.read.take(2).map(_.isReady)).bulkConnectPairs(),
    _.allocationPorts.expand(
      _.physRegisterId <> Alloc.physRegId.io.alloc,
      _.branchId <> Alloc.branchId.io.alloc,
      _.loadId <> Alloc.loadId.io.alloc,
      _.storeId <> Alloc.storeId.io.alloc
    ),
    _.retirement, // TODO
    _.eventBus <> Stage.exe.io.eventBus
  )



 */
}
