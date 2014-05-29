package targetsan.mcmods.gateway

import cpw.mods.fml.common.Mod
import cpw.mods.fml.common.event.FMLInitializationEvent
import cpw.mods.fml.common.event.FMLPostInitializationEvent

@Mod(modid = "gateway", useMetadata = true, modLanguage = "scala")
object GatewayMod {
	val MODID = "gateway"
    
    @Mod.EventHandler
	def init(event: FMLInitializationEvent)
	{ }
    @Mod.EventHandler
	def postInit(event: FMLPostInitializationEvent)
    { }
}
