package targetsan.mcmods.gateway

import cpw.mods.fml.common.event._
import cpw.mods.fml.common.Mod
import cpw.mods.fml.common.network.NetworkMod
import net.minecraftforge.common.Configuration
import java.io.File
import cpw.mods.fml.common.registry.GameRegistry
import cpw.mods.fml.common.registry.LanguageRegistry
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.block.Block
import net.minecraft.item.Item

@Mod(modid = "Gateway", useMetadata = true, modLanguage = "scala")
@NetworkMod(clientSideRequired = true, serverSideRequired = true)
object ModMain {
    private val BLOCK_GATEWAY_ID = 1024
    private val BLOCK_PORTAL_ID = BLOCK_GATEWAY_ID + 1
    private val BLOCK_KEYSTONE_ID = BLOCK_PORTAL_ID + 1
    
    private val ITEM_GATE_KEY_ID = 4096
    
    private var blockKeystoneId: Int = 0
    private var blockGatewayId: Int = 0
    private var blockPortalId: Int = 0
    private var itemGateIgniterId: Int = 0
    
    @Mod.EventHandler
	def preInit(event: FMLPreInitializationEvent) {
        val configFile = new File(event.getModConfigurationDirectory().getAbsolutePath() + "/Gateway.cfg")
        val config = new Configuration(configFile)
        config.load()
        
        blockKeystoneId = config.getBlock("keystone", BLOCK_KEYSTONE_ID).getInt()
        blockGatewayId = config.getBlock("gateway", BLOCK_GATEWAY_ID).getInt()
        blockPortalId = config.getBlock("portal", BLOCK_PORTAL_ID).getInt()
        itemGateIgniterId = config.getItem("gatekey", ITEM_GATE_KEY_ID).getInt()
        
        config.save()
    }
    @Mod.EventHandler
	def init(event: FMLInitializationEvent) {
        Assets.blockKeystone = new BlockKeystone(blockKeystoneId)
        GameRegistry.registerBlock(Assets.blockKeystone, classOf[ItemBlock], "keystone", "Gateway")
        LanguageRegistry.addName(Assets.blockKeystone, "Keystone")
        
        Assets.blockPortal = new BlockPortal(blockPortalId)
        GameRegistry.registerBlock(Assets.blockPortal, classOf[ItemBlock], "portal", "Gateway")
        LanguageRegistry.addName(Assets.blockPortal, "Gateway portal pillar")
        
        Assets.blockGateway = new BlockGateway(blockGatewayId)
        GameRegistry.registerBlock(Assets.blockGateway, classOf[ItemBlock], "gateway", "Gateway")
        LanguageRegistry.addName(Assets.blockGateway, "Gateway")
        
        Assets.itemGateIgniter = new ItemGateIgniter(itemGateIgniterId)
        GameRegistry.registerItem(Assets.itemGateIgniter, "gatekey", "Gateway")
        LanguageRegistry.addName(Assets.itemGateIgniter, "Gateway Igniter")
    }
    @Mod.EventHandler
	def postInit(event: FMLPostInitializationEvent) {
        GameRegistry.addRecipe(new ItemStack(Assets.blockKeystone),
            "oio",
            "igi",
            "oio",
            'o': Character, Block.obsidian,
            'i': Character, Item.ingotIron,
            'g': Character, Block.glass
        )
        GameRegistry.addRecipe(new ItemStack(Assets.itemGateIgniter),
            "r f",
            "nen",
            "nnn",
            'r': Character, Item.redstone,
            'f': Character, Item.flint,
            'n': Character, Item.goldNugget,
            'e': Character, Item.enderPearl
		)
    }
}

object Assets {
    var blockKeystone: BlockKeystone = null
    var blockGateway: BlockGateway = null
    var blockPortal: BlockPortal = null
    
    var itemGateIgniter: ItemGateIgniter = null
}
