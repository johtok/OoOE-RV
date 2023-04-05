import chisel3._
import chisel3.util.log2Ceil

import scala.util.Random
import ooo.util._

package object ooo {

  case class Configuration(
                            reorderBufferSize: Int,
                            issueQueueSize: Int,
                            loadQueueSize: Int,
                            storeQueueSize: Int,
                            numOfSnapshotBuffers: Int,
                            initialStateMap: Seq[Int]
                          ) {
    val physRegisterIdWidth = log2Ceil(reorderBufferSize).W
    val loadIdWidth = log2Ceil(loadQueueSize).W
    val storeIdWidth = log2Ceil(storeQueueSize).W
    val branchIdWidth = log2Ceil(numOfSnapshotBuffers).W
  }

  object Configuration {
    def random(): Configuration = {
      val robSize = Random.nextPow2(32 to 1024)
      ooo.Configuration(
        robSize,
        Random.nextPow2(16 to 64),
        Random.nextPow2(4 to 32),
        Random.nextPow2(4 to 32),
        Random.nextPow2(2 to 8),
        Seq.fill(32)(Random.nextInt(0 until robSize))
      )
    }
  }




}
