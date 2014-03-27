package targetsan.mcmods.gateway

import cpw.mods.fml.common.event._
import cpw.mods.fml.common.Mod
import cpw.mods.fml.common.network.NetworkMod

@Mod(modid = "Gateway", useMetadata = true, modLanguage = "scala")
@NetworkMod(clientSideRequired = true, serverSideRequired = true)
object ModMain {
    @Mod.EventHandler
	def preInit(event: FMLPreInitializationEvent) { }	
    @Mod.EventHandler
	def init(event: FMLInitializationEvent) { }
    @Mod.EventHandler
	def postInit(event: FMLPostInitializationEvent) { }
}
