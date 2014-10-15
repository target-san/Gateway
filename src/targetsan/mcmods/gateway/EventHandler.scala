package targetsan.mcmods.gateway

import cpw.mods.fml.common.Loader
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import net.minecraft.entity.Entity
import net.minecraft.item.ItemStack
import net.minecraft.util.{ChatComponentText, ChatStyle, EnumChatFormatting}
import net.minecraft.world.World
import net.minecraftforge.event.entity.living.EnderTeleportEvent
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.event.world.{ChunkEvent, WorldEvent}
import net.minecraftforge.oredict.OreDictionary

import scala.util._

object EventHandler
{
	@SubscribeEvent
	def onWorldLoad(event: WorldEvent.Load): Unit =
		if (event.world.isRemote) // client-only
		if (Loader.isModLoaded("NotEnoughItems")) {
			// makes sense only for NEI
			codechicken.nei.api.API.hideItem(new ItemStack(Assets.BlockGateway, 1, OreDictionary.WILDCARD_VALUE))
			codechicken.nei.api.API.hideItem(new ItemStack(Assets.BlockPillar, 1, OreDictionary.WILDCARD_VALUE))
		}

	// Used to handle unload watchers when they're needed
	// Satellite installs such a watcher over its linked tile's chunk
	// but only in case they're in different chunks
	@SubscribeEvent
	def onChunkUnload(event: ChunkEvent.Unload): Unit =
		if (!event.world.isRemote)
			ChunkWatcher.onChunkUnload(event.getChunk)
	
    @SubscribeEvent
    def onFlintAndSteelPreUse(event: PlayerInteractEvent): Unit =
		if (!event.entityPlayer.worldObj.isRemote) // Works only server-side
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
				.foldLeft[Try[Boolean]](Success(false))
				{	case (Success(false), (_, core)) =>
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

	private def isGatewayUnderEntity(entity: Entity, world: World, x: Double, y: Double, z: Double): Boolean =
		world.getBlock(Math.floor(x).toInt, Math.floor(y - entity.yOffset).toInt - 1, Math.floor(z).toInt) == Assets.BlockGateway
}
