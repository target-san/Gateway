package targetsan.mcmods.gateway

import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.util._
import net.minecraft.world.WorldServer

import Utils._
import gateway.api._
import net.minecraftforge.common.MinecraftForge

class TileGateway extends TileEntity {
	private val EmptyOwner = new java.util.UUID(0L, 0L)

	//******************************************************************************************************************
	// Gateway tile entity's state variables
	//******************************************************************************************************************

	/** Portal creator and owner's UUID */
	private var owner = EmptyOwner
	private var ownerName = ""
	private var exitPos: ChunkCoordinates = null
	private var exitWorld: WorldServer = null
	private var flags = 0

	//******************************************************************************************************************
	// Flags: disposal marks. When all 4 marks are set, gateway is removed from world, along with its peer
	//******************************************************************************************************************
	private val DisposeMarksMask = 0x0F

	private def sideToFlag(side: Int): Int = {
		if (!(0 to 3 contains side))
			throw new IllegalArgumentException(s"Disposal marks: side index $side is not in range [0..3]")
		1 << side
	}

	private def markSideDisposed(side: Int, mark: Boolean) =
		if (mark) flags |= sideToFlag(side)
		else flags &= ~sideToFlag(side)

	private def areAllSidesMarked = (flags & DisposeMarksMask) == DisposeMarksMask

	//******************************************************************************************************************
	// Flags: owner block's stored metadata; used to define exact block type
	//******************************************************************************************************************
	private val MetaMask = 0x0F
	private val MetaOffset = 4

	private def metadata = (flags >> MetaOffset) & MetaMask

	private def metadata_=(value: Int) = {
		val newValue = (value & MetaMask) << MetaOffset
		val mask = MetaMask << MetaOffset
		flags = (flags & ~mask) | newValue
	}

	//******************************************************************************************************************
	// Flags: multiblock state - not initialized, working or disposing
	//******************************************************************************************************************
	private object State extends Enumeration {
		type State = Value
		val PreInit, Alive, Disposing = Value
	}

	import State._

	private val StateMask = 0x03
	private val StateOffset = 8

	private def state: State = State((flags >> StateOffset) & StateMask)

	private def state_=(value: State) = {
		val newValue = (value.id & StateMask) << StateOffset
		val mask = StateMask << StateOffset
		flags = (flags & ~mask) | newValue
	}

	private def isMainState = !worldObj.isRemote && state == Alive

	//******************************************************************************************************************
	// Overrides
	//******************************************************************************************************************
	override def updateEntity(): Unit = {
		super.updateEntity()
		if (!isMainState)
			return

		if (teleportQueue.nonEmpty) // check only if we're gonna teleport something
			checkConnectionValid()
		for (e <- teleportQueue)
			teleport(e)
		teleportQueue = Nil
	}

	// A more unified way to notify multiblock that it's dead
	override def invalidate() {
		super.invalidate()
		if (!isMainState)
			return
		// Swithc to disposal cycle
		state = Disposing
		// Remove multiblock here
		GatewayMod.BlockGateway.cores(metadata).multiblock.disassemble(worldObj, xCoord, yCoord, zCoord)
		// This would trigger removal of the gateway's endpoint located on the other side
		exitWorld
			.getTileEntity(exitPos.posX, exitPos.posY, exitPos.posZ)
			.as[TileGateway]
			.foreach { _.invalidate() }
	}

	// NBT
	override def readFromNBT(tag: NBTTagCompound) {
		if (tag == null)
			return
		super.readFromNBT(tag)
		val pos = tag.getIntArray("exitPos")
		exitPos = new ChunkCoordinates(pos(0), pos(1), pos(2))
		exitWorld = Utils.world(pos(3))
		owner = java.util.UUID.fromString(tag.getString("owner"))
		ownerName = tag.getString("ownerName")
		flags = tag.getInteger("flags")
	}

	override def writeToNBT(tag: NBTTagCompound) {
		if (tag == null)
			return
		super.writeToNBT(tag)
		tag.setIntArray("exitPos", Array(exitPos.posX, exitPos.posY, exitPos.posZ, exitWorld.provider.dimensionId))
		tag.setString("owner", owner.toString)
		tag.setString("ownerName", ownerName)
		tag.setInteger("flags", flags)
	}

	//******************************************************************************************************************
	// Public API
	//******************************************************************************************************************

	/** Teleports entity
	 *
	 * @param entity the one being teleported
	 */
	def teleportEntity(entity: Entity) {
		if (!isMainState || entity == null || entity.timeUntilPortal > 0) // Performed only server-side, when entity has no cooldown on it
			return
		// Defer teleport till tile entity update
		val mount = Utils.bottomMount(entity)
		if (!(teleportQueue contains mount))
			teleportQueue :+= mount
	}

	def getExitPos = exitPos
	def getExitWorld = exitWorld

	def init(endpoint: TileGateway, player: EntityPlayer): Unit = {
		if (worldObj.isRemote)
			return
		if (state != PreInit) // owner and other params are set only once
			throw new IllegalStateException("Gateway parameters are set only once")

		state = Alive
		exitPos = new ChunkCoordinates(endpoint.xCoord, endpoint.yCoord, endpoint.zCoord)
		exitWorld = Utils.world(endpoint.worldObj.provider.dimensionId)
		owner = player.getGameProfile.getId
		ownerName = player.getGameProfile.getName
		// NB: assemble isn't used here. Because multiblock should be already constructed by the time TE is initialized
		markDirty()
		metadata = getBlockMetadata
	}

	def markForDispose(player: EntityPlayer, side: Int) {
		if (!isMainState)
			return

		if (player.getGameProfile.getId != owner) {
			player.addChatMessage(
				new ChatComponentText(s"Only the owner of this gateway, $ownerName, can severe it")
					.setChatStyle(new ChatStyle().setColor(EnumChatFormatting.YELLOW))
			)
			return
		}
		markSideDisposed(side, mark = true)
		if (!areAllSidesMarked)
			return

		invalidate()
		player.addChatMessage(
			new ChatComponentText(s"Gateway from ${worldObj.provider.getDimensionName} to ${exitWorld.provider.getDimensionName} was severed")
				.setChatStyle(new ChatStyle().setColor(EnumChatFormatting.YELLOW))
		)
	}

	def unmarkForDispose(side: Int) {
		if (isMainState)
			markSideDisposed(side, mark = false)
	}

	//******************************************************************************************************************
	// Private helpers
	//******************************************************************************************************************

	private var teleportQueue: List[Entity] = Nil

	/** Sends teleporting entity to peer tile
	 *
	 * @param entity The one being teleported
	 */
	private def teleport(entity: Entity): Unit = {
		val (ex, ey, ez) = getExitPos(entity)

		val enterEvent = new GatewayEnterEvent(
			entity,
			new ChunkCoordinates(xCoord, yCoord, zCoord),
			worldObj,
			new ChunkCoordinates(exitPos),
			exitWorld,
			Vec3.createVectorHelper(ex, ey, ez))

		if (MinecraftForge.EVENT_BUS.post(enterEvent))
			return

		val server = enterEvent.destWorld.as[WorldServer].getOrElse(Utils.world(enterEvent.destWorld.provider.dimensionId))
		val newEntity = EP3Teleporter.apply(entity, enterEvent.destPos.xCoord, enterEvent.destPos.yCoord, enterEvent.destPos.zCoord, server)
		if (newEntity == null)
			return

		setCooldown(newEntity)
		MinecraftForge.EVENT_BUS.post(
			new GatewayLeaveEvent(
				newEntity,
				new ChunkCoordinates(xCoord, yCoord, zCoord),
				worldObj,
				new ChunkCoordinates(exitPos),
				exitWorld)
		)
	}

	private def checkConnectionValid(): Unit = {
		exitWorld
			.getTileEntity(exitPos.posX, exitPos.posY, exitPos.posZ)
			.as[TileGateway]
			.getOrElse { throw new IllegalStateException("FATAL: no peer gateway found.") }
	}

	private def setCooldown(entity: Entity): Unit =
		for (e <- Utils.enumRiders(entity))
			if (e.timeUntilPortal < Utils.DefaultCooldown)
				e.timeUntilPortal = Utils.DefaultCooldown

	//******************************************************************************************************************
	// Teleporting: computes destination coordinates for the entity, based on enter and exit tiles
	// TODO: maybe move to separate object
	//******************************************************************************************************************
	def getExitPos(entity: Entity) =
		translateCoordEnterToExit(getEntityThruBlockExit(entity, xCoord, yCoord, zCoord))
	// Transposes specified set of coordinates along vector specified by this and source TEs' coords
	private def translateCoordEnterToExit(coord: (Double, Double, Double)): (Double, Double, Double) =
		(coord._1 + exitPos.posX - xCoord, coord._2 + exitPos.posY - yCoord, coord._3 + exitPos.posZ - zCoord)
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
