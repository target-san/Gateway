package targetsan.mcmods.gateway

import cpw.mods.fml.common.event._
import cpw.mods.fml.common.Mod
import cpw.mods.fml.common.network.NetworkMod
import net.minecraftforge.common.Configuration
import java.io.File
import cpw.mods.fml.common.registry.GameRegistry
import cpw.mods.fml.common.registry.LanguageRegistry

@Mod(modid = "Gateway", useMetadata = true, modLanguage = "scala")
@NetworkMod(clientSideRequired = true, serverSideRequired = true)
object ModMain {
    val BLOCK_GATEWAY_ID = 1024
    private var gatewayId: Int = 0
    var blockGateway: BlockGateway = null
    
    @Mod.EventHandler
	def preInit(event: FMLPreInitializationEvent) {
        val configFile = new File(event.getModConfigurationDirectory().getAbsolutePath() + "/Gateway.cfg")
        val config = new Configuration(configFile)
        config.load()
        
        gatewayId = config.getBlock("gateway", BLOCK_GATEWAY_ID).getInt()
        
        config.save()
    }
    @Mod.EventHandler
	def init(event: FMLInitializationEvent) {
        blockGateway = new BlockGateway(gatewayId)
        GameRegistry.registerBlock(blockGateway, "gateway")
        LanguageRegistry.addName(blockGateway, "Gateway")
    }
    @Mod.EventHandler
	def postInit(event: FMLPostInitializationEvent) { }
}
