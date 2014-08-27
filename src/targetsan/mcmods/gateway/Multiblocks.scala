package targetsan.mcmods.gateway

import net.minecraft.world.World
import net.minecraft.init.Blocks
import scala.util.Try
import scala.util.Success
import net.minecraft.entity.player.EntityPlayer
import scala.util.Failure
import net.minecraft.block.BlockLiquid

trait Multiblock
{
	protected val PortalPillarHeight = 3
	// Perform full multiblock assembly, including construction of both endpoints
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
object RedstoneCoreMultiblock extends MultiblockImpl
{
	override def assemble(world: World, x: Int, y: Int, z: Int, owner: EntityPlayer): Try[Boolean] =
	{
		val to = Gateway.dimension
		if (!isMultiblockPresent(world, x, y, z))
			return Success(false)
			
		if (world.provider.dimensionId == to.provider.dimensionId)
			return Failure(new Exception(s"Redstone isn't powerful enough to open gateway from ${to.provider.getDimensionName()}. Try something different."))

		findExit(world, x, y, z) match
		{
			case Left(message) => Failure(new Exception(message))
			case Right((ex, ey, ez)) =>
				mutualInit(world, x, y, z, NetherMultiblock, to, ex, ey, ez, owner)
				Success(true)
		}
	}

	override def disassemble(world: World, x: Int, y: Int, z: Int) =
	{
		// dispose everything above platform
		for {
			(x, y, z) <- Utils.enumVolume(x, y + 1, z, x, y + PortalPillarHeight, z)
		}
			if (world.getBlock(x, y, z) == GatewayMod.BlockGatewayAir)
				world.setBlockToAir(x, y, z)
		// dispose core
		//world.setBlock(x, y, z, Blocks.netherrack)
		world.setBlock(x, y, z, Blocks.redstone_block)
		// dispose platform
		for ((_, sat) <- GatewayMod.BlockGatewayBase.satellites)
			world
				.setBlock(
					x + sat.xOffset, y, z + sat.zOffset,
					//if (sat.isDiagonal) Blocks.obsidian else Blocks.gravel // disabled temporarily
					if (sat.isDiagonal) Blocks.obsidian else Blocks.glass // temporary stub - returns original multiblock
				)
	}
	
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
	
	private def translatePoint(from: World, x: Int, y: Int, z: Int, to: World): (Int, Int, Int) =
	{
		def mapCoord(c: Int) = Math.round(c * from.provider.getMovementFactor() / to.provider.getMovementFactor()).toInt
		(mapCoord(x), (to.provider.getActualHeight - 1) / 2, mapCoord(z))
	}
	
	private def findExit(from: World, x: Int, y: Int, z: Int): Either[String, (Int, Int, Int)] =
	{
		val to = Gateway.dimension
		// Compute destination coordinates
		val (ex, ey, ez) = translatePoint(from, x, y, z, to)
		// Check if destination point is free
		if (!isDestinationFree(to, ex, ey, ez))
			Left(s"Another gateway's exit in the ${to.provider.getDimensionName()} prevents this gateway from opening")
		else
			Right((ex, ey, ez))
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
		Utils.enumVolume(
				x - Radius, 0, z - Radius,
				x + Radius, to.provider.getActualHeight - 1, z + Radius
			)
			.forall { case (x, y, z) => to.getBlock(x, y, z) != GatewayMod.BlockGatewayBase }
	}
	// Proper scan algorithm
	private def findExit2(from: World, x0: Int, y0: Int, z0: Int): Either[String, (Int, Int, Int)] =
	{
		val to = Gateway.dimension
		val (cx, cy, cz) = translatePoint(from, x0, y0, z0, to)
		
		val DeadZoneRadius = 7
		val LookupRadius = 3
		val LookupHeight = to.provider.getActualHeight() / 4
		
		var endpoints: List[(Int, Int, Int, Int)] = Nil
		
		val endpointVolume = mapVolume(to, cx - LookupRadius, cy - LookupHeight, cz - LookupRadius, cx + LookupRadius, cy + LookupHeight, cz + LookupRadius)
		// First pass. This one scans for block types in placement area and marks unavailable columns due to presense of near gateways
		Utils
		.enumVolume(cx - DeadZoneRadius, 0, cz - DeadZoneRadius, cx + DeadZoneRadius, to.provider.getActualHeight - 1, cz + DeadZoneRadius)
		.foreach { case (x, y, z) =>
			val entity = to.getTileEntity(x, y, z)
			if (entity != null && entity.isInstanceOf[TileGateway]) // detect nearby cores
				return Left("Another gateway is too near to the possible endpoint in the ")
		}
		
		Right((cx, cy, cz))
	}
	
	private object BlockType extends Enumeration
	{
		type BlockType = Value
		val None, Invalid, Solid, Complex, Liquid, Air = Value
	}
	// Scans volume and returns mapping - coordinates to block type
	// Algorithm suggests that endpoint volume shouldn't contain 'invalid' blocks
	// So presense of other gateways is marked by 'Invalid' type, returned in certain radius
	// Which is 1 less than dead zone - because endpoint radius is 1
	private val DeadR = 4
	private val LookupR = 3
	private val EndpointR = 2
	private val HeightFractions = 4 // N, used for 1/Nth of actual dimension height
	
	private val EffDeadR = DeadR - EndpointR
	private val EffLookupR = LookupR + EndpointR
	
	private def scanVolume(w: World, cx: Int, cy: Int, cz: Int): (Int, Int, Int) => BlockType.BlockType =
	{
		val LookupH = w.provider.getActualHeight() / HeightFractions

		val lookupX = cx - EffLookupR to cx + EffLookupR
		val lookupY = cy - LookupH to cy + LookupH
		val lookupZ = cz - EffLookupR to cz + EffLookupR
		// Dead zone markup manipulation; no need to span across whole zone, as the region outside lookup is dead anyway
		val deadFieldStorage = Array.fill(lookupX.length * lookupZ.length)(false) // defines dead zone
		def isDead(x: Int, z: Int): Boolean =
			if ((lookupX contains x) && (lookupZ contains z))
				deadFieldStorage((x - lookupX.start) * lookupZ.length + z - lookupZ.start)
			else true

		def setDead(x: Int, z: Int, value: Boolean): Unit =
			if ((lookupX contains x) && (lookupZ contains z))
				deadFieldStorage((x - lookupX.start) * lookupZ.length + z - lookupZ.start) = value
		// Lookup zone manipulation
		val typeStorage = Array.fill(lookupX.length * lookupY.length * lookupZ.length)(BlockType.None)
		def getType(x: Int, y: Int, z: Int): BlockType.BlockType =
			if (
				!(lookupX contains x) ||
				!(lookupY contains y) ||
				!(lookupZ contains z) ||
				isDead(x, z)
			)
				BlockType.Invalid
			else typeStorage(
				((x - lookupX.start) * lookupY.length + (y - lookupY.start )) * lookupZ.length + (z - lookupZ.start)
			)
		def setType(x: Int, y: Int, z: Int, t: BlockType.BlockType): Unit =
			if ( (lookupX contains x) &&
				 (lookupY contains y) &&
				 (lookupZ contains z)
			)
				typeStorage(
						((x - lookupX.start) * lookupY.length + (y - lookupY.start )) * lookupZ.length + (z - lookupZ.start)
				) = t
		// Perform full zone scan
		val FullR = EffLookupR + EffDeadR
		for ( (x, y, z) <- Utils.enumVolume(cx - FullR, 1, cz - FullR, cx + FullR, w.provider.getActualHeight() - 1, cz + FullR))
		{
			val entity = w.getTileEntity(x, y, z)
			// Mark all columns in other GW's 'dead zone' as 'dead'
			if (entity != null && entity.isInstanceOf[TileGateway])
				for {x1 <- x - EffDeadR to x + EffDeadR; z1 <- z - EffDeadR to z + EffDeadR}
					setDead(x, y, true)
			// Qualify block only if it wasn't already qualified, or shadowed by dead zone
			if (getType(x, y, z) == BlockType.None)
				setType(x, y, z,
					{
						val b = w.getBlock(x, y, z)
						if (b.getBlockHardness(w, x, y, z) < 0) BlockType.Invalid
						else if (b.isAir(w, x, y, z)) BlockType.Air
						else if (b.isInstanceOf[BlockLiquid]) BlockType.Liquid 
						else if (b.isBlockNormalCube()) BlockType.Solid
						else BlockType.Complex
					}
				)
		}
		// Return getter func as-is
		getType
	}
}

object NetherMultiblock extends MultiblockImpl
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
		for ((x, y, z) <- Utils.enumVolume(x - 2, y, z - 2, x + 2, y, z + 2))
			if (world.isAirBlock(x, y, z))
				world.setBlock(x, y, z, Blocks.stone)
		// shielding
		for ((x, y, z) <- Utils.enumVolume(x - 1, y + 1, z - 1, x + 1, y + PortalPillarHeight , z + 1))
			if (world.getBlock(x, y, z) != GatewayMod.BlockGatewayAir)
				world.setBlock(x, y, z, GatewayMod.BlockGatewayAir, GatewayMod.BlockGatewayAir.Shield, 3)
	}

	override def disassemble(world: World, x: Int, y: Int, z: Int) =
	{
		// dispose everything above platform
		for {
			(x, y, z) <- Utils.enumVolume(x - 1, y + 1, z - 1, x + 1, y + PortalPillarHeight, z + 1)
		}
			if (world.getBlock(x, y, z) == GatewayMod.BlockGatewayAir)
				world.setBlockToAir(x, y, z)
		// dispose platform
		for ((x, y, z) <- Utils.enumVolume(x - 1, y, z - 1, x + 1, y, z + 1))
			world.setBlock(x, y, z, Blocks.netherrack)
		// dispose core
		world.setBlock(x, y, z, Blocks.obsidian)
	}
}
