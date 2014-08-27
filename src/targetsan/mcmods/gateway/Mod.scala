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
import net.minecraft.server.MinecraftServer
import net.minecraft.world.World

@Mod(modid = "gateway", useMetadata = true, modLanguage = "scala")
object GatewayMod {
	val MODID = "gateway"
		
	val BlockGatewayBase = new BlockGatewayBase
	val BlockGatewayAir  = new BlockGatewayAir 
    
    @Mod.EventHandler
	def init(event: FMLInitializationEvent)
	{
		GameRegistry.registerBlock(BlockGatewayBase, "GatewayBase")
		GameRegistry.registerBlock(BlockGatewayAir,  "GatewayAir")
		GameRegistry.registerTileEntity(classOf[TileGateway], "tileGateway")
	}

    @Mod.EventHandler
	def postInit(event: FMLPostInitializationEvent)
    {
    	MinecraftForge.EVENT_BUS.register(Gateway)
    }
}

object Utils
{
	def world(dim: Int) = MinecraftServer.getServer().worldServerForDimension(dim)
	
	def enumVolume(x1: Int, y1: Int, z1: Int, x2: Int, y2: Int, z2: Int) =
		for (x <- x1 to x2; y <- y1 to y2; z <- z1 to z2) yield (x, y, z)
}
