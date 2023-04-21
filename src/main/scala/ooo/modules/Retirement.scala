package ooo.modules

import chisel3._
import chisel3.util.Valid
import ooo.Configuration
import ooo.Types.{ArchRegisterId, Event, EventType, PhysRegisterId}
import ooo.modules.Retirement.StateUpdate
import ooo.util.BundleExpander

object Retirement {
  class StateUpdate(implicit c: Configuration) extends Bundle {
    val pr = PhysRegisterId()
    val rd = ArchRegisterId()
  }
}

class Retirement()(implicit c: Configuration) extends Module {

  val io = IO(new Bundle {
    val dealloc = Flipped(new IdAllocator.DeallocationPort(c.physRegisterIdWidth))
    val allocPushBack = Flipped(new IdAllocator.PushBackPort(c.physRegisterIdWidth))
    val stateUpdate = Valid(new StateUpdate)
    val robPort = Flipped(new ReorderBuffer.RetirementPort)
    val eventBus = Flipped(Valid(new Event))
  })

  val tail = io.dealloc.oldestAllocatedId
  val nextTail = io.dealloc.nextOldestAllocatedId

  io.robPort.pr := nextTail

  val retire = !io.dealloc.noAllocations && io.robPort.ready && !io.eventBus.valid

  val misprediction = io.eventBus.valid && io.eventBus.bits.eventType.isOneOf(EventType.Branch, EventType.Jump)

  io.allocPushBack.expand(
    _.newHead := io.eventBus.bits.pr,
    _.pushBackHead := misprediction
  )

  io.retirement.expand(
    _.valid := retire,
    _.bits.pr := tail,
    _.bits.rd := io.robPort.rd
  )


}
