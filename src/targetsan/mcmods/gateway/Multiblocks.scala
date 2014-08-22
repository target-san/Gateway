package targetsan.mcmods.gateway

import net.minecraft.world.World
import net.minecraft.init.Blocks

/**
 * Some notes.
 * 1. These three methods are united here _only_ because they control multiblock.
 * 2. canAssembleHere and assemble are used to construct multiblock,
 *    _but_ disassemble is used by multiblock's control entity to remove it.
 *    So creation and disposal are separated. 
 */
trait Multiblock // FIXME: more proper name
{
	val PortalPillarHeight = 3
	// This one is used only when endpoint is constructed by player, i.e. it checks presense of valid multiblock
	def canAssembleHere(world: World, x: Int, y: Int, z: Int): Boolean
	def assemble(world: World, x: Int, y: Int, z: Int): Unit
	def disassemble(world: World, x: Int, y: Int, z: Int): Unit
}
/// The simplest version - 3x3, obsidian on corners, glass on sides, redstone block in center
class RedstoneCoreMultiblock extends Multiblock
{
	override def canAssembleHere(w: World, x: Int, y: Int, z: Int) =
		w.getBlock(x, y, z) == Blocks.redstone_block &&
		// corners
		w.getBlock(x - 1, y, z - 1) == Blocks.obsidian &&
		w.getBlock(x + 1, y, z - 1) == Blocks.obsidian &&
		w.getBlock(x - 1, y, z + 1) == Blocks.obsidian &&
		w.getBlock(x + 1, y, z + 1) == Blocks.obsidian &&
		// sides
		w.getBlock(x - 1, y, z) == Blocks.glass &&
		w.getBlock(x + 1, y, z) == Blocks.glass &&
		w.getBlock(x, y, z - 1) == Blocks.glass &&
		w.getBlock(x, y, z + 1) == Blocks.glass

	override def assemble(world: World, x: Int, y: Int, z: Int) =
	{
		// Core
		world.setBlock(x, y, z, GatewayMod.BlockGatewayBase , GatewayMod.BlockGatewayBase.RedstoneCore, 3)
		// Satellite platform blocks
		for ((i, sat) <- GatewayMod.BlockGatewayBase.satellites)
			world.setBlock(x + sat.xOffset, y, z + sat.zOffset, GatewayMod.BlockGatewayBase, i, 3)
		// Portal column
		for (y1 <- y+1 to y+PortalPillarHeight )
			GatewayMod.BlockGatewayAir.placePortal(world, x, y1, z)
	}

	override def disassemble(world: World, x: Int, y: Int, z: Int) =
	{
		// dispose everything above platform
		for {
			(x, y, z) <- Utils.enumVolume(world, x, y + 1, z, x, y + PortalPillarHeight, z)
		}
			if (world.getBlock(x, y, z) == GatewayMod.BlockGatewayAir)
				world.setBlockToAir(x, y, z)
		// dispose core
		world.setBlock(x, y, z, Blocks.netherrack)
		// dispose platform
		for ((_, sat) <- GatewayMod.BlockGatewayBase.satellites)
			world
				.setBlock(
					x + sat.xOffset, y, z + sat.zOffset,
					if (sat.isDiagonal) Blocks.obsidian else Blocks.gravel
				)
	}
}

class NetherMultiblock extends Multiblock
{
	// prevents from using this as standalone multiblock
	override def canAssembleHere(w: World, x: Int, y: Int, z: Int) = false

	override def assemble(world: World, x: Int, y: Int, z: Int) =
	{
		// Core
		world.setBlock(x, y, z, GatewayMod.BlockGatewayBase , GatewayMod.BlockGatewayBase.MirrorCore, 3)
		// Satellite platform blocks
		for ((i, sat) <- GatewayMod.BlockGatewayBase.satellites)
			world.setBlock(x + sat.xOffset, y, z + sat.zOffset, GatewayMod.BlockGatewayBase, i, 3)
		// Portal column
		for (y1 <- y+1 to y+PortalPillarHeight )
			GatewayMod.BlockGatewayAir.placePortal(world, x, y1, z)
		// additional platform
		for ((x, y, z) <- Utils.enumVolume(world, x - 2, y, z - 2, x + 2, y, z + 2))
			if (world.isAirBlock(x, y, z))
				world.setBlock(x, y, z, Blocks.stone)
		// shielding
		for ((x, y, z) <- Utils.enumVolume(world, x - 1, y + 1, z - 1, x + 1, y + PortalPillarHeight , z + 1))
			if (world.getBlock(x, y, z) != GatewayMod.BlockGatewayAir)
				world.setBlock(x, y, z, GatewayMod.BlockGatewayAir, GatewayMod.BlockGatewayAir.Shield, 3)
	}

	override def disassemble(world: World, x: Int, y: Int, z: Int) =
	{
		// dispose everything above platform
		for {
			(x, y, z) <- Utils.enumVolume(world, x - 1, y + 1, z - 1, x + 1, y + PortalPillarHeight, z + 1)
		}
			if (world.getBlock(x, y, z) == GatewayMod.BlockGatewayAir)
				world.setBlockToAir(x, y, z)
		// dispose platform
		for ((x, y, z) <- Utils.enumVolume(world, x - 1, y, z - 1, x + 1, y, z + 1))
			world.setBlock(x, y, z, Blocks.netherrack)
		// dispose core
		world.setBlock(x, y, z, Blocks.obsidian)
	}
}
