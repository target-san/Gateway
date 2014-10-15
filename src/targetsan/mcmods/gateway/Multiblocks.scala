package targetsan.mcmods.gateway

import net.minecraft.world.World
import net.minecraft.init.Blocks
import targetsan.mcmods.gateway.block.Core
import scala.util.Try
import scala.util.Success
import net.minecraft.entity.player.EntityPlayer
import scala.util.Failure
import net.minecraft.block.{Block, BlockLiquid}

trait Multiblock
{
	// Perform full multiblock assembly, including construction of both endpoints
	def assemble(world: World, x: Int, y: Int, z: Int, owner: EntityPlayer): Try[Boolean]
	def disassemble(world: World, x: Int, y: Int, z: Int): Unit
}

trait MultiblockImpl extends Multiblock
{
	protected val PortalPillarHeight = 3
	protected def core: Core

	protected def rawAssemble(world: World, x: Int, y: Int, z: Int) =
	{
		// Satellite platform blocks
		for ((_, sat) <- GatewayMod.BlockGateway.satellites)
			sat.place(world, x + sat.xOffset, y, z + sat.zOffset)
		// Portal column
		for (y1 <- y+1 to y+PortalPillarHeight )
			GatewayMod.BlockGateway.Pillar.place(world, x, y1, z)
	}
	
	protected def mutualInit(from: World, x1: Int, y1: Int, z1: Int, endpoint: MultiblockImpl, to: World, x2: Int, y2: Int, z2: Int, owner: EntityPlayer)
	{
		core.place(from, x1, y1, z1)
		endpoint.core.place(to, x2, y2, z2)
		// Place and initialize cores before satellite blocks.
		// Because cores are kind of self-sufficient, but satellites are not
		val core1 = from.getTileEntity(x1, y1, z1).asInstanceOf[TileGateway]
		val core2 = to.getTileEntity(x2, y2, z2).asInstanceOf[TileGateway]

		core1.init(core2, owner)
		core2.init(core1, owner)

		rawAssemble(from, x1, y1, z1)
		endpoint.rawAssemble(to, x2, y2, z2)
	}
	// Provides blocks which replace
	protected def platformReplacement(dx: Int, dz: Int): Block

	override def disassemble(world: World, x: Int, y: Int, z: Int): Unit = {
		// dispose pillar
		for ( y <- (y + 1) to (y + PortalPillarHeight) )
			if (world.getBlock(x, y, z) == GatewayMod.BlockGateway)
				world.setBlockToAir(x, y, z)
		// dispose platform
		for ((_, sat) <- GatewayMod.BlockGateway.satellites)
			world
				.setBlock(x + sat.xOffset, y, z + sat.zOffset, platformReplacement(sat.xOffset, sat.zOffset))
		// dispose core
		world.setBlock(x, y, z, platformReplacement(0, 0))
	}

}
/// The simplest version - 3x3, obsidian on corners, glass on sides, redstone block in center
object RedstoneCoreMultiblock extends MultiblockImpl
{
	override def core = GatewayMod.BlockGateway.RedstoneCore

	override def assemble(world: World, x: Int, y: Int, z: Int, owner: EntityPlayer): Try[Boolean] =
	{
		val to = Utils.interDimension
		if (!isMultiblockPresent(world, x, y, z))
			return Success(false)

		world.provider.dimensionId match {
			case Utils.InterDimensionId => // Cannot be opened from Nether. At least not this type.
				return Failure(new Exception(s"Redstone isn't powerful enough to open gateway from ${to.provider.getDimensionName}. Try something different."))
			case Utils.EndDimensionId => // Prevent opening from End if dragon is still alive; stub here
			case _ => ()
		}

		findExit(world, x, y, z) match
		{
			case Left(message) => Failure(new Exception(message))
			case Right((ex, ey, ez)) =>
				mutualInit(world, x, y, z, NetherMultiblock, to, ex, ey, ez, owner)
				Success(true)
		}
	}

	override def platformReplacement(dx: Int, dz: Int) =
		(dx, dz) match {
			case (0, 0) => Assets.netherrack
			case (x, z) if x == 0 || z == 0 => Assets.gravel
			case _ => Assets.obsidian
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

	//******************************************************************************************************************
	// Calculation of exit position
	//******************************************************************************************************************

}

object NetherMultiblock extends MultiblockImpl
{
	override def core = GatewayMod.BlockGateway.MirrorCore
	// prevents from using this as standalone multiblock
	override def assemble(world: World, x: Int, y: Int, z: Int, owner: EntityPlayer): Try[Boolean] = Success(false)

	override def rawAssemble(world: World, x: Int, y: Int, z: Int) =
	{
		// main platform and pillar
		super.rawAssemble(world, x, y, z)
		// Air around pillar
		for ((x, y, z) <- Utils.enumVolume(x - 1, y + 1, z - 1, x + 1, y + PortalPillarHeight , z + 1))
			if (world.getBlock(x, y, z) != GatewayMod.BlockGateway)
				world.setBlockToAir(x, y, z)
		// additional platform
		for ((x, y, z) <- Utils.enumVolume(x - 2, y, z - 2, x + 2, y, z + 2))
			if (world.isAirBlock(x, y, z))
				world.setBlock(x, y, z, Assets.stone)
	}

	override def platformReplacement(dx: Int, dz: Int) =
		if (dx == 0 && dz == 0) Assets.obsidian
		else Assets.netherrack
}
