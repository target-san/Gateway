package targetsan.mcmods.gateway;

import cpw.mods.fml.common.FMLCommonHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.S05PacketSpawnPosition;
import net.minecraft.network.play.server.S07PacketRespawn;
import net.minecraft.network.play.server.S1DPacketEntityEffect;
import net.minecraft.network.play.server.S1FPacketSetExperience;
import net.minecraft.network.play.server.S2BPacketChangeGameState;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ItemInWorldManager;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.demo.DemoWorldManager;

/**
 * This is a stripped down {@link enhancedportals.portal.EntityManager} Many
 * thanks to EnhancedPortals3 team for this code Only teleportation code is left
 * See original at {@link https://github.com/SkyNetAB/enhanced-portals/blob/9d01
 * c643c0115a9eee0248ce6e6c850b2b7a6066
 * /src/main/java/enhancedportals/portal/EntityManager.java}
 */
public class EP3Teleporter {
	public static Entity apply(Entity entity, double x, double y, double z, WorldServer world)
	{
		if (entity == null)
			return null;
		if (entity.worldObj.isRemote)
			return entity;
		// locate bottom-most entity and recalculate coordinates accordingly
		Entity teleportTarget = getBottomMount(entity);
		double
			destX = x + teleportTarget.posX - entity.posX,
			destY = y + teleportTarget.posY - entity.posY,
			destZ = z + teleportTarget.posZ - entity.posZ;
		return transferEntityWithRider(teleportTarget, destX, destY, destZ, world);
	}
	
	private static Entity getBottomMount(Entity entity)
	{
		return entity.ridingEntity != null
			? getBottomMount(entity.ridingEntity)
			: entity;
	}
	
	private static Entity transferEntityWithRider(Entity entity, double x, double y, double z, WorldServer world)
	{
		Entity rider = entity.riddenByEntity;

		// If Entity has a rider...
		if (rider != null) {
			// Unmount rider
			rider.mountEntity(null);
			// Calculate rider's position relative to mount
			double
				riderX = rider.posX - entity.posX,
				riderY = rider.posY - entity.posY,
				riderZ = rider.posZ - entity.posZ;
			// Send it back through as it's own entity
			rider = transferEntityWithRider(rider, x + riderX, y + riderY, z + riderZ, world);
		}

		// Transfer the entity.
		entity = transferEntity(entity, x, y, z, world);

		// Remount entity with rider.
		if (rider != null) {
			rider.mountEntity(entity);
		}

		return entity;
	}

	private static Entity transferEntity(Entity entity, double x, double y, double z, WorldServer world)
	{
		// If entity is going to the same dimension...
		if (entity.worldObj.provider.dimensionId == world.provider.dimensionId)
			return transferEntityWithinDimension(entity, x, y, z);
		// Otherwise send it to another dimension...
		else
			return transferEntityToDimension(entity, x, y, z, (WorldServer) entity.worldObj, world);
	}

	private static Entity transferEntityToDimension(Entity entity, double x, double y, double z, WorldServer exitingWorld, WorldServer enteringWorld)
	{
		// If the entity teleporting is a player:
		if (entity instanceof EntityPlayer) {
			EntityPlayerMP player = (EntityPlayerMP) entity;
			MinecraftServer server = player.mcServer;
			ServerConfigurationManager config = server
					.getConfigurationManager();

			player.closeScreen();
			player.dimension = enteringWorld.provider.dimensionId;
			player.playerNetServerHandler.sendPacket(new S07PacketRespawn(
					player.dimension, player.worldObj.difficultySetting,
					enteringWorld.getWorldInfo().getTerrainType(),
					player.theItemInWorldManager.getGameType()));

			exitingWorld.removePlayerEntityDangerously(player);
			player.isDead = false;
			player.setLocationAndAngles(x, y, z, player.rotationYaw,
					player.rotationPitch);
			player.velocityChanged = true;

			enteringWorld.spawnEntityInWorld(player);
			player.setWorld(enteringWorld);

			config.func_72375_a(player, exitingWorld);
			player.playerNetServerHandler.setPlayerLocation(x, y, z,
					player.rotationYaw, entity.rotationPitch);
			player.theItemInWorldManager.setWorld(enteringWorld);

			config.updateTimeAndWeatherForPlayer(player, enteringWorld);
			config.syncPlayerInventory(player);

			// Instate any potion effects the player had when teleported.
			for (Object potion : player.getActivePotionEffects())
				player.playerNetServerHandler
				.sendPacket(new S1DPacketEntityEffect(player
						.getEntityId(), (PotionEffect) potion));

			// If there is instability, give effects.
			player.playerNetServerHandler
			.sendPacket(new S1FPacketSetExperience(player.experience,
					player.experienceTotal, player.experienceLevel));
			FMLCommonHandler.instance().firePlayerChangedDimensionEvent(player, exitingWorld.provider.dimensionId, enteringWorld.provider.dimensionId);
			// GameRegistry.onPlayerChangedDimension(player); // TODO

			return player;
		}
		// If the entity teleporting is something other than a player:
		else {
			NBTTagCompound tag = new NBTTagCompound();
			entity.writeToNBTOptional(tag);

			// Delete the entity. Will be taken care of next tick.
			entity.setDead();

			// Create new entity.
			Entity newEntity = EntityList.createEntityFromNBT(tag,
					enteringWorld);

			// Set position, momentum of new entity at the other portal.
			if (newEntity != null) {
				newEntity.velocityChanged = true;
				newEntity.setLocationAndAngles(x, y, z, entity.rotationYaw,
						entity.rotationPitch);
				newEntity.forceSpawn = true;
				enteringWorld.spawnEntityInWorld(newEntity);
				newEntity.setWorld(enteringWorld);
			}

			exitingWorld.resetUpdateEntityTick();
			enteringWorld.resetUpdateEntityTick();

			return newEntity;
		}
	}

	private static Entity transferEntityWithinDimension(Entity entity,
			double x, double y, double z) {
		// If the entity teleporting is a player:
		if (entity instanceof EntityPlayer) {
			EntityPlayerMP player = (EntityPlayerMP) entity;
			// The actual transporting.
			player.setPositionAndUpdate(x, y, z);
			// For the momentum module.
			player.velocityChanged = true;
			player.worldObj.updateEntityWithOptionalForce(player, false);

			// If there is instability, give effects.
			return player;
		}
		// If the entity teleporting is something other than a player:
		else {
			WorldServer world = (WorldServer) entity.worldObj;
			NBTTagCompound tag = new NBTTagCompound();
			entity.writeToNBTOptional(tag);

			// Delete the entity. Will be taken care of next tick.
			entity.setDead();

			// Create new entity.
			Entity newEntity = EntityList.createEntityFromNBT(tag, world);

			// Set position, momentum of new entity at the other portal.
			if (newEntity != null) {
				newEntity.velocityChanged = true;
				newEntity.setLocationAndAngles(x, y, z, entity.rotationYaw,
						entity.rotationPitch);
				newEntity.forceSpawn = true;
				world.spawnEntityInWorld(newEntity);
				newEntity.setWorld(world);
			}

			world.resetUpdateEntityTick();

			return newEntity;
		}
	}
	
	private EntityPlayerMP respawnPlayerToLocation(EntityPlayerMP player, double x, double y, double z, WorldServer world)
	{
		MinecraftServer server = player.mcServer;
		ServerConfigurationManager config = server.getConfigurationManager();

		player.getServerForPlayer().getEntityTracker().removePlayerFromTrackers(player);
		player.getServerForPlayer().getEntityTracker().removeEntityFromAllTrackingPlayers(player);
		player.getServerForPlayer().getPlayerManager().removePlayer(player);
		config.playerEntityList.remove(player);
		server.worldServerForDimension(player.dimension).removePlayerEntityDangerously(player); // TODO: Replace with softer removal, which would just mark old entity as dead

		player.dimension = world.provider.dimensionId;
		ItemInWorldManager worldMgr = server.isDemo()
			? new DemoWorldManager(world)
			: new ItemInWorldManager(world);

		EntityPlayerMP newPlayer = new EntityPlayerMP(server, world, player.getGameProfile(), worldMgr);
		newPlayer.playerNetServerHandler = player.playerNetServerHandler;
		newPlayer.clonePlayer(player, true);
		newPlayer.dimension = world.provider.dimensionId;
		newPlayer.setEntityId(player.getEntityId());
		// was config.func_72381_a(newPlayer, player, world);
		newPlayer.theItemInWorldManager.setGameType(player.theItemInWorldManager.getGameType());
		newPlayer.theItemInWorldManager.initializeGameType(world.getWorldInfo().getGameType());
		
		newPlayer.setLocationAndAngles(x, y, z, player.rotationYaw, player.rotationPitch);

		world.theChunkProviderServer.loadChunk((int)newPlayer.posX >> 4, (int)newPlayer.posZ >> 4);

		newPlayer.playerNetServerHandler.sendPacket(new S07PacketRespawn(newPlayer.dimension, newPlayer.worldObj.difficultySetting, newPlayer.worldObj.getWorldInfo().getTerrainType(), newPlayer.theItemInWorldManager.getGameType()));
		newPlayer.playerNetServerHandler.setPlayerLocation(newPlayer.posX, newPlayer.posY, newPlayer.posZ, newPlayer.rotationYaw, newPlayer.rotationPitch);
		newPlayer.playerNetServerHandler.sendPacket(new S1FPacketSetExperience(newPlayer.experience, newPlayer.experienceTotal, newPlayer.experienceLevel));
		config.updateTimeAndWeatherForPlayer(newPlayer, world);
		world.getPlayerManager().addPlayer(newPlayer);
		world.spawnEntityInWorld(newPlayer);
		config.playerEntityList.add(newPlayer);
		newPlayer.addSelfToInternalCraftingInventory();
		newPlayer.setHealth(newPlayer.getHealth());
		FMLCommonHandler.instance().firePlayerRespawnEvent(newPlayer);
		return newPlayer;
	}
}
