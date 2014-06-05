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
	// This list is supposedly processed the next tick after fill, so should be stored in NBT
	// First element in pair is rider, the second is mount
	private var remountQueue: List[(Entity, Entity)] = Nil
		
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
	    
	    teleportQueue +:= entity
	}
	
	private def enqueueRemount(rider: Entity, mount: Entity)
	{
		remountQueue +:= (rider, mount)
	}
	
	// Update func
	override def updateEntity
	{
		// Process teleportation queue, comes from this dimension
		teleportQueue.foreach(teleportAny(_))
		teleportQueue = Nil
		// Process remount queue, comes from another dimension
		remountQueue.foreach { case (rider, mount) => rider.mountEntity(mount) }
		remountQueue = Nil
	}
	
	override def canUpdate = !worldObj.isRemote // updates occur only server-side
	
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
		val remounts = tag.getIntArray("remounts")
		if (remounts != null)
			remountQueue = List
				.fromArray(remounts)
				.map(worldObj.getEntityByID(_))
				.grouped(2)
				.filter(_.length == 2)
				.map( { case a :: b :: Nil => (a, b) } )
				.toList 
	}
	
	override def writeToNBT(tag: NBTTagCompound)
	{
		if (tag == null)
			return
		super.writeToNBT(tag)
		tag.setIntArray("exitPos", Array(exitX, exitY, exitZ, exitDim.provider.dimensionId))
		tag.setString("owner", owner)
		tag.setIntArray("remounts",
			remountQueue.flatMap(x => List(x._1.getEntityId, x._2.getEntityId) ).toArray
		)
	}
	
	// Teleport helpers
	private def teleportAny(entity: Entity)
	{
		
	}
	// Teleports only non-player entities; applicable for single entities only
	private def teleportEntity(entity: Entity, x: Double, y: Double, z: Double, to: WorldServer): Entity =
	{
		val from = Utils.world(entity.worldObj.provider.dimensionId)
		val tag = new NBTTagCompound
		entity.writeToNBT(tag)
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
}
