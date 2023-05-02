package ooo.modules

import chisel3._
import chisel3.experimental.{ChiselAnnotation, annotate}
import chisel3.util.{Decoupled, MuxCase, Valid}
import firrtl.annotations.MemoryArrayInitAnnotation
import ooo.Configuration
import ooo.Types.EventType._
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
  val rom = VecInit(program.getWords)
  val programEnd = pc(31,2) === program.getWords.length.U

  val instruction = rom(pc(31,2))

  val opcode = instruction.opcode
  val isBranch = opcode === Opcode.branch
  val isJal = opcode === Opcode.jal
  val imm = Mux(isBranch, instruction.immediate.bType, instruction.immediate.jType)

  // predict backward branches as taken
  val prediction = Mux(imm.sign, BranchPrediction.Taken, BranchPrediction.NotTaken)

  val pcChange = io.eventBus.valid && io.eventBus.bits.eventType.isOneOf(Branch, Jump)

  val validReg = RegInit(0.B)

  nextPc := MuxCase(pc + 4.U, Seq(
    programEnd -> pc,
    pcChange -> io.eventBus.bits.pc,
    (!io.instructionStream.ready && validReg) -> pc,
    (isJal || (isBranch && prediction === BranchPrediction.Taken)) -> (pc.asSInt + imm).asUInt
  ))

  io.instructionStream.valid := validReg
  val outReg = Reg(new InstructionPackage)
  io.instructionStream.bits := outReg

  when(io.instructionStream.ready || !validReg) {
    validReg := !programEnd
    outReg.expand(
      _.instruction := instruction,
      _.pc := pc,
      _.branchPrediction := prediction
    )
  }


}

object InstructionStreamer extends App { emitVerilog(new InstructionStreamer(Program("empty",Seq.fill(8*1024)(Random.nextInt(255))))(Configuration.random()))}