package ooo.modules

import chisel3._
import chisel3.util.Valid
import ooo.Configuration
import ooo.Types.{ArchRegisterId, Event, EventType, PhysRegisterId}
import ooo.util.BundleExpander


class Retirement()(implicit c: Configuration) extends Module {

  val io = IO(new Bundle {
    val dealloc = Flipped(new IdAllocator.DeallocationPort(c.physRegisterIdWidth))
    val snapDealloc = Flipped(new IdAllocator.DeallocationPort(c.snapshotIdWidth))
    val allocPushBack = Flipped(new IdAllocator.PushBackPort(c.physRegisterIdWidth))
    val snapPushBack = Flipped(new IdAllocator.PushBackPort(c.snapshotIdWidth))
    val decoderPort = Flipped(new Decoder.RetirementPort)
    val robPort = Flipped(new ReorderBuffer.RetirementPort)
    val eventBus = Flipped(Valid(new Event))
  })

  val debug = if(c.simulation) Some(IO(new Bundle {
    val exception = Output(Bool())
  })) else None

  val tail = io.dealloc.oldestAllocatedId
  val nextTail = io.dealloc.nextOldestAllocatedId

  val retire = !io.dealloc.noAllocations && (io.decoderPort.allocationCheck.isAllocated || (io.robPort.ready && !(io.eventBus.valid && io.eventBus.bits.eventType.isOneOf(EventType.Branch, EventType.Jump))))

  val branch = io.eventBus.bits.eventType === EventType.Branch
  val jump = io.eventBus.bits.eventType === EventType.Jump
  val shouldJump = io.eventBus.valid && ((branch && io.eventBus.bits.misprediction) || jump)

  io.allocPushBack.expand(
    _.newHead := io.eventBus.bits.pr + 1.U,
    _.pushBackHead := shouldJump
  )
  io.snapPushBack.expand(
    _.newHead := io.eventBus.bits.snapshotId + 1.U,
    _.pushBackHead := shouldJump
  )

  io.robPort.pr := Mux(retire, io.dealloc.nextOldestAllocatedId, io.dealloc.oldestAllocatedId)

  io.snapDealloc.release := branch

  io.dealloc.release := retire

  io.decoderPort.stateUpdate.expand(
    _.valid := retire && io.robPort.hasWriteBack,
    _.bits.pr := tail,
    _.bits.rd := io.robPort.rd
  )
  io.decoderPort.allocationCheck.pr := tail

  if(c.simulation) {
    debug.get.exception := retire && io.robPort.hadException
  }



}
