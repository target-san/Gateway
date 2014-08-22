package targetsan.mcmods.gateway

import net.minecraft.world.World
import net.minecraft.init.Blocks
import scala.util.Try
import scala.util.Success
import net.minecraft.entity.player.EntityPlayer
import scala.util.Failure

/**
 * Some notes.
 * 1. These three methods are united here _only_ because they control multiblock.
 * 2. canAssembleHere and assemble are used to construct multiblock,
 *    _but_ disassemble is used by multiblock's control entity to remove it.
 *    So creation and disposal are separated. 
 */
trait Multiblock
{
	protected val PortalPillarHeight = 3
	// This one only assembles multiblock
	def assemble(world: World, x: Int, y: Int, z: Int, owner: EntityPlayer): Try[Boolean]
	def disassemble(world: World, x: Int, y: Int, z: Int): Unit
}

trait MultiblockImpl extends Multiblock
{
	protected def rawAssemble(world: World, x: Int, y: Int, z: Int)
	
	protected def mutualInit(from: World, x1: Int, y1: Int, z1: Int, endpoint: MultiblockImpl, to: World, x2: Int, y2: Int, z2: Int, owner: EntityPlayer)
	{
		rawAssemble(from, x1, y1, z1)
		endpoint.rawAssemble(to, x2, y2, z2)
		
		val core1 = from.getTileEntity(x1, y1, z1).asInstanceOf[TileGateway]
		val core2 = to.getTileEntity(x2, y2, z2).asInstanceOf[TileGateway]
		
		core1.init(core2, owner)
		core2.init(core1, owner)
	}
}
/// The simplest version - 3x3, obsidian on corners, glass on sides, redstone block in center
class RedstoneCoreMultiblock extends MultiblockImpl
{
	override def assemble(world: World, x: Int, y: Int, z: Int, owner: EntityPlayer): Try[Boolean] =
	{
		val to = Gateway.dimension
		if (!isMultiblockPresent(world, x, y, z))
			return Success(false)
			
		if (world.provider.dimensionId == to.provider.dimensionId)
			return Failure(new Exception(s"Redstone isn't powerful enough to open gateway from ${to.provider.getDimensionName()}. Try something different."))
		
		// Compute destination coordinates; TODO: implement volume lookup for optimal location 
		def mapCoord(c: Int) = Math.round(c * world.provider.getMovementFactor() / to.provider.getMovementFactor()).toInt
		
		val ex = mapCoord(x)
		val ey = (to.provider.getActualHeight - 1) / 2
		val ez = mapCoord(z)
		// Check if destination point is free
		if (!isDestinationFree(to, ex, ey, ez))
			return Failure(new Exception(s"Another gateway's exit in the ${to.provider.getDimensionName()} prevents this gateway from opening"))

		mutualInit(world, x, y, z, companion, to, ex, ey, ez, owner)
		Success(true)
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
	
	private val companion = new NetherMultiblock
	
	override def rawAssemble(world: World, x: Int, y: Int, z: Int)
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

	private def isMultiblockPresent(w: World, x: Int, y: Int, z: Int) =
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

    // Checks if there are no active gateways in the nether too near
	private def isDestinationFree(to: World, x: Int, y: Int, z: Int): Boolean =
	{
		val Radius = 7
		Utils.enumVolume(to,
				x - Radius, 0, z - Radius,
				x + Radius, to.provider.getActualHeight - 1, z + Radius
			)
			.forall { case (x, y, z) => to.getBlock(x, y, z) != GatewayMod.BlockGatewayBase }
	}
}

class NetherMultiblock extends MultiblockImpl
{
	// prevents from using this as standalone multiblock
	override def assemble(world: World, x: Int, y: Int, z: Int, owner: EntityPlayer): Try[Boolean] = Success(false)

	override def rawAssemble(world: World, x: Int, y: Int, z: Int) =
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
