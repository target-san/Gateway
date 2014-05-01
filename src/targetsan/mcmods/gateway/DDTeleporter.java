package targetsan.mcmods.gateway;

import java.util.ArrayList;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemDoor;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.packet.Packet41EntityEffect;
import net.minecraft.network.packet.Packet43Experience;
import net.minecraft.network.packet.Packet9Respawn;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import cpw.mods.fml.common.registry.GameRegistry;

public class DDTeleporter
{
	private DDTeleporter() { }
	
	private static void setEntityPosition(Entity entity, double x, double y, double z)
	{
		entity.setLocationAndAngles(x, y, z, entity.rotationYaw, entity.rotationPitch);
		entity.worldObj.updateEntityWithOptionalForce(entity, false);
	}
	
	public static Entity teleport(Entity entity, double x, double y, double z, int dim)
	{
		if (entity == null)
			throw new IllegalArgumentException("entity cannot be null.");
		//This beautiful teleport method is based off of xCompWiz's teleport function. 

		WorldServer oldWorld = (WorldServer) entity.worldObj;
		WorldServer newWorld;
		EntityPlayerMP player = (entity instanceof EntityPlayerMP) ? (EntityPlayerMP) entity : null;

		// Is something riding? Handle it first.
		if (entity.riddenByEntity != null)
			return teleport(entity.riddenByEntity, x, y, z, dim);

		// Are we riding something? Dismount and tell the mount to go first.
		Entity cart = entity.ridingEntity;
		if (cart != null)
		{
			entity.mountEntity(null);
			cart = teleport(cart, x, y, z, dim);
			// We keep track of both so we can remount them on the other side.
		}

		// Determine if our destination is in another realm.
		boolean difDest = entity.dimension != dim;
		newWorld = difDest
			? MinecraftServer.getServer().worldServerForDimension(dim)
			: oldWorld;
		

		// GreyMaria: What is this even accomplishing? We're doing the exact same thing at the end of this all.
		// TODO Check to see if this is actually vital.
		setEntityPosition(entity, x, y, z);

		if (difDest) // Are we moving our target to a new dimension?
		{
			if(player != null) // Are we working with a player?
			{
				// We need to do all this special stuff to move a player between dimensions.
				//Give the client the dimensionData for the destination
				
				// Set the new dimension and inform the client that it's moving to a new world.
				player.dimension = dim;
				player.playerNetServerHandler.sendPacketToPlayer(new Packet9Respawn(player.dimension, (byte)player.worldObj.difficultySetting, newWorld.getWorldInfo().getTerrainType(), newWorld.getHeight(), player.theItemInWorldManager.getGameType()));

				// GreyMaria: Used the safe player entity remover before.
				// This should fix an apparently unreported bug where
				// the last non-sleeping player leaves the Overworld
				// for a pocket dimension, causing all sleeping players
				// to remain asleep instead of progressing to day.
				((WorldServer)entity.worldObj).getPlayerManager().removePlayer(player);
				oldWorld.removePlayerEntityDangerously(player);
				player.isDead = false;

				// Creates sanity by ensuring that we're only known to exist where we're supposed to be known to exist.
				oldWorld.getPlayerManager().removePlayer(player);
				newWorld.getPlayerManager().addPlayer(player);

				player.theItemInWorldManager.setWorld(newWorld);

				// Synchronize with the server so the client knows what time it is and what it's holding.
				player.mcServer.getConfigurationManager().updateTimeAndWeatherForPlayer(player, newWorld);
				player.mcServer.getConfigurationManager().syncPlayerInventory(player);
				for(Object potionEffect : player.getActivePotionEffects())
				{
					PotionEffect effect = (PotionEffect)potionEffect;
					player.playerNetServerHandler.sendPacketToPlayer(new Packet41EntityEffect(player.entityId, effect));
				}

				player.playerNetServerHandler.sendPacketToPlayer(new Packet43Experience(player.experience, player.experienceTotal, player.experienceLevel));
			}  	

			// Creates sanity by removing the entity from its old location's chunk entity list, if applicable.
			int entX = entity.chunkCoordX;
			int entZ = entity.chunkCoordZ;
			if ((entity.addedToChunk) && (oldWorld.getChunkProvider().chunkExists(entX, entZ)))
			{
				oldWorld.getChunkFromChunkCoords(entX, entZ).removeEntity(entity);
				oldWorld.getChunkFromChunkCoords(entX, entZ).isModified = true;
			}
			// Memory concerns.
			oldWorld.onEntityRemoved(entity);

			if (player == null) // Are we NOT working with a player?
			{
				NBTTagCompound entityNBT = new NBTTagCompound();
				entity.isDead = false;
				entity.writeMountToNBT(entityNBT);
				if(entityNBT.hasNoTags())
				{
					return entity;
				}
				entity.isDead = true;
				entity = EntityList.createEntityFromNBT(entityNBT, newWorld);

				if (entity == null)
				{
					// TODO FIXME IMPLEMENT NULL CHECKS THAT ACTUALLY DO SOMETHING.
					/* 
					 * shit ourselves in an organized fashion, preferably
					 * in a neat pile instead of all over our users' games
					 */
				}

			}

			// Finally, respawn the entity in its new home.
			newWorld.spawnEntityInWorld(entity);
			entity.setWorld(newWorld);
		}
		entity.worldObj.updateEntityWithOptionalForce(entity, false);

		// Hey, remember me? It's time to remount.
		if (cart != null)
		{
			// Was there a player teleported? If there was, it's important that we update shit.
			if (player != null)
			{
				entity.worldObj.updateEntityWithOptionalForce(entity, true);	
			}
			entity.mountEntity(cart);
		}

		// Did we teleport a player? Load the chunk for them.
		if (player != null)
		{
			newWorld.getChunkProvider().loadChunk(MathHelper.floor_double(entity.posX) >> 4, MathHelper.floor_double(entity.posZ) >> 4);
			// Tell Forge we're moving its players so everyone else knows.
			// Let's try doing this down here in case this is what's killing NEI.
			GameRegistry.onPlayerChangedDimension((EntityPlayer)entity);
		}
		setEntityPosition(entity, x, y, z);
		return entity;    
	}
}
