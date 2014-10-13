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
		GameRegistry.registerBlock(Blocks.Gateway, "gateway")
		GameRegistry.registerBlock(Blocks.Pillar, "pillar")
	}

    @Mod.EventHandler
	def postInit(event: FMLPostInitializationEvent)
    {
    	MinecraftForge.EVENT_BUS.register(EventHandler)
    }
}

object Blocks {
	val Gateway = new block.Gateway
	val Pillar = new block.Pillar
}


