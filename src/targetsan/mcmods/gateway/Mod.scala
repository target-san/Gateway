package targetsan.mcmods.gateway

import cpw.mods.fml.common.Mod
import cpw.mods.fml.common.event.FMLInitializationEvent
import cpw.mods.fml.common.event.FMLPostInitializationEvent
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World
import net.minecraft.world.chunk.Chunk
import net.minecraftforge.common.MinecraftForge
import cpw.mods.fml.common.registry.GameRegistry
import net.minecraft.server.MinecraftServer
import net.minecraft.util.{ChunkCoordinates, IIcon}
import net.minecraft.entity.Entity
import net.minecraftforge.common.util.ForgeDirection

import scala.collection.mutable.ListBuffer
import scala.reflect.ClassTag

@Mod(modid = "gateway", useMetadata = true, modLanguage = "scala")
object GatewayMod {
	val MODID = "gateway"

    @Mod.EventHandler
	def init(event: FMLInitializationEvent)
	{
	}

    @Mod.EventHandler
	def postInit(event: FMLPostInitializationEvent)
    {
    	MinecraftForge.EVENT_BUS.register(EventHandler)
    }
}

object Utils
{
	case class ChunkPos(x: Int, z: Int, world: World) {
		def this(chunk: Chunk) =
			this(chunk.xPosition, chunk.zPosition, chunk.worldObj)
	}
	case class BlockPos(x: Int, y: Int, z: Int, world: World) {
		def chunk = ChunkPos(x >> 4, z >> 4, world)

		def this(tile: TileEntity) =
			this(tile.xCoord, tile.yCoord, tile.zCoord, tile.getWorldObj)
		def this(coords: ChunkCoordinates, world: World) =
			this(coords.posX, coords.posY, coords.posZ, world)

		def + (that: BlockPos) = BlockPos(x + that.x, y + that.y, z + that.z, world)

		def + (dir: ForgeDirection) = BlockPos(x + dir.offsetX, y + dir.offsetY, z + dir.offsetZ, world)
		def - (dir: ForgeDirection) = this + dir.getOpposite
	}

	def offsetToDirection(x: Int, y: Int, z: Int): ForgeDirection =
		(x, y, z) match {
			case ( 0, -1,  0) => ForgeDirection.DOWN
			case ( 0,  1,  0) => ForgeDirection.UP
			case ( 0,  0, -1) => ForgeDirection.NORTH
			case ( 0,  0,  1) => ForgeDirection.SOUTH
			case (-1,  0,  0) => ForgeDirection.WEST
			case ( 1,  0,  0) => ForgeDirection.EAST

			case _ => ForgeDirection.UNKNOWN
		}

	val NetherDimensionId = -1
	val EndDimensionId = 1

	val DefaultCooldown = 80
	
	val InterDimensionId = NetherDimensionId
	def interDimension = Utils.world(InterDimensionId)
	
	def world(dim: Int) = MinecraftServer.getServer.worldServerForDimension(dim)
	
	def enumVolume(x1: Int, y1: Int, z1: Int, x2: Int, y2: Int, z2: Int) =
		for (x <- x1 to x2; y <- y1 to y2; z <- z1 to z2) yield (x, y, z)

	def bottomMount(entity: Entity): Entity =
		if (entity == null) null
		else if (entity.ridingEntity != null) bottomMount(entity.ridingEntity)
		else entity

	def enumRiders(entity: Entity): Seq[Entity] =
	{
		var iter = bottomMount(entity)
		val builder = ListBuffer[Entity]()
		while (iter != null)
		{
			builder += iter
			iter = iter.riddenByEntity
		}
		builder.result()
	}

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
