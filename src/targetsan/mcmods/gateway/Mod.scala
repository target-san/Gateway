package targetsan.mcmods.gateway

import cpw.mods.fml.common.Mod
import cpw.mods.fml.common.event.FMLInitializationEvent
import cpw.mods.fml.common.event.FMLPostInitializationEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.registry.GameRegistry
import net.minecraft.item.ItemBlock
import net.minecraft.block.Block

@Mod(modid = "gateway", useMetadata = true, modLanguage = "scala")
object GatewayMod {
	val MODID = "gateway"
		
	var BlockGatewayBase: Block = null
	var BlockGatewayAir: Block = null 
    
    @Mod.EventHandler
	def init(event: FMLInitializationEvent)
	{
		BlockGatewayBase = GameRegistry.registerBlock(new BlockGatewayBase, "GatewayBase")
		BlockGatewayAir  = GameRegistry.registerBlock(new BlockGatewayAir,  "GatewayAir")
		GameRegistry.registerTileEntity(classOf[TileGateway], "tileGateway")
	}

    @Mod.EventHandler
	def postInit(event: FMLPostInitializationEvent)
    {
    	MinecraftForge.EVENT_BUS.register(this)
    }
    
    @SubscribeEvent
    def onPlayerInteract(event: PlayerInteractEvent): Unit =
    {
    	Utils.flintAndSteelPreUse(event)
    }
}
