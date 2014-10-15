package targetsan.mcmods.gateway.multiblock

import net.minecraft.block.BlockLiquid
import net.minecraft.init.Blocks
import net.minecraft.world.World
import targetsan.mcmods.gateway._
import targetsan.mcmods.gateway.Utils._

/** Locates suitable exit point based on location rating algorithm
  */
object ExitLocator {
	// Proper scan algorithm
	private def findExit(from: World, x0: Int, y0: Int, z0: Int): Either[String, (Int, Int, Int)] = {
		val to = Utils.interDimension
		val center = translatePoint(from, x0, y0, z0, to)
		val LookupH = to.provider.getActualHeight / HeightFractions
		val offset = BlockPos(LookupR, LookupH, LookupR)

		val volume = scanVolume(to, cx, cy, cz)



		findExit(to, center - offset, center + offset, center)
	}

	private def translatePoint(from: World, x: Int, y: Int, z: Int, to: World): BlockPos =
	{
		def mapCoord(c: Int) = Math.floor(c * from.provider.getMovementFactor / to.provider.getMovementFactor).toInt
		BlockPos(mapCoord(x), (to.provider.getActualHeight - 1) / 2, mapCoord(z))
	}

	private def findExit(world: World, min: BlockPos, max: BlockPos, center: BlockPos): Either[String, (Int, Int, Int)] =
	{
		def sqr(x: Int) = x * x

		def distFactor(x: Int, y: Int, z: Int): Double =
			Math.log(sqr(x - center.x) + sqr((y - center.y) / 2) + sqr(z - center.z) + 1)

		val (anchors, normals) = Utils
			.enumVolume(min, max)
			.view
			.map(pos => (pos._1, pos._2, pos._3, ratePosition(pos._1, pos._2, pos._3, volume) ) ) // calculate position-independent rates
			.filter(_._4 != Int.MaxValue) // Get rid of invalid positions
			.map({ case (x, y, z, r) => (x, y, z, r + distFactor(x, y, z)) }) // Add distance factor
			.sortBy(_._4) // Sort by rate
			.partition(_._4  < 0)

		anchors.length match {
			case 1 => // exactly one anchor, as expected
				val pos = anchors.head
				Right((pos._1, pos._2, pos._3))
			case 0 => // no anchors, so just use most suitable
				normals.headOption match {
					case Some((x, y, z, _)) => Right((x, y, z))
					case _ => Left("Gateway cannot open - there are obstacles on the other side. Might be other gateway too near")
				}
			case _ => Left("More than one usable anchor detected, so portal cannot open. Please remove all except one, or let it open by itself")
		}
	}

	private object BlockType extends Enumeration
	{
		type BlockType = Value
		val None, Invalid, Solid, Complex, Liquid, Air, Anchor = Value
	}
	// Scans volume and returns mapping - coordinates to block type
	// Algorithm suggests that endpoint volume shouldn't contain 'invalid' blocks
	// So presence of other gateways is marked by 'Invalid' type, returned in certain radius
	// Which is 1 less than dead zone - because endpoint radius is 1
	private val DeadR = 8
	private val LookupR = 5
	private val EndpointR = 3
	private val HeightFractions = 4 // N, used for 1/Nth of actual dimension height

	private val EffDeadR = DeadR - EndpointR
	private val EffLookupR = LookupR + EndpointR

	private type VolumeFunc = (Int, Int, Int) => BlockType.BlockType

	private def scanVolume(w: World, cx: Int, cy: Int, cz: Int): VolumeFunc =
	{
		val LookupH = w.provider.getActualHeight / HeightFractions

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
		for ( (x, y, z) <- Utils.enumVolume(cx - FullR, 1, cz - FullR, cx + FullR, w.provider.getActualHeight - 1, cz + FullR))
		{
			val entity = w.getTileEntity(x, y, z)
			// Mark all columns in other GW's 'dead zone' as 'dead'
			if (entity != null && entity.isInstanceOf[TileGateway])
				for {x1 <- x - EffDeadR to x + EffDeadR; z1 <- z - EffDeadR to z + EffDeadR}
					setDead(x1, z1, true)
			// Qualify block only if it wasn't already qualified, or shadowed by dead zone
			if (getType(x, y, z) == BlockType.None)
				setType(x, y, z,
				{
					val b = w.getBlock(x, y, z)
					if (b.getBlockHardness(w, x, y, z) < 0) BlockType.Invalid
					else if (b == Blocks.redstone_block) BlockType.Anchor
					else if (b.isAir(w, x, y, z)) BlockType.Air
					else if (b.isInstanceOf[BlockLiquid]) BlockType.Liquid
					else if (b.isBlockNormalCube) BlockType.Solid
					else BlockType.Complex
				}
				)
		}
		// Return getter func as-is
		getType
	}

	private def ratePosition(x: Int, y: Int, z: Int, volume: VolumeFunc): Int =
	{
		import BlockType._
		// Search for invalid blocks and lava in vicinity. Should prevent from dipping portal right into lava
		val volumeRate = Utils
			.enumVolume(x - EndpointR, y + 1, z - EndpointR, x + EndpointR, y + PortalPillarHeight + 1, z + EndpointR)
			.foldLeft(0)
		{ case (r, (x, y, z)) =>
			volume(x, y, z) match {
				case Invalid | Liquid => Int.MaxValue
				case _ => r
			}
		}
		// Calculate rate of region where portal pillar is located, along with near exit zone
		val pillarRate = Utils // Calculate ratings sum for upper part, which is pillar+shield
			.enumVolume(x - 1, y + 1, z - 1, x + 1, y + PortalPillarHeight, z + 1)
			.foldLeft(volumeRate)
		{ case (r, (x, y, z)) =>
			if (r == Int.MaxValue) r
			else volume(x, y, z) match {
				case Solid => r + 2
				case Complex => r + 1
				case _ => r
			}
		}

		val rate = Utils // Calculate ratings for lower part, which is main platform + extension
			.enumVolume(x - 2, y, z - 2, x + 2, y, z + 2)
			.foldLeft(pillarRate)
		{ case (r, (x, y, z)) =>
			if (r == Int.MaxValue) r
			else volume(x, y, z) match {
				case Invalid => Int.MaxValue
				case Solid => r
				case _ => r + 1
			}
		}

		if (rate == Int.MaxValue) rate
		else if (volume(x, y, z) == Anchor) rate - 1000000
		else rate
	}

}
