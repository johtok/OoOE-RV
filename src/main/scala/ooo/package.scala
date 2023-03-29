import chisel3._
import chisel3.util.log2Ceil

package object ooo {

  case class Configuration(
                            reorderBufferSize: Int,
                            issueQueueSize: Int,
                            loadQueueSize: Int,
                            storeQueueSize: Int,
                            numOfSnapshotBuffers: Int
                          ) {
    val physRegisterIdWidth = log2Ceil(reorderBufferSize).W
    val loadIdWidth = log2Ceil(loadQueueSize).W
    val storeIdWidth = log2Ceil(storeQueueSize).W
    val branchIdWidth = log2Ceil(numOfSnapshotBuffers).W
  }

}
