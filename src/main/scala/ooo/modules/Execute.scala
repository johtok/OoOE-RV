package ooo.modules

import chisel3._
import chisel3.internal.firrtl.Width
import ooo.Types._
import ooo.Configuration
import chisel3.util._
import ooo.util.BundleExpander

class Execute()(implicit c: Configuration) extends Module {
  val io = IO(new Bundle {
    val Instruction = Flipped(Decoupled(new ExecutePackage))
    val eventBus = Valid(new Event)
    val Writeback = Decoupled(new WritePort)

  })


}

