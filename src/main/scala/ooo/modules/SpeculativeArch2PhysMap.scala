package ooo.modules

import chisel3._
import ooo.Configuration
import ooo.Types.{ArchRegisterId, PhysRegisterId, SnapshotId}


object SpeculativeArch2PhysMap {
  def apply()(implicit c: Configuration): SpeculativeArch2PhysMap = Module(new SpeculativeArch2PhysMap)
}
class SpeculativeArch2PhysMap(implicit c: Configuration) extends Module {

  val io = IO(new Bundle {
    val read = new Arch2PhysMap.ReadPort
    val write = new Arch2PhysMap.WritePort

    val save = Input(new Bundle {
      val takeSnapshot = Bool()
      val snapshotId = SnapshotId()
    })
    val restore = Input(new Bundle {
      val restoreSnapshot = Bool()
      val snapshotId = SnapshotId()
    })
  })


  val rf = Reg(Vec(32, PhysRegisterId()))
  val snapshots = Reg(Vec(c.numOfSnapshots, Vec(32, PhysRegisterId())))

  io.read.prs := io.read.rs.map { rs => Mux(
    rs === io.write.rd && io.write.write,
    io.write.prd,
    rf(rs))
  }

  when(io.write.write) { rf(io.write.rd) := io.write.prd }

  when(io.save.takeSnapshot) { snapshots(io.save.snapshotId) := rf }
  when(io.restore.restoreSnapshot) { rf := snapshots(io.restore.snapshotId) }

}