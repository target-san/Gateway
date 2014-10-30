package targetsan.mcmods.gateway.tile

import java.util.UUID

import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.{Vec3, ChunkCoordinates}
import net.minecraft.world.{World, WorldServer}
import net.minecraftforge.common.MinecraftForge
import targetsan.mcmods.gateway._
import targetsan.mcmods.gateway.Utils._

class Core extends Gateway {
	private var partnerPos: BlockPos = null
	private var partnerWorld: World = null
	private var ownerId: UUID = null
	private var ownerName = ""
	private var storedBlocks: block.Multiblock.BunchOfBlocks = null

	//******************************************************************************************************************
	// State flag field parts
	//******************************************************************************************************************

	// Multiblock assembled marker
	// size: 1 offset: 0
	override def isAssembled = getFlag(0)
	override def isAssembled_= (value: Boolean): Unit = { setFlag(0, value) }

	// All public funcs should check this, ensures that gateway multiblock is assembled, and we're on logical server
	private def isAlive = !worldObj.isRemote && isAssembled

	// Side disposal marks
	// offset: 4, size: 4
	private def tileToMark(tile: BlockPos) = offsetToDirection(tile - BlockPos(this)).ordinal() - 2

	private def canMark(tile: BlockPos): Boolean = 0 to 3 contains tileToMark(tile)
	private def setDisposalMark(tile: BlockPos, value: Boolean) =
		if (canMark(tile)) setFlag(4 + tileToMark(tile), value)
	private def areMarksSet = getState(4, 4) == 0x0F

	//******************************************************************************************************************
	// Lifecycle control
	//******************************************************************************************************************
	def init(owner: EntityPlayer, partner: Core, savedBlocks: block.Multiblock.BunchOfBlocks): Unit = {
		if (worldObj.isRemote) return // server only
		if (isAssembled || isInvalid)
			throw new IllegalStateException("Gateway core can be initialized only once. Init should be done only for valid tile")

		isAssembled = true

		partnerPos = BlockPos(partner)
		partnerWorld = partner.getWorldObj
		ownerId = owner.getGameProfile.getId
		ownerName = owner.getGameProfile.getName
		storedBlocks = savedBlocks

		enumGatewayTiles foreach { _.isAssembled = true }

		markDirty()
	}

	override def invalidate(): Unit = {
		super.invalidate()
		if (!isAlive)
			return

		isAssembled = false
		// Phase 1 - disassemble; this tile will be also marked
		enumGatewayTiles foreach { _.isAssembled = false }
		// Phase 2 - remove all, replace with what's been here before
		block.Multiblock.disassemble(worldObj, BlockPos(this), storedBlocks)
		// Phase 3 - kill other side
		partnerWorld
			.getTileEntity(partnerPos.x, partnerPos.y, partnerPos.z)
			.as[Core]
			.foreach { _.invalidate() }
	}

	private def enumGatewayTiles =
		for {
			(offset, part) <- block.Multiblock.Parts
			tile <- worldObj.getTileEntity(xCoord + offset.x, yCoord + offset.y, zCoord + offset.z).as[tile.Gateway]
		}
			yield tile

	//******************************************************************************************************************
	// Teleport support
	//******************************************************************************************************************
	private var teleportSet = Set.empty[Entity]
	// Enqueue for teleporting
	override def teleport(entity: Entity): Unit = {
		// Does good only for working gateway, on server, with valid entity and no cooldown on it
		if (!isAlive || entity == null || entity.timeUntilPortal > 0)
			return

		teleportSet += bottomMount(entity)
	}
	// We can't properly teleport riding entities on entity update loop,
	// so we're scheduling TP till TE update loop and then do the action
	override def updateEntity(): Unit = {
		super.updateEntity()
		if (!isAlive) return

		if (teleportSet.nonEmpty) {
			for (e <- teleportSet)
				doTeleport(e)
			teleportSet = Set.empty
		}
	}
	// Performs actual teleportation
	private def doTeleport(entity: Entity): Unit = {
		val (ex, ey, ez) = getExitPos(entity)

		val enterEvent = new api.GatewayEnterEvent(
			entity,
			new ChunkCoordinates(xCoord, yCoord, zCoord),
			worldObj,
			partnerPos.toChunkCoordinates,
			partnerWorld,
			Vec3.createVectorHelper(ex, ey, ez))

		if (MinecraftForge.EVENT_BUS.post(enterEvent))
			return

		val server = enterEvent.destWorld.as[WorldServer].getOrElse(Utils.world(enterEvent.destWorld.provider.dimensionId))
		val newEntity = EP3Teleporter.apply(entity, enterEvent.destPos.xCoord, enterEvent.destPos.yCoord, enterEvent.destPos.zCoord, server)
		if (newEntity == null)
			return

		for (r <- enumRiders(newEntity))
			r.timeUntilPortal = DefaultCooldown

		MinecraftForge.EVENT_BUS.post(
			new api.GatewayLeaveEvent(
				newEntity,
				new ChunkCoordinates(xCoord, yCoord, zCoord),
				worldObj,
				partnerPos.toChunkCoordinates,
				partnerWorld)
		)

	}

	//******************************************************************************************************************
	// In-game deconstruction
	//******************************************************************************************************************
	def startDisposeFrom(invoker: EntityPlayer, tile: BlockPos): Unit = {
		if (!isAlive || invoker == null || !canMark(tile))
			return

		if (invoker.getGameProfile.getId != ownerId) {
			Chat.error(invoker, "error.not-an-owner", ownerName)
			return
		}

		setDisposalMark(tile, value = true)

		if (areMarksSet) {
			invalidate()
			Chat.warn(invoker, "warn.gateway-closed", worldObj.provider.getDimensionName, partnerWorld.provider.getDimensionName)
		}
	}

	def stopDisposeFrom(tile: BlockPos): Unit =
		if (isAlive)
			setDisposalMark(tile, value = false)

	//******************************************************************************************************************
	// Persistence
	//******************************************************************************************************************

	// Set of NBT field names
	private val PARTNER_POS_TAG = "partner-pos"
	private val OWNER_ID_TAG = "owner-id"
	private val OWNER_NAME_TAG = "owner-name"
	private val STORED_BLOCKS_TAG = "stored-blocks"

	override def readFromNBT(tag: NBTTagCompound): Unit = {
		super.readFromNBT(tag)

 		ownerId = tag.getUUID(OWNER_ID_TAG)
		val (pos, world) = tag.getBlockPos4D(PARTNER_POS_TAG)
		partnerPos = pos
		partnerWorld = world
		ownerName = tag.getString(OWNER_NAME_TAG)
		storedBlocks = tag.getBunchOfBlocks(STORED_BLOCKS_TAG)
	}

	override def writeToNBT(tag: NBTTagCompound): Unit = {
		super.writeToNBT(tag)

		tag.setUUID(OWNER_ID_TAG, ownerId)
		tag.setBlockPos4D(PARTNER_POS_TAG, partnerPos, partnerWorld)
		tag.setString(OWNER_NAME_TAG, ownerName)
		tag.setBunchOfBlocks(STORED_BLOCKS_TAG, storedBlocks)
	}

	//******************************************************************************************************************
	// Teleport helpers
	//******************************************************************************************************************

	private def getExitPos(entity: Entity) =
		translateCoordEnterToExit(getEntityThruBlockExit(entity, xCoord, yCoord, zCoord))
	// Transposes specified set of coordinates along vector specified by this and source TEs' coords
	private def translateCoordEnterToExit(coord: (Double, Double, Double)): (Double, Double, Double) =
		(coord._1 + partnerPos.x - xCoord, coord._2 + partnerPos.y - yCoord, coord._3 + partnerPos.z - zCoord)
	/** This function is used to calculate entity's position after moving through a block
	  *  Entity is considered to touch block at the start of move, and it's really necessary
	  *  for the computation to be correct. The move itself is like entity has moved in XZ plane
	  *  through block till it stops touching the one. The move vector is the entity's velocity.
	  *  If entity's XZ velocity is zero, then the vector from entity center to block center is taken
	  *  @param entity Entity to move
	  *  @param blockX X coord of pivot block
	  *  @param blockY Y coord of pivot block
	  *  @param blockZ Z coord of pivot block
	  *  @return       Exit point
	  */
	private def getEntityThruBlockExit(entity: Entity, blockX: Int, blockY: Int, blockZ: Int): (Double, Double, Double) =
	{
		val eps = 0.001
		val (x, z) = (entity.posX - blockX, entity.posZ - blockZ)
		// guard against zero velocity
		val (dx, dz) =
			if ( entity.motionX * entity.motionX + entity.motionZ * entity.motionZ > eps * eps) (entity.motionX, entity.motionZ)
			else (0.5 - x, 0.5 - z)
		val (x1, z1) = getEntityThruBlockExit(x, z, entity.width, dx, dz)
		(x1 + blockX, entity.posY, z1 + blockZ)
	}
	/** Searches for entity's suitable XZ exit position out of gateway
	  *  Assumes that gateway block is at (0, 0), so recalculate entity coordinates
	  *  No delta-check for XZ speed is performed
	  */
	private def getEntityThruBlockExit(x: Double, z: Double, width: Double, dx: Double, dz: Double): (Double, Double) = {
		// FPU calculation precision
		val eps = 0.001
		// Compute line equation from move vector
		val a = -dz
		val b = dx
		val c = x * dz - z * dx
		// Side coordinates for larger box, which edge would contain new entity center
		val collisionEps = 0.05
		val left = - (width / 2 + collisionEps)
		val right = -left + 1

		def findCoord1(coef1: Double, coef2: Double, coef3: Double, coord2: Double): Option[Double] =
			if (coef1.abs < eps) None // no sense in dealing with tiny coefficients
			else {
				val coord1 = - (coef2 * coord2 + coef3) / coef1
				if (left <= coord1 && coord1 <= right) Some(coord1)
				else None
			}

		def sameDir(x1: Double, z1: Double): Boolean = dx * (x1 - x) + dz * (z1 - z) > 0

		def pointFromX(x: Double): Option[(Double, Double)] =
			for (z <- findCoord1(b, a, c, x) if sameDir(x, z)) yield (x, z)

		def pointFromZ(z: Double): Option[(Double, Double)] =
			for (x <- findCoord1(a, b, c, z) if sameDir(x, z) ) yield (x, z)

		List(pointFromX(left), pointFromX(right), pointFromZ(left), pointFromZ(right))
			.flatten // get rid of inexistent points
			.head
	}
}
