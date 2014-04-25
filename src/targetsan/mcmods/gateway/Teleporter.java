/**
 * This file originates from https://github.com/ShadedDimension/enhanced-portals/blob/c34864df76a95465c6f01383182c6c3a363b6acb/src/main/java/uk/co/shadeddimensions/ep3/portal/EntityManager.java
 * Represents ShadedDimension's entity teleportation algorithm
 * All side things like biometrics are dropped, it only moves _any_ entity to specified dimension
 * Mount/rider pairs also supported 
 */
package targetsan.mcmods.gateway;

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
import cpw.mods.fml.common.registry.GameRegistry;

public class Teleporter {
	public static void teleport(Entity entity, double x, double y, double z, int dim) {
		WorldServer destination = MinecraftServer.getServer().worldServerForDimension(dim);
		if (destination == null)
			return;
		transferEntityWithRider(entity, x, y, z, destination);
	}

	static Entity transferEntityWithRider(Entity entity, double x, double y, double z, WorldServer world) {
		Entity rider = entity.riddenByEntity;

		if (rider != null) {
			rider.mountEntity(null);
			rider = transferEntityWithRider(rider, x, y, z, world);
		}

		entity = transferEntity(entity, x, y, z, world);

		if (rider != null) {
			rider.mountEntity(entity);
		}

		return entity;
	}

	static Entity transferEntity(Entity entity, double x, double y, double z, WorldServer world) {
		return entity.worldObj.provider.dimensionId == world.provider.dimensionId
			? transferEntityWithinDimension(entity, x, y, z)
			: transferEntityToDimension(entity, x, y, z, (WorldServer) entity.worldObj, world);
	}

	static Entity transferEntityToDimension(Entity entity, double x, double y, double z, WorldServer exitingWorld, WorldServer enteringWorld) {
		if (entity == null)
			return null;

		if (entity instanceof EntityPlayer)
			return transferPlayerToDimension((EntityPlayerMP)entity, x, y, z, exitingWorld, enteringWorld);

		NBTTagCompound tag = new NBTTagCompound();
		entity.writeToNBTOptional(tag);

		int chunkX = entity.chunkCoordX;
		int chunkZ = entity.chunkCoordZ;

		if (entity.addedToChunk && exitingWorld.getChunkProvider().chunkExists(chunkX,
						chunkZ)) {
			exitingWorld.getChunkFromChunkCoords(chunkX, chunkZ)
					.removeEntity(entity);
		}

		exitingWorld.loadedEntityList.remove(entity);
		exitingWorld.onEntityRemoved(entity);

		Entity newEntity = EntityList.createEntityFromNBT(tag,
				enteringWorld);

		if (newEntity != null) {
			newEntity.setLocationAndAngles(x, y, z, entity.rotationYaw, entity.rotationPitch);
			newEntity.forceSpawn = true;
			enteringWorld.spawnEntityInWorld(newEntity);
			newEntity.setWorld(enteringWorld);
		}

		exitingWorld.resetUpdateEntityTick();
		enteringWorld.resetUpdateEntityTick();

		return newEntity;
	}

	static Entity transferPlayerToDimension(EntityPlayerMP player, double x, double y, double z, WorldServer exitingWorld, WorldServer enteringWorld) {
		MinecraftServer server = player.mcServer;
		ServerConfigurationManager config = server.getConfigurationManager();

		player.closeScreen();
		player.dimension = enteringWorld.provider.dimensionId;
		player.playerNetServerHandler
				.sendPacketToPlayer(new Packet9Respawn(player.dimension,
						(byte) player.worldObj.difficultySetting,
						enteringWorld.getWorldInfo().getTerrainType(),
						enteringWorld.getHeight(),
						player.theItemInWorldManager.getGameType()));

		exitingWorld.removePlayerEntityDangerously(player);
		player.isDead = false;
		player.setLocationAndAngles(x, y, z, player.rotationYaw, player.rotationPitch);

		enteringWorld.spawnEntityInWorld(player);
		player.setWorld(enteringWorld);

		config.func_72375_a(player, exitingWorld);
		player.playerNetServerHandler.setPlayerLocation(x, y, z, player.rotationYaw, player.rotationPitch);
		player.theItemInWorldManager.setWorld(enteringWorld);

		config.updateTimeAndWeatherForPlayer(player, enteringWorld);
		config.syncPlayerInventory(player);

		Iterator potion = player.getActivePotionEffects().iterator();
		while (potion.hasNext()) {
			player.playerNetServerHandler
					.sendPacketToPlayer(new Packet41EntityEffect(
							player.entityId, (PotionEffect) potion.next()));
		}

		player.playerNetServerHandler
				.sendPacketToPlayer(new Packet43Experience(
						player.experience, player.experienceTotal,
						player.experienceLevel));
		GameRegistry.onPlayerChangedDimension(player);

		return player;
	}
	
	static Entity transferEntityWithinDimension(Entity entity, double x, double y, double z) {
		if (entity == null)
			return null;

		if (entity instanceof EntityPlayer) {
			EntityPlayerMP player = (EntityPlayerMP) entity;
			player.setPositionAndUpdate(x, y, z);
			player.worldObj.updateEntityWithOptionalForce(player, false);
			return player;
		}

		WorldServer world = (WorldServer) entity.worldObj;
		NBTTagCompound tag = new NBTTagCompound();
		entity.writeToNBTOptional(tag);

		int chunkX = entity.chunkCoordX;
		int chunkZ = entity.chunkCoordZ;

		if (entity.addedToChunk && world.getChunkProvider().chunkExists(chunkX, chunkZ))
			world.getChunkFromChunkCoords(chunkX, chunkZ).removeEntity(entity);

		world.loadedEntityList.remove(entity);
		world.onEntityRemoved(entity);

		Entity newEntity = EntityList.createEntityFromNBT(tag, world);

		if (newEntity != null) {
			newEntity.setLocationAndAngles(x, y, z, entity.rotationYaw, entity.rotationPitch);
			newEntity.forceSpawn = true;
			world.spawnEntityInWorld(newEntity);
			newEntity.setWorld(world);
		}

		world.resetUpdateEntityTick();
		return newEntity;
	}

}
