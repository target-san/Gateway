package targetsan.mcmods.gateway

import cpw.mods.fml.common.event._
import cpw.mods.fml.common.Mod
import cpw.mods.fml.common.network.NetworkMod
import net.minecraftforge.common.Configuration
import java.io.File
import cpw.mods.fml.common.registry.GameRegistry
import cpw.mods.fml.common.registry.LanguageRegistry
import net.minecraft.item.ItemBlock

@Mod(modid = "Gateway", useMetadata = true, modLanguage = "scala")
@NetworkMod(clientSideRequired = true, serverSideRequired = true)
object ModMain {
    private val BLOCK_GATEWAY_ID = 1024
    private val BLOCK_PORTAL_ID = BLOCK_GATEWAY_ID + 1
    private var gatewayId: Int = 0
    private var portalId: Int = 0
    
    @Mod.EventHandler
	def preInit(event: FMLPreInitializationEvent) {
        val configFile = new File(event.getModConfigurationDirectory().getAbsolutePath() + "/Gateway.cfg")
        val config = new Configuration(configFile)
        config.load()
        
        gatewayId = config.getBlock("gateway", BLOCK_GATEWAY_ID).getInt()
        portalId = config.getBlock("portal", BLOCK_PORTAL_ID).getInt()
        
        config.save()
    }
    @Mod.EventHandler
	def init(event: FMLInitializationEvent) {
        Blocks.portal = new BlockPortal(portalId)
        GameRegistry.registerBlock(Blocks.portal, classOf[ItemBlock], "portal", "Gateway")
        LanguageRegistry.addName(Blocks.portal, "Gateway portal pillar")
        
        Blocks.gateway = new BlockGateway(gatewayId)
        GameRegistry.registerBlock(Blocks.gateway, classOf[ItemBlock], "gateway", "Gateway")
        LanguageRegistry.addName(Blocks.gateway, "Gateway")
    }
    @Mod.EventHandler
	def postInit(event: FMLPostInitializationEvent) { }
}
