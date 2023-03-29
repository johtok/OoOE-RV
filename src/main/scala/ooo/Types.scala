package ooo

import chisel3._

object Types {

  object ArchRegisterId { def apply() = UInt(5.W) }
  object PhysRegisterId { def apply()(implicit c: Configuration) = UInt(c.physRegisterIdWidth) }
  object BranchId { def apply()(implicit c: Configuration) = UInt(c.branchIdWidth) }
  object LoadId { def apply()(implicit c: Configuration) = UInt(c.loadIdWidth) }
  object StoreId { def apply()(implicit c: Configuration) = UInt(c.storeIdWidth) }


}
