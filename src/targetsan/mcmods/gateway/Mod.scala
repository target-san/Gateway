package targetsan.mcmods.gateway

import cpw.mods.fml.common.Mod
import cpw.mods.fml.common.event.FMLInitializationEvent
import cpw.mods.fml.common.event.FMLPostInitializationEvent
import net.minecraftforge.common.MinecraftForge
import cpw.mods.fml.common.registry.GameRegistry
import net.minecraft.server.MinecraftServer
import net.minecraft.util.IIcon

import scala.reflect.ClassTag

@Mod(modid = "gateway", useMetadata = true, modLanguage = "scala")
object GatewayMod {
	val MODID = "gateway"
		
	val BlockGateway = new BlockGateway
    
    @Mod.EventHandler
	def init(event: FMLInitializationEvent)
	{
		GameRegistry.registerBlock(BlockGateway, "BlockGateway")
		GameRegistry.registerTileEntity(classOf[TileGateway], "tileGateway")
	}

    @Mod.EventHandler
	def postInit(event: FMLPostInitializationEvent)
    {
    	MinecraftForge.EVENT_BUS.register(EventHandler)
    }
}

object Utils
{
	val DefaultCooldown = 80
	
	val InterDimensionId = -1 // Nether
	def interDimension = Utils.world(InterDimensionId)
	
	def world(dim: Int) = MinecraftServer.getServer.worldServerForDimension(dim)
	
	def enumVolume(x1: Int, y1: Int, z1: Int, x2: Int, y2: Int, z2: Int) =
		for (x <- x1 to x2; y <- y1 to y2; z <- z1 to z2) yield (x, y, z)
		
	implicit class IconTransformOps(icon: IIcon) extends AnyRef
	{
		def flippedU: IIcon = new IconTransformer(icon, true, false)
		def flippedV: IIcon = new IconTransformer(icon, false, true)
		def flippedUV: IIcon = new IconTransformer(icon, true, true)
	}
	
	class IconTransformer(private val icon: IIcon, private val flipU: Boolean, private val flipV: Boolean) extends IIcon
	{
		def getMinU = if (flipU) icon.getMaxU else icon.getMinU
		def getMaxU = if (flipU) icon.getMinU else icon.getMaxU
		def getMinV = if (flipV) icon.getMaxV else icon.getMinV
		def getMaxV = if (flipV) icon.getMinV else icon.getMaxV
		
		def getInterpolatedU(u: Double) = getMinU + (getMaxU - getMinU) * (u.toFloat / 16.0F)
		def getInterpolatedV(v: Double) = getMinV + (getMaxV - getMinV) * (v.toFloat / 16.0F)
    
    	def getIconWidth = icon.getIconWidth
    	def getIconHeight = icon.getIconHeight
    	def getIconName = icon.getIconName
	}

	implicit class SafeCast(t: Any) {
		def as[T](implicit tag: ClassTag[T]): Option[T] =
			t match {
				case tag(typed) => Some(typed)
				case _ => None
			}
	}
}
