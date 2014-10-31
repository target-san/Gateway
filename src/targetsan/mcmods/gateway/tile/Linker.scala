package targetsan.mcmods.gateway.tile

import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection
import scala.reflect.ClassTag

import targetsan.mcmods.gateway._
import targetsan.mcmods.gateway.Utils._

trait Linker {
	// Retrieves linked block class, along with its position
	// Returns value only if block is of requested type
	// Used almost exclusively by BlockLinker
	def linkedBlockAs[T: ClassTag](side: ForgeDirection): Option[(T, World, BlockPos)]
	// Returns linked tile entity if it's presend and of the required type
	def linkedTileAs[T: ClassTag](side: ForgeDirection): Option[T]
}
