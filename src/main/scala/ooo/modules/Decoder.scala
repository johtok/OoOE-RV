package ooo.modules

import chisel3._
import chisel3.util.{Decoupled, Valid}
import ooo.Configuration
import ooo.Types.{Event, InstructionPackage, IssuePackage}


class Decoder()(implicit c: Configuration) extends Module {

  val io = IO(new Bundle {
    val instructionStream = Flipped(Decoupled(new InstructionPackage))
    val issueStream = Decoupled(new IssuePackage)
    val allocationPorts = new Bundle {
      val physRegisterId = Flipped(new IdAllocator.AllocationPort(c.physRegisterIdWidth))
      val branchId = Flipped(new IdAllocator.AllocationPort(c.branchIdWidth))
      val loadId = Flipped(new IdAllocator.AllocationPort(c.loadIdWidth))
      val storeId = Flipped(new IdAllocator.AllocationPort(c.storeIdWidth))
    }
    val eventBus = Flipped(Valid(new Event))
  })

  io.elements.foreach(_._2 := DontCare)

  val specArch2Phys = Module(new SpeculativeArch2PhysMap)
  val stateArch2Phys = Module(new StateArch2PhysMap)



}
