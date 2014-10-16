package targetsan.mcmods.gateway

import cpw.mods.fml.common.Loader
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import net.minecraft.item.ItemStack
import net.minecraft.util.{ChatComponentText, ChatStyle, EnumChatFormatting}
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.oredict.OreDictionary

import scala.util._

object EventHandler
{
	@SubscribeEvent
	def onWorldLoad(event: WorldEvent.Load): Unit =
		if (event.world.isRemote) // client-only
		if (Loader.isModLoaded("NotEnoughItems")) {
			// makes sense only for NEI
			codechicken.nei.api.API.hideItem(new ItemStack(Assets.BlockPlatform, 1, OreDictionary.WILDCARD_VALUE))
			codechicken.nei.api.API.hideItem(new ItemStack(Assets.BlockPillar, 1, OreDictionary.WILDCARD_VALUE))
		}

    @SubscribeEvent
    def onFlintAndSteelPreUse(event: PlayerInteractEvent): Unit =
		if (!event.entityPlayer.worldObj.isRemote) // Works only server-side
		// We're interested in Flint'n'Steel clicking some block only
		if (event.entityPlayer != null)
		if (event.entityPlayer.getHeldItem != null)
		if (event.entityPlayer.getHeldItem.getItem == net.minecraft.init.Items.flint_and_steel)
		if (event.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK)
			{} // TODO: start multiblock construction here
}
