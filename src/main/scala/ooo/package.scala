import chisel3._
import chisel3.util.log2Ceil

import scala.util.Random
import ooo.util._

package object ooo {

  case class Configuration(
                            reorderBufferSize: Int,
                            issueQueueSize: Int,
                            memQueueSize: Int,
                            numOfSnapshots: Int,
                            initialStateMap: Seq[Int],
                            simulation: Boolean = false
                          ) {
    val physRegisterIdWidth = log2Ceil(reorderBufferSize).W
    val memIdWidth = log2Ceil(memQueueSize).W
    val snapshotIdWidth = log2Ceil(numOfSnapshots).W
  }

  object Configuration {
    def random(): Configuration = {
      val robSize = Random.nextPow2(32 to 1024)
      ooo.Configuration(
        robSize,
        Random.nextPow2(16 to 64),
        Random.nextPow2(4 to 32),
        Random.nextPow2(4 to 32),
        Seq.fill(32)(Random.nextInt(0 until robSize))
      )
    }
    def default(): Configuration = {
      Configuration(
        64,
        4,
        4,
        4,
        Seq.range(10,42)
      )
    }
  }
}
