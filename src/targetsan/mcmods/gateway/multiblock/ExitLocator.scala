package targetsan.mcmods.gateway.multiblock

import net.minecraft.init.Blocks
import net.minecraft.world.World
import targetsan.mcmods.gateway._
import targetsan.mcmods.gateway.Utils._

/** Locates suitable exit point based on location rating algorithm
  */
object ExitLocator {
	// Proper scan algorithm
	def findExit(fromWorld: World, fromPos: BlockPos): Either[String, BlockPos] = {
		val to = Utils.interDimension
		val center = translatePoint(fromWorld, fromPos, to)
		val LookupH = to.provider.getActualHeight / HeightFractions
		val offset = BlockPos(LookupR, LookupH, LookupR)

		findExit(to, center - offset to center + offset, center)
	}

	private def translatePoint(from: World, pos: BlockPos, to: World): BlockPos =
	{
		def mapCoord(c: Int) = Math.floor(c * from.provider.getMovementFactor / to.provider.getMovementFactor).toInt
		BlockPos(mapCoord(pos.x), (to.provider.getActualHeight - 1) / 2, mapCoord(pos.z))
	}

	private def findExit(world: World, vol: Volume, center: BlockPos): Either[String, BlockPos] =
	{
		def sqr(x: Int) = x * x
		val volFunc = scanVolume(world, vol)

		def distFactor(pos: BlockPos): Double =
			Math.log(sqr(pos.x - center.x) + sqr((pos.y - center.y) / 2) + sqr(pos.z - center.z) + 1)

		val (anchors, normals) = vol
			.enum
			.view
			.map(pos => (pos, ratePosition(pos, volFunc) ) ) // calculate position-independent rates
			.filter(_._2 != Int.MaxValue) // Get rid of invalid positions
			.map({ case (pos, r) => (pos, r + distFactor(pos)) }) // Add distance factor
			.sortBy(_._2) // Sort by rate
			.partition(_._2  < 0)

		anchors.length match {
			// exactly one anchor, as expected
			case 1 => Right(anchors.head._1)
			// no anchors, so just use most suitable
			case 0 => normals.headOption map { i => Right(i._1) } getOrElse Left("error.exit-lookup.not-found")
			// More than one anchor
			case _ => Left("error.exit-lookup.multiple-anchors")
		}
	}

	private object BlockType extends Enumeration
	{
		type BlockType = Value
		val None, Invalid, Solid, Complex, Liquid, Air, Anchor = Value

		def evalBlock(world: World, pos: BlockPos): BlockType = {
			val b = world.getBlock(pos.x, pos.y, pos.z)
			if (b.getBlockHardness(world, pos.x, pos.y, pos.z) < 0) BlockType.Invalid
			else if (b == Blocks.redstone_block) BlockType.Anchor
			else if (b.isAir(world, pos.x, pos.y, pos.z)) BlockType.Air
			else if (b.getMaterial.isLiquid) BlockType.Liquid
			else if (b.isBlockNormalCube) BlockType.Solid
			else BlockType.Complex
		}
	}
	// Scans volume and returns mapping - coordinates to block type
	// Algorithm suggests that endpoint volume shouldn't contain 'invalid' blocks
	// So presence of other gateways is marked by 'Invalid' type, returned in certain radius
	// Which is 1 less than dead zone - because endpoint radius is 1
	private val DeadR = 8
	private val LookupR = 5
	private val EndpointR = 3
	private val HeightFractions = 4 // N, used for 1/Nth of actual dimension height
	private val PortalPillarHeight = 3

	private type VolumeFunc = BlockPos => BlockType.BlockType

	private def scanVolume(w: World, vol: Volume): VolumeFunc =
	{
		val endpointDelta = BlockPos(EndpointR, 0, EndpointR)
		val lookVol = Volume(vol.min - endpointDelta, vol.max + endpointDelta)

		// Dead zone markup manipulation; no need to span across whole zone, as the region outside lookup is dead anyway
		val deadFieldStorage = Array.fill(lookVol.sizeX * lookVol.sizeZ)(false) // defines dead zone
		def deadIndex(x: Int, z: Int): Option[Int] =
			if ((lookVol.rangeX contains x) && (lookVol.rangeZ contains z))
				Some((x - lookVol.minX) * lookVol.sizeZ + z - lookVol.minZ)
			else None

		def isDead(x: Int, z: Int): Boolean =
			deadIndex(x, z) map { deadFieldStorage(_) } getOrElse true
		def setDead(x: Int, z: Int, value: Boolean): Unit =
			deadIndex(x, z) foreach { deadFieldStorage(_) = value }

		// Lookup zone manipulation
		val typeStorage = Array.fill(lookVol.sizeX * lookVol.sizeY * lookVol.sizeZ)(BlockType.None)
		def typeIndex(pos: BlockPos): Option[Int] =
			if (lookVol contains pos)
				Some(
					((pos.x - lookVol.minX) * lookVol.sizeY + (pos.y - lookVol.minY )) * lookVol.sizeZ + (pos.z - lookVol.minZ)
				)
			else None

		def getType(pos: BlockPos): BlockType.BlockType =
			if (isDead(pos.x, pos.z)) BlockType.Invalid
			else typeIndex(pos) map { typeStorage(_) } getOrElse BlockType.Invalid

		def setType(pos: BlockPos, t: BlockType.BlockType): Unit = typeIndex(pos) foreach { typeStorage(_) = t }
		// Perform full zone scan
		// This is original lookup zone, extended by dead zone radius
		val DeadMarkR = DeadR - EndpointR
		for ( pt <- Utils.enumVolume(vol.minX - DeadR, 1, vol.minZ - DeadR, vol.maxX + DeadR, w.provider.getActualHeight - 1, vol.maxZ + DeadR))
		{
			// Mark all columns in other GW's hard dead radius as dead
			for {
				e <- w.getTileEntity(pt.x, pt.y, pt.z).as[tile.Core]
				x1 <- pt.x - DeadMarkR to pt.x + DeadMarkR
				z1 <- pt.z - DeadMarkR to pt.z + DeadMarkR
			}
				setDead(x1, z1, true)
			// Qualify block only if it wasn't already qualified, or shadowed by dead zone
			if (getType(pt) == BlockType.None)
				setType(pt, BlockType.evalBlock(w, pt))
		}
		// Return getter func as-is
		getType
	}

	private def ratePosition(pos: BlockPos, volume: VolumeFunc): Int =
	{
		import BlockType._
		// Search for invalid blocks and lava in vicinity. Should prevent from dipping portal right into lava
		val volumeRate = Utils
			.enumVolume(pos.x - EndpointR, pos.y + 1, pos.z - EndpointR, pos.x + EndpointR, pos.y + PortalPillarHeight + 1, pos.z + EndpointR)
			.foldLeft(0)
		{ case (r, pos) =>
			volume(pos) match {
				case Invalid | Liquid => Int.MaxValue
				case _ => r
			}
		}
		// Calculate rate of region where portal pillar is located, along with near exit zone
		val pillarRate = Utils // Calculate ratings sum for upper part, which is pillar+shield
			.enumVolume(pos.x - 1, pos.y + 1, pos.z - 1, pos.x + 1, pos.y + PortalPillarHeight, pos.z + 1)
			.foldLeft(volumeRate)
		{ case (r, pos) =>
			if (r == Int.MaxValue) r
			else volume(pos) match {
				case Solid => r + 2
				case Complex => r + 1
				case _ => r
			}
		}

		val rate = Utils // Calculate ratings for lower part, which is main platform + extension
			.enumVolume(pos.x - 2, pos.y, pos.z - 2, pos.x + 2, pos.y, pos.z + 2)
			.foldLeft(pillarRate)
		{ case (r, pos) =>
			if (r == Int.MaxValue) r
			else volume(pos) match {
				case Invalid => Int.MaxValue
				case Solid => r
				case _ => r + 1
			}
		}

		if (rate == Int.MaxValue) rate
		else if (volume(pos) == Anchor) rate - 1000000
		else rate
	}

}
