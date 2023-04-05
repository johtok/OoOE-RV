package ooo.modules

import chisel3._
import chisel3.experimental.{ChiselAnnotation, annotate}
import chisel3.util.{Decoupled, MuxCase, Valid}
import firrtl.annotations.MemoryArrayInitAnnotation
import ooo.Configuration
import ooo.Types.Immediate.InstructionFieldExtractor
import ooo.Types.{BranchPrediction, Event, EventType, InstructionPackage, Opcode, Word}
import ooo.util.{BundleExpander, Program, SIntExtension, UIntSeqExtension}

import scala.util.Random



class InstructionStreamer(program: Program)(implicit c: Configuration) extends Module {

  val io = IO(new Bundle {
    val instructionStream = Decoupled(new InstructionPackage)
    val eventBus = Flipped(Valid(new Event))
  })

  val pc = RegInit(Word(), 0.U)
  val nextPc = Wire(Word())
  pc := nextPc
  val rom = VecInit(pc)

  val instruction = rom(pc)

  val opcode = instruction.opcode
  val isBranch = opcode === Opcode.branch
  val isJal = opcode === Opcode.jal
  val imm = Mux(isBranch, instruction.immediate.bType, instruction.immediate.jType)

  // predict backward branches as taken
  val prediction = Mux(imm.sign, BranchPrediction.Taken, BranchPrediction.NotTaken)

  nextPc := MuxCase(pc + 4.U, Seq(
    io.eventBus.valid -> io.eventBus.bits.pc,
    !io.instructionStream.ready -> pc,
    (isJal || (isBranch && prediction === BranchPrediction.Taken)) -> (pc.asSInt + imm).asUInt
  ))

  io.instructionStream.valid := 1.B
  io.instructionStream.bits := Reg(new InstructionPackage).expand(
    _.instruction := instruction,
    _.pc := pc,
    _.branchPrediction := prediction
  )


}

object InstructionStreamer extends App { emitVerilog(new InstructionStreamer(Program("empty",Seq.fill(8*1024)(Random.nextInt(255))))(Configuration.random()))}