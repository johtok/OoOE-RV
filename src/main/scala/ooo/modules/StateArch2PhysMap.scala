package ooo.modules

import chisel3._
import ooo.Configuration
import ooo.Types.{ArchRegisterId, PhysRegisterId}
import ooo.util.SeqDataExtension

object StateArch2PhysMap {
  def apply()(implicit c: Configuration): StateArch2PhysMap = Module(new StateArch2PhysMap)
}
class StateArch2PhysMap(implicit c: Configuration) extends Module {

  val io = IO(new Bundle {
    val read = new Arch2PhysMap.ReadPort
    val write = new Arch2PhysMap.WritePort

    val allocationCheck = new Bundle {
      val newPid = Input(PhysRegisterId())
      val isAllocated = Output(Bool())
    }
  })

  val debug = if(c.simulation) Some(IO(Output(Vec(32, PhysRegisterId())))) else None

  val rf = RegInit(Vec(32, PhysRegisterId()), c.initialStateMap.map(_.U).toVec)

  if(c.simulation) debug.get := rf

  io.read.prs := io.read.rs.map(rf(_))

  when(io.write.write) { rf(io.write.rd) := io.write.prd }

  io.allocationCheck.isAllocated := rf.map(_ === io.allocationCheck.newPid).toVec.reduceTree(_ || _)

}
