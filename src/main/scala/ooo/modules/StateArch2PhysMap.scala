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

  val rf = RegInit(Vec(32, PhysRegisterId()), c.initialStateMap.map(_.U).toVec)

  io.read.pids := io.read.rs.map { rs => Mux(
    rs === io.write.rd && io.write.write,
    io.write.pid,
    rf(rs)
  ) }

  when(io.write.write) { rf(io.write.rd) := io.write.pid }

  io.allocationCheck.isAllocated := rf.map(_ === io.allocationCheck.newPid).toVec.reduceTree(_ || _)

}
