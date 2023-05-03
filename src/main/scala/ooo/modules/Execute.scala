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
    val eventBus = Decoupled(new Event)
  })
//TODO: find out if XLEN is given in types.scala
  val XLEN = 32; 

  //OLD Code FROM TJARK
    io.elements.foreach(_._2 := DontCare)

  //New Code:
  /*
  //Functional Units
  
    //ALU
  val ALU = Module(new ALU(XLEN)) //TODO: fix input of ALU to be more alligned with the rest of the development
  ALU.io.in <> io.Instruction //TODO: make sure that names in interfaces match  
    //BRANCH
  val BRANCH = Module(new Branch(XLEN))
  BRANCH.io.in <> io.Instruction //TODO: make sure that names in interfaces match
    //JUMP
  val JUMP = Module(new Jump(XLEN))
  JUMP.io.in <> io.Instruction //TODO: make sure that names in interfaces match
*/
}

