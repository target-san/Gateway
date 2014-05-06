/**
 * This file originates from https://github.com/ShadedDimension/enhanced-portals/blob/c34864df76a95465c6f01383182c6c3a363b6acb/src/main/java/uk/co/shadeddimensions/ep3/portal/EntityManager.java
 * Represents ShadedDimension's entity teleportation algorithm
 * All side things like biometrics are dropped, it only moves _any_ entity to specified dimension
 * Mount/rider pairs also supported 
 */
package targetsan.mcmods.gateway;

import java.util.Arrays;
import java.util.Iterator;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.packet.Packet41EntityEffect;
import net.minecraft.network.packet.Packet43Experience;
import net.minecraft.network.packet.Packet9Respawn;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import cpw.mods.fml.common.registry.GameRegistry;

public class Teleporter {
	public static Entity teleport(Entity entity, double x, double y, double z, int dim) {
		WorldServer destination = MinecraftServer.getServer().worldServerForDimension(dim);
		if (destination == null)
			return entity;
		return transferEntityWithRider(entity, x, y, z, destination);
	}

	private static Entity transferEntityWithRider(Entity entity, double x, double y, double z, WorldServer world) {
		if (entity == null || entity.isDead)
			return null;
		
		if (entity.ridingEntity != null)
			return transferEntityWithRider(entity.ridingEntity, x, y, z, world);

		Entity rider = entity.riddenByEntity;

		if (rider != null) {
			rider.mountEntity(null);
		}

		entity = transferEntity(entity, x, y, z, world);

		if (rider != null) {
			rider = transferEntityWithRider(rider, x, y, z, world);
			entity.worldObj.updateEntityWithOptionalForce(entity, true);
			rider.mountEntity(entity);
		}

		return entity;
	}

	private static Entity transferEntity(Entity entity, double x, double y, double z, WorldServer world) {
		if (entity.worldObj.provider.dimensionId != world.provider.dimensionId)
			entity = moveToDimension(entity, world);
		if (entity == null) return null;
		entity = moveWithinDimension(entity, x, y, z);
		if (entity == null) return null;
		entity.worldObj.updateEntityWithOptionalForce(entity, false);
		
		return entity;
	}
	
	private static Entity moveToDimension(Entity entity, WorldServer toWorld) {
		return entity instanceof EntityPlayer
			? movePlayerToDimension((EntityPlayerMP)entity, toWorld)
			: moveEntityToDimension(entity, toWorld);
	}

	private static Entity moveEntityToDimension(Entity entity, WorldServer toWorld) {
		NBTTagCompound tag = new NBTTagCompound();
		entity.writeToNBTOptional(tag);
		
		WorldServer fromWorld = (WorldServer)entity.worldObj;
		removeFromChunk(entity);
		fromWorld.onEntityRemoved(entity);
		entity.isDead = true;
		
		entity = EntityList.createEntityFromNBT(tag, toWorld);
		
		if (entity == null)
			return null;
		
		entity.forceSpawn = true;
		toWorld.spawnEntityInWorld(entity);
		entity.setWorld(toWorld);
		
		return entity;
	}

	private static Entity movePlayerToDimension(EntityPlayerMP player, WorldServer toWorld) {
		MinecraftServer server = player.mcServer;
		ServerConfigurationManager config = server.getConfigurationManager();
		WorldServer fromWorld = (WorldServer) player.worldObj;

		player.closeScreen();
		player.dimension = toWorld.provider.dimensionId;
		player.playerNetServerHandler
			.sendPacketToPlayer(new Packet9Respawn(player.dimension,
				(byte) player.worldObj.difficultySetting,
				toWorld.getWorldInfo().getTerrainType(),
				toWorld.getHeight(),
				player.theItemInWorldManager.getGameType()
			));

		// removePlayerEntityDangerously cannot be used here -
		// it would break loadedEntitiesList and thus unapplicable inside updateEntities
		removeFromChunk(player);
		fromWorld.removeEntity(player);
		fromWorld.unloadEntities(Arrays.asList(player));
		player.isDead = false;
		fromWorld.getPlayerManager().removePlayer(player);

		toWorld.getPlayerManager().addPlayer(player);
		toWorld.spawnEntityInWorld(player);
		player.setWorld(toWorld);
		player.theItemInWorldManager.setWorld(toWorld);

		config.updateTimeAndWeatherForPlayer(player, toWorld);
		config.syncPlayerInventory(player);

		for (Object potion: player.getActivePotionEffects())
			player
				.playerNetServerHandler
				.sendPacketToPlayer(
					new Packet41EntityEffect(player.entityId, (PotionEffect) potion)
				);

		player.playerNetServerHandler
				.sendPacketToPlayer(new Packet43Experience(
						player.experience, player.experienceTotal,
						player.experienceLevel));
		GameRegistry.onPlayerChangedDimension(player);

		return player;
	}
	
	private static Entity moveWithinDimension(Entity entity, double x, double y, double z) {
		if (entity instanceof EntityPlayer)
			((EntityPlayerMP)entity).setPositionAndUpdate(x, y, z);
		else
			entity.setLocationAndAngles(x, y, z, entity.rotationYaw, entity.rotationPitch);
		
		return entity;
	}
	
	private static void removeFromChunk(Entity entity) {
		int chunkX = entity.chunkCoordX;
		int chunkZ = entity.chunkCoordZ;
		WorldServer world = (WorldServer)entity.worldObj;

		if (entity.addedToChunk && world.getChunkProvider().chunkExists(chunkX, chunkZ)) {
			Chunk chunk = world.getChunkFromChunkCoords(chunkX, chunkZ);
			chunk.removeEntity(entity);
			chunk.isModified = true;
		}
	}
}
