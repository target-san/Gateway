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
    def onFlintAndSteelPreUse(event: PlayerInteractEvent): Unit =
    {
		if (event.entityPlayer.worldObj.isRemote) // Works only server-side
			return
		// We're interested in Flint'n'Steel clicking some block only
		if (event.entityPlayer == null ||
			event.entityPlayer.getHeldItem.getItem != net.minecraft.init.Items.flint_and_steel ||
			event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK
		)
			return
		// Try place gateway here
		Utils.tryPlaceGateway(event.entityPlayer.worldObj, event.x, event.y, event.z, event.entityPlayer)
    }
}
