package targetsan.mcmods.gateway

import net.minecraft.entity.Entity
import net.minecraft.server.MinecraftServer
import net.minecraft.world.WorldServer
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.entity.EntityList
import net.minecraft.network.play.server.S07PacketRespawn
import scala.collection.JavaConversions._
import net.minecraft.network.play.server.S1DPacketEntityEffect
import net.minecraft.potion.PotionEffect
import cpw.mods.fml.common.FMLCommonHandler

object Teleport
{
	// Teleport helpers
	def apply(entity: Entity, x: Double, y: Double, z: Double, dim: Int): Entity =
	{
		val to = Utils.world(dim)
		val (dx, dy, dz) = (x - entity.posX, y - entity.posY, z - entity.posZ)
		// check for rider and unmount
		val rider = if (entity.riddenByEntity != null) entity.riddenByEntity else null
		if (rider != null)
			rider.mountEntity(null)
		// teleport
		val newEntity = teleport(entity, x, y, z, to)
		// teleport rider, if any, and schedule remount
		if (rider != null)
		{
			val newRider = teleport(rider, rider.posX + dx, rider.posY + dy, rider.posZ + dz, to)
			newRider.mountEntity(newEntity)
		}
		to.updateEntity(newEntity)
		newEntity
	}
	
	private def teleport(entity: Entity, x: Double, y: Double, z: Double, to: WorldServer): Entity =
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
}
