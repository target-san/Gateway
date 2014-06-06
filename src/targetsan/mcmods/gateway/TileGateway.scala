package targetsan.mcmods.gateway

import net.minecraft.tileentity.TileEntity
import net.minecraft.entity.Entity
import net.minecraft.world.World
import net.minecraft.server.MinecraftServer
import net.minecraft.world.IBlockAccess
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.world.WorldServer
import net.minecraft.entity.EntityList
import cpw.mods.fml.common.FMLCommonHandler
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.network.play.server.S07PacketRespawn
import scala.collection.JavaConversions._
import net.minecraft.network.play.server.S1DPacketEntityEffect
import net.minecraft.potion.PotionEffect

class TileGateway extends TileEntity
{
	private val PILLAR_HEIGHT = 3
	
	private var exitX = 0
	private var exitY = 0
	private var exitZ = 0
	private var exitDim: WorldServer = null
	private var owner = ""
	// This list is processed the same tick it's initialized, so it shouldn't be stored in NBT
	private var teleportQueue: List[Entity] = Nil
		
	def init(x: Int, y: Int, z: Int, player: EntityPlayer)
	{
		if (worldObj.isRemote)
			return
		// This init can be called only from non-gateway dimension
		if (worldObj.provider.dimensionId == Gateway.DIMENSION_ID)
			throw new IllegalStateException("Tile cannot be initialized in such a way from Nether")
		initBase(x, y, z, player)
		exitDim = Gateway.dimension
		// When gateway tile is properly initialized, we construct exitpoint on the other side
		val gateworld = Gateway.dimension
		gateworld.setBlock(x, y, z, GatewayMod.BlockGatewayBase)
		gateworld.getTileEntity(x, y, z).asInstanceOf[TileGateway].init(xCoord, yCoord, zCoord, worldObj.provider.dimensionId, player)
	}
    
	private def init(x: Int, y: Int, z: Int, dim: Int, player: EntityPlayer)
	{
		if (worldObj.provider.dimensionId != Gateway.DIMENSION_ID)
			throw new IllegalStateException("Tile can be initialized in such a way only from Nether")
		initBase(x, y, z, player)
		exitDim = Utils.world(dim)
	}
	
	private def initBase(ex: Int, ey: Int, ez: Int, player: EntityPlayer)
	{
		if (!owner.isEmpty()) // owner and other params are set only once
			throw new IllegalStateException("Gateway parameters are set only once")
		exitX = ex
		exitY = ey
		exitZ = ez
		owner = player.getGameProfile().getId()
		worldObj.markTileEntityChunkModified(xCoord, yCoord, zCoord, this)
	}
    
	def dispose()
	{
		if (worldObj.isRemote)
			return
		// This would trigger removal of the gateway's endpoint located in Nether
		if (worldObj.provider.dimensionId != Gateway.DIMENSION_ID)
			Gateway.dimension.setBlock(exitX, exitY, exitZ, Blocks.stone)
			
		owner = null
		exitX = 0
		exitY = 0
		exitZ = 0
		exitDim = null
	}
	
	def teleportEntity(entity: Entity)
	{
	    if (worldObj.isRemote || entity == null) // Performed only server-side
	    	return
	    checkGatewayValid
	    val scheduled = getBottomMount(entity) // avoid multi-port on riders/mounts
	    if (!teleportQueue.contains(scheduled))
	    	teleportQueue +:= scheduled
	}
	
	private def getBottomMount(entity: Entity): Entity = 
		if (entity.ridingEntity != null) getBottomMount(entity.ridingEntity)
		else entity
	
	private def checkGatewayValid
	{
		if (owner == null || owner.isEmpty)
			throw new IllegalStateException("Gateway not initialized properly: owner isn't set")
		if (exitDim == null)
			throw new IllegalStateException("Gateway not initialized properly: exit dimension reference is NULL")
		if (!exitDim.getTileEntity(exitX, exitY, exitZ).isInstanceOf[TileGateway])
			throw new IllegalStateException("Gateway not constructed properly: there's no gateway exit on the other side")
	}
	
	// Update func
	override def updateEntity
	{
		if (worldObj.isRemote)
			return
		// Process teleportation queue, comes from this dimension
		teleportQueue.foreach(teleportAny(_))
		teleportQueue = Nil
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
		exitDim = Utils.world(pos(3))
		owner = tag.getString("owner")
	}
	
	override def writeToNBT(tag: NBTTagCompound)
	{
		if (tag == null)
			return
		super.writeToNBT(tag)
		tag.setIntArray("exitPos", Array(exitX, exitY, exitZ, exitDim.provider.dimensionId))
		tag.setString("owner", owner)
	}
	
	// Teleport helpers
	private def teleportAny(entity: Entity): Entity =
	{
		// check for rider and unmount
		val rider = if (entity.riddenByEntity != null) entity.riddenByEntity else null
		if (rider != null)
			rider.mountEntity(null)
		// teleport
		val newEntity = doTeleport(entity)
		// teleport rider, if any, and schedule remount
		if (rider != null)
		{
			val newRider = doTeleport(rider)
			newRider.mountEntity(newEntity)
		}
		exitDim.updateEntity(newEntity)
		newEntity
	}
	
	private def doTeleport(entity: Entity) = doTeleportAny(entity, getExitPos(entity), exitDim)
		
	// Selects between player and non-player teleport
	private def doTeleportAny(entity: Entity, exit: (Double, Double, Double), to: WorldServer): Entity =
		doTeleportAny(entity, exit._1, exit._2, exit._3, to)

	private def doTeleportAny(entity: Entity, x: Double, y: Double, z: Double, to: WorldServer): Entity =
		if (entity.isInstanceOf[EntityPlayer]) teleportPlayer(entity.asInstanceOf[EntityPlayerMP], x, y, z, to)
		else                                   teleportEntity(entity, x, y, z, to)
	// Teleports only non-player entities; applicable for single entities only
	private def teleportEntity(entity: Entity, x: Double, y: Double, z: Double, to: WorldServer): Entity =
	{
		val from = Utils.world(entity.worldObj.provider.dimensionId)
		val tag = new NBTTagCompound
		entity.writeMountToNBT(tag)
		from.removeEntity(entity)
		
		val newEntity = EntityList.createEntityFromNBT(tag, to)
		newEntity.setLocationAndAngles(x, y, z, newEntity.rotationYaw, newEntity.rotationPitch)
		newEntity.forceSpawn = true
		to.spawnEntityInWorld(newEntity)
		
		from.resetUpdateEntityTick()
		to.resetUpdateEntityTick()
		
		newEntity
	}
	// Cleaned up duplicate of ServerConfigurationManager.transferPlayerToDimension
	// Differs by not using Teleporter, and thus not susceptible to The End dimension speciality
	// Like TeleportEntity, doesn't care about mount/rider status
	private def teleportPlayer(player: EntityPlayerMP, x: Double, y: Double, z: Double, to: WorldServer): Entity =
	{
		/* Note: Yeah, this code is exactly THAT bad.
		 * It's an almost-direct clone of teleport ServerConfigurationManager.transferPlayerToDimension
		 * You're welcome to devise a more elegant way of player's teleportation
		 */
        val oldDim = player.dimension
        val from = Utils.world(player.dimension)
        player.dimension = to.provider.dimensionId
        // remove from old world
        player.playerNetServerHandler.sendPacket(
        	new S07PacketRespawn(
        		player.dimension,
        		player.worldObj.difficultySetting,
        		player.worldObj.getWorldInfo().getTerrainType(),
        		player.theItemInWorldManager.getGameType())
        )
        from.removePlayerEntityDangerously(player)
        player.isDead = false
        // relocation
        player.setPositionAndUpdate(x, y, z)
        // Transfer between worlds
        from.getPlayerManager().removePlayer(player)
        to.getPlayerManager().addPlayer(player)
        to.theChunkProviderServer.loadChunk(player.posX.toInt >> 4, player.posZ.toInt >> 4)
        // Install into new world
        player.setPositionAndUpdate(player.posX, player.posY, player.posZ)
        to.spawnEntityInWorld(player)
        //to.updateEntityWithOptionalForce(player, false)
        player.setWorld(to)
        player.theItemInWorldManager.setWorld(to)
        // Some updates
        val confman = player.mcServer.getConfigurationManager()
        confman.updateTimeAndWeatherForPlayer(player, to)
        confman.syncPlayerInventory(player)
        // Sync potion effects
        for (effect <- player.getActivePotionEffects())
        	player.playerNetServerHandler.sendPacket(new S1DPacketEntityEffect(player.getEntityId(), effect.asInstanceOf[PotionEffect]))
        	
        FMLCommonHandler.instance().firePlayerChangedDimensionEvent(player, oldDim, player.dimension);
		
		player
	}

	private def getExitPos(entity: Entity) = translateCoordEnterToExit(getEntityThruBlockExit(entity, xCoord, yCoord, zCoord))
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
	// Transposes specified set of coordinates along vector specified by this TE's coords and exit block's coords
	private def translateCoordEnterToExit(coord: (Double, Double, Double)): (Double, Double, Double) =
		(coord._1 + exitX - xCoord, coord._2 + exitY - yCoord, coord._3 + exitZ - zCoord)
}
