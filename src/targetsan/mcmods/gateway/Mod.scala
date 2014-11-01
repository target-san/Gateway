package targetsan.mcmods.gateway

import cpw.mods.fml.common.{Loader, Mod}
import cpw.mods.fml.common.event.FMLInitializationEvent
import cpw.mods.fml.common.event.FMLPostInitializationEvent
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.registry.GameRegistry

import net.minecraft.item.ItemStack
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.oredict.OreDictionary

@Mod(modid = "gateway", useMetadata = true, modLanguage = "scala")
object GatewayMod {
	val MODID = "gateway"

    @Mod.EventHandler
	def init(event: FMLInitializationEvent): Unit =
	{
		GameRegistry.registerBlock(Assets.BlockPlatform, "platform")
		GameRegistry.registerBlock(Assets.BlockPillar, "pillar")

		GameRegistry.registerTileEntity(classOf[tile.Core],  "platform-core")
		GameRegistry.registerTileEntity(classOf[tile.Perimeter], "platform-perimeter")
	}

    @Mod.EventHandler
	def postInit(event: FMLPostInitializationEvent)
    {
    	MinecraftForge.EVENT_BUS.register(Assets)
	    MinecraftForge.EVENT_BUS.register(block.Multiblock)
	    MinecraftForge.EVENT_BUS.register(AchievementTracker)
    }
}

object Assets {
	val BlockPlatform = new block.Platform
	val BlockPillar = new block.Pillar

	@SubscribeEvent
	def hideBlocksFromNEI(event: WorldEvent.Load): Unit =
		if (event.world.isRemote) // client-only
		if (Loader.isModLoaded("NotEnoughItems")) {
			// makes sense only for NEI
			codechicken.nei.api.API.hideItem(new ItemStack(BlockPlatform, 1, OreDictionary.WILDCARD_VALUE))
			codechicken.nei.api.API.hideItem(new ItemStack(BlockPillar, 1, OreDictionary.WILDCARD_VALUE))
		}
}
