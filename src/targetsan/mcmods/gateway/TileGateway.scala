package targetsan.mcmods.gateway

import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.ChatComponentText
import net.minecraft.util.ChatStyle
import net.minecraft.util.ChunkCoordinates
import net.minecraft.util.EnumChatFormatting
import net.minecraft.world.World
import net.minecraft.world.WorldServer

class TileGateway extends TileEntity
{
	private val EmptyOwner = new java.util.UUID(0L, 0L)
	
	private var exitX = 0
	private var exitY = 0
	private var exitZ = 0
	private var exitDim: Int = 0
	private val exitWorld = new Cached( () => Utils.world(exitDim) )
	private var owner = EmptyOwner
	private var ownerName = ""
	private var flags = 0
	
	/** A set of methods which works with sides markup 
	 *  Implements conventional way to remove gateways, which are effectively indestructible
	 */
	private val DisposeMarksMask = 0x0F

	private def sideToFlag(side: Int): Int = 
	{
		if (! (0 to 3 contains side))
			throw new IllegalArgumentException(s"Disposal marks: side index $side is not in range [0..3]")
		1 << side
	}
	
	private def markSideDisposed(side: Int, mark: Boolean) =
		if (mark) flags |= sideToFlag(side)
		else flags &= ~sideToFlag(side)
		
	private def areAllSidesMarked = (flags & DisposeMarksMask) == DisposeMarksMask
	
	/** Accessor funcs which get/set stored block metadata.
	 *  This is due to block's meta might be cleaned when TE is invalidated by block removal. I'm not sure.
	 *  Might be dropped in future
	 */
	private val MetaMask = 0x0F
	private val MetaOffset = 4
	
	private def metadata               = (flags >> MetaOffset) & MetaMask
	private def metadata_=(value: Int) =
	{
		val newValue = (value & MetaMask) << MetaOffset
		val mask = MetaMask << MetaOffset
		flags = (flags & ~mask) | newValue
	}
	/** Flag accessors for controlling multiblock's state. Would possibly prevent some strange things. I'm not sure.
	 */
	private object State extends Enumeration
	{
		type State = Value
		val PreInit, Alive, Disposing = Value
	}
	import State._
	
	private val StateMask = 0x03
	private val StateOffset = 8

	private def state: State          = State((flags >> StateOffset) & StateMask)
	private def state_=(value: State) =
	{
		val newValue = (value.id & StateMask) << StateOffset
		val mask = StateMask << StateOffset
		flags = (flags & ~mask) | newValue
	}
	
	def getEndPoint = new ChunkCoordinates(exitX, exitY, exitZ)
	def getEndWorld = exitWorld.value
	
	def init(endpoint: TileGateway, player: EntityPlayer): Unit =
	{
		if (worldObj.isRemote)
			return
		if (state != PreInit) // owner and other params are set only once
			throw new IllegalStateException("Gateway parameters are set only once")

		state = Alive
		exitX = endpoint.xCoord
		exitY = endpoint.yCoord
		exitZ = endpoint.zCoord
		owner = player.getGameProfile().getId()
		ownerName = player.getGameProfile().getName()
		exitDim = endpoint.worldObj.provider.dimensionId
		exitWorld.reset
		// NB: assemble isn't used here. Because multiblock should be already constructed by the time TE is initialized
		markDirty();
		metadata = getBlockMetadata()
	}
    
	def teleportEntity(entity: Entity)
	{
	    if (worldObj.isRemote || entity == null || entity.timeUntilPortal > 0) // Performed only server-side, when entity has no cooldown on it
	    	return
	    checkGatewayValid
	    scheduleTeleport(entity)
	}
	
	private var teleportQueue: List[Entity] = Nil
	
	private def scheduleTeleport(entity: Entity) =
	{
		val mount = getBottomMount(entity)
		if (!(teleportQueue contains mount))
			teleportQueue :+= mount
	}
	
	override def updateEntity(): Unit =
	{
		if (worldObj.isRemote)
			return
			
		for (e <- teleportQueue)
		{
			val exit = getExitPos(e)
			val newEntity = EP3Teleporter.apply(e, exit._1, exit._2, exit._3, exitWorld.value.asInstanceOf[WorldServer])
			if (newEntity != null)
				setCooldown(newEntity)
		}
		teleportQueue = Nil
	}
	
	def markForDispose(player: EntityPlayer, side: Int)
	{
		if (worldObj.isRemote)
			return
		
		if (player.getGameProfile().getId() != owner)
		{
			player.addChatMessage(
				new ChatComponentText(s"Only the owner of this gateway, $ownerName, can severe it")
				.setChatStyle(new ChatStyle().setColor(EnumChatFormatting.YELLOW))
			)
			return
		}
		markSideDisposed(side, true)
		if (!areAllSidesMarked)
			return

		invalidate()
		player.addChatMessage(
			new ChatComponentText(s"Gateway from ${worldObj.provider.getDimensionName} to ${exitWorld.value.provider.getDimensionName} was severed")
			.setChatStyle(new ChatStyle().setColor(EnumChatFormatting.YELLOW))
		)
	}
	
	def unmarkForDispose(side: Int)
	{
		if (worldObj.isRemote)
			return
		markSideDisposed(side, false)
	}
	
	// A more unified way to notify multiblock that it's dead
	override def invalidate
	{
		super.invalidate
		dispose
	}
	
	// NBT
	override def readFromNBT(tag: NBTTagCompound)
	{
		if (tag == null)
			return
		super.readFromNBT(tag)
		val pos = tag.getIntArray("exitPos")
		exitX = pos(0)
		exitY = pos(1)
		exitZ = pos(2)
		exitDim = pos(3)
		exitWorld.reset
		owner = java.util.UUID.fromString(tag.getString("owner"))
		ownerName = tag.getString("ownerName")
		flags = tag.getInteger("flags")
	}
	
	override def writeToNBT(tag: NBTTagCompound)
	{
		if (tag == null)
			return
		super.writeToNBT(tag)
		tag.setIntArray("exitPos", Array(exitX, exitY, exitZ, exitDim))
		tag.setString("owner", owner.toString)
		tag.setString("ownerName", ownerName)
		tag.setInteger("flags", flags)
	}
	
	private def dispose()
	{
		if (worldObj.isRemote || state != Alive)
			return
		
		state = Disposing
		// Remove multiblock here
		GatewayMod.BlockGateway.cores(metadata).multiblock.disassemble(worldObj, xCoord, yCoord, zCoord)
		// This would trigger removal of the gateway's endpoint located on the other side
		val exitTE = exitWorld.value.getTileEntity(exitX, exitY, exitZ)
		if (exitTE != null && exitTE.isInstanceOf[TileGateway])
			exitTE.invalidate()
	}
	
	private def getBottomMount(entity: Entity): Entity = 
		if (entity.ridingEntity != null) getBottomMount(entity.ridingEntity)
		else entity
	
	private def checkGatewayValid // FIXME: do something with this, I suppose?...
	{
		if (owner == null || owner == EmptyOwner)
			throw new IllegalStateException("Gateway not initialized properly: owner isn't set")
		if (exitDim == null)
			throw new IllegalStateException("Gateway not initialized properly: exit dimension reference is NULL")
		if (!exitWorld.value.getTileEntity(exitX, exitY, exitZ).isInstanceOf[TileGateway])
			throw new IllegalStateException("Gateway not constructed properly: there's no gateway exit on the other side")
	}
	
	private def setCooldown(entity: Entity): Unit =
	{
		if (entity.riddenByEntity != null)
			setCooldown(entity.riddenByEntity)
		if (entity.timeUntilPortal < Utils.DefaultCooldown)
			entity.timeUntilPortal = Utils.DefaultCooldown
	}
	
	private def getExitPos(entity: Entity) = translateCoordEnterToExit(getEntityThruBlockExit(entity, xCoord, yCoord, zCoord))
	// Transposes specified set of coordinates along vector specified by this TE's coords and exit block's coords
	private def translateCoordEnterToExit(coord: (Double, Double, Double)): (Double, Double, Double) =
		(coord._1 + exitX - xCoord, coord._2 + exitY - yCoord, coord._3 + exitZ - zCoord)
	/** This function is used to calculate entity's position after moving through a block
	 *  Entity is considered to touch block at the start of move, and it's really necessary
	 *  for the computation to be correct. The move itself is like entity has moved in XZ plane
	 *  through block till it stops touching the one. The move vector is the entity's velocity.
	 *  If entity's XZ velocity is zero, then the vector from entity center to block center is taken
	 *  @param entity Entity to move
	 *  @param block  Block which entity must move through
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
