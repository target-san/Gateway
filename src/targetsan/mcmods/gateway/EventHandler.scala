package targetsan.mcmods.gateway

import cpw.mods.fml.relauncher.{Side, SideOnly}

import scala.util._
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.event.entity.living.{EnderTeleportEvent, LivingSpawnEvent}
import net.minecraft.world.World
import net.minecraft.util.{ChatStyle, ChatComponentText, EnumChatFormatting}
import net.minecraftforge.event.world.{ChunkEvent, WorldEvent}
import net.minecraft.item.ItemStack
import net.minecraftforge.oredict.OreDictionary
import cpw.mods.fml.common.Loader
import net.minecraft.entity.Entity
import cpw.mods.fml.common.eventhandler.Event

object EventHandler
{
	lazy val INSTANCE = if (Utils.isServer) Server else Client

	object Client {
		private val BlockGatewayItemStack = new ItemStack(GatewayMod.BlockGateway, 1, OreDictionary.WILDCARD_VALUE)

		@SubscribeEvent
		def onWorldLoad(event: WorldEvent.Load): Unit =
			if (Loader.isModLoaded("NotEnoughItems")) // makes sense only for NEI
				codechicken.nei.api.API.hideItem(BlockGatewayItemStack)
	}

	object Server {
		// Used to handle unload watchers when they're needed
		// Satellite installs such a watcher over its linked tile's chunk
		// but only in case they're in different chunks
		@SubscribeEvent
		def onChunkUnload(event: ChunkEvent.Unload): Unit =
			ChunkWatcher.onChunkUnload(event.getChunk)

		@SubscribeEvent
		def onFlintAndSteelPreUse(event: PlayerInteractEvent): Unit =
		// We're interested in Flint'n'Steel clicking some block only
			if (event.entityPlayer != null)
				if (event.entityPlayer.getHeldItem != null)
					if (event.entityPlayer.getHeldItem.getItem == net.minecraft.init.Items.flint_and_steel)
						if (event.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK)
							GatewayMod
								.BlockGateway
								.cores
								// Success(false) = nothing found, but we can continue
								// Success(true)  = some multiblock successfully constructed, we should stop
								// Failure(error) = some multiblock was valid to construct, but an error occured
								.foldLeft[Try[Boolean]](Success(false)) { case (Success(false), (_, core)) =>
								core.multiblock.assemble(event.entityPlayer.worldObj, event.x, event.y, event.z, event.entityPlayer)
							case (x, _) => x
							} match {
								case Failure(error) =>
									event.entityPlayer.addChatMessage(
										new ChatComponentText(error.getMessage)
											.setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED))
									)
								case _ => ()
							}

		// Prevent Endermen from teleporting onto Gateway surface
		@SubscribeEvent
		def onEnderTeleport(event: EnderTeleportEvent): Unit =
			if (isGatewayUnderEntity(event.entity, event.entity.worldObj, event.targetX, event.targetY, event.targetZ))
				event.setCanceled(true)

		// Prevent any mobs from spawning directly on Gateway surface
		@SubscribeEvent
		def onMobSpawn(event: LivingSpawnEvent.CheckSpawn): Unit =
			if (isGatewayUnderEntity(event.entity, event.world, event.x, event.y, event.z))
				event.setResult(Event.Result.DENY)

		private def isGatewayUnderEntity(entity: Entity, world: World, x: Double, y: Double, z: Double): Boolean =
			world.getBlock(Math.floor(x).toInt, Math.floor(y - entity.yOffset).toInt - 1, Math.floor(z).toInt) == GatewayMod.BlockGateway
	}
}
