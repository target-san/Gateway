package targetsan.mcmods.gateway

import scala.util._
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.event.entity.living.{EnderTeleportEvent, LivingSpawnEvent}
import net.minecraft.world.World
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.ChatComponentText
import net.minecraft.init.Blocks
import net.minecraft.util.ChatStyle
import net.minecraft.util.EnumChatFormatting
import net.minecraftforge.event.world.WorldEvent
import net.minecraft.item.ItemStack
import net.minecraftforge.oredict.OreDictionary
import cpw.mods.fml.common.Loader
import net.minecraft.entity.Entity
import cpw.mods.fml.common.eventhandler.Event

object EventHandler
{
	private val BlockGatewayItemStack = new ItemStack(GatewayMod.BlockGateway, 1, OreDictionary.WILDCARD_VALUE)
	
	@SubscribeEvent
	def onWorldLoad(event: WorldEvent.Load): Unit =
		if (event.world.isRemote) // client-only
		if (Loader.isModLoaded("NotEnoughItems")) // makes sense only for NEI
			codechicken.nei.api.API.hideItem(BlockGatewayItemStack)
	
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
	// Prevent any mobs from spawning directly on Gateway surface
	@SubscribeEvent
	def onMobSpawn(event: LivingSpawnEvent.CheckSpawn): Unit =
		if (isGatewayUnderEntity(event.entity, event.world, event.x, event.y, event.z))
			event.setResult(Event.Result.DENY)
	
	private def isGatewayUnderEntity(entity: Entity, world: World, x: Double, y: Double, z: Double): Boolean =
		world.getBlock(Math.floor(x).toInt, Math.floor(y - entity.yOffset).toInt - 1, Math.floor(z).toInt) == GatewayMod.BlockGateway
}
