package targetsan.mcmods.gateway

import cpw.mods.fml.common.Mod
import cpw.mods.fml.common.event.FMLInitializationEvent
import cpw.mods.fml.common.event.FMLPostInitializationEvent
import cpw.mods.fml.common.registry.GameRegistry
import net.minecraftforge.common.MinecraftForge

@Mod(modid = "gateway", useMetadata = true, modLanguage = "scala")
object GatewayMod {
	val MODID = "gateway"

    @Mod.EventHandler
	def init(event: FMLInitializationEvent): Unit =
	{
		GameRegistry.registerBlock(Assets.BlockGateway, "gateway")
		GameRegistry.registerBlock(Assets.BlockPillar, "pillar")

		GameRegistry.registerTileEntity(classOf[tile.Core],  "gateway-core")
		GameRegistry.registerTileEntity(classOf[tile.Perimeter], "gateway-perimeter")
	}

    @Mod.EventHandler
	def postInit(event: FMLPostInitializationEvent)
    {
    	MinecraftForge.EVENT_BUS.register(EventHandler)
    }
}

object Assets {
	val BlockGateway = new block.Gateway
	val BlockPillar = new block.Pillar
}


