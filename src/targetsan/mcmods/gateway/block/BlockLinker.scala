package targetsan.mcmods.gateway.block

import targetsan.mcmods.gateway.Utils._
import net.minecraft.world.{World, IBlockAccess}
import net.minecraftforge.common.util.ForgeDirection

import scala.reflect.ClassTag

trait BlockLinker {
	// Provides access to the linked block for the specified side
	// Works through linker tile entity
	def linkedBlockAs[T: ClassTag](world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection): Option[(T, World, BlockPos)]
}
