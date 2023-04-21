package ooo.modules

import chisel3._
import chisel3.internal.firrtl.Width
import chisel3.util.{MuxCase, log2Ceil}
import ooo.modules.IdAllocator.{AllocationPort, AllocatorStatePort, DeallocationPort, PushBackPort}
import ooo.util.BundleExpander

object IdAllocator {

  class AllocationPort(idWidth: Width) extends Bundle {
    val id = Output(UInt(idWidth))
    val offer = Output(Bool())
    val take = Input(Bool())
  }
  class DeallocationPort(idWidth: Width) extends Bundle {
    val oldestAllocatedId = Output(UInt(idWidth))
    val nextOldestAllocatedId = Output(UInt(idWidth))
    val noAllocations = Output(Bool())
    val release = Input(Bool())
  }
  class PushBackPort(idWidth: Width) extends Bundle {
    val newHead = Input(UInt(idWidth))
    val pushBackHead = Input(Bool())
  }
  class AllocatorStatePort(idWidth: Width) extends Bundle {
    val oldest = Output(UInt(idWidth))
    val youngest = Output(UInt(idWidth))
    val wrapped = Output(Bool())
  }

  def shouldBeKilled(id: UInt, cut: UInt, oldest: UInt, youngest: UInt, wrapped: Bool): Bool = {
    // TODO: verify
    Mux(!wrapped && cut >= oldest,
      id > cut || id < youngest,
      id > cut && id <= youngest
    )
  }

  def apply(idCount: Int): IdAllocator = Module(new IdAllocator(idCount))

}
class IdAllocator(idCount: Int) extends Module {

  val w = log2Ceil(idCount - 1).W
  def Id() = UInt(w)

  val io = IO(new Bundle {
    val alloc = new AllocationPort(w)
    val dealloc = new DeallocationPort(w)
    val pushBack = new PushBackPort(w)
    val state = new AllocatorStatePort(w)
  })

  val head = RegInit(Id(), 0.U)
  val tail = RegInit(Id(), 0.U)
  val wrapBitHead = RegInit(0.B)
  val wrapBitTail = RegInit(0.B)

  val collision = head === tail
  val full = collision && wrapBitHead =/= wrapBitTail
  val empty = collision && wrapBitHead === wrapBitTail

  val wrapHead = head === (idCount - 1).U
  val wrapTail = tail === (idCount - 1).U
  val nextHead = Mux(wrapHead, 0.U, head + 1.U)
  val nextTail = Mux(wrapTail, 0.U, tail + 1.U)

  when(io.alloc.take && !full) {
    head := nextHead
    when(wrapHead) { wrapBitHead := !wrapBitHead }
  }
  when(io.dealloc.release && !empty) {
    tail := nextTail
    when(wrapTail) { wrapBitTail := !wrapBitTail }
  }

  when(io.pushBack.pushBackHead) {
    head := io.pushBack.newHead
    when(io.pushBack.newHead >= tail) {
      wrapHead := wrapTail // TODO: verify
    }
  }

  io.alloc.expand(
    _.id := head,
    _.offer := !full && !io.pushBack.pushBackHead
  )
  io.dealloc.expand(
    _.noAllocations := empty || io.pushBack.pushBackHead,
    _.oldestAllocatedId := tail,
    _.nextOldestAllocatedId := nextTail
  )

}
