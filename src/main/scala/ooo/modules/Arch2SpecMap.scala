package ooo.modules

import chisel3._
import ooo.Configuration
import ooo.Types.{ArchRegisterId, BranchId, PhysRegisterId}
class Arch2SpecMap(implicit c: Configuration) extends Module {

  val io = IO(new Bundle {
    val rs = Input(Vec(2, ArchRegisterId()))
    val pids = Output(Vec(2, PhysRegisterId()))

    val update = Input(new Bundle {
      val write = Bool()
      val pid = PhysRegisterId()
      val rd = ArchRegisterId()
    })

    val save = Input(new Bundle {
      val takeSnapshot = Bool()
      val branchId = BranchId()
    })
    val restore = Input(new Bundle {
      val restoreSnapshot = Bool()
      val branchId = BranchId()
    })
  })


  val rf = Reg(Vec(32, PhysRegisterId()))

  val snapshots = Reg(Vec(c.numOfSnapshotBuffers, Vec(32, PhysRegisterId())))

  io.pids := io.rs.map { rs => Mux(rs === io.update.rd && io.update.write, io.update.pid, rf(rs)) }

  val rfUpdate = WireDefault(rf)
  rfUpdate(io.update.rd) := io.update.pid

  when(io.update.write) { rf := rfUpdate }

  when(io.save.takeSnapshot) { snapshots(io.save.branchId) := Mux(io.update.write, rfUpdate, rf) }
  when(io.restore.restoreSnapshot) { rf := snapshots(io.restore.branchId) }

}