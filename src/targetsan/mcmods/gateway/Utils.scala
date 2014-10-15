package targetsan.mcmods.gateway

import net.minecraft.entity.Entity
import net.minecraft.server.MinecraftServer
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.{IIcon, ChunkCoordinates}
import net.minecraft.world.World
import net.minecraft.world.chunk.Chunk
import net.minecraftforge.common.util.ForgeDirection

import scala.collection.mutable.ListBuffer
import scala.reflect.ClassTag

package object Utils
{
	private def maskOfSize(size: Int) = (0x01 << size) - 1

	def getBits(field: Int, offset: Int, size: Int) =
		(field >>> offset) & maskOfSize(size)
	def setBits(field: Int, offset: Int, size: Int, bits: Int): Int = {
		val mask = maskOfSize(size) << offset
		(field & (~mask)) | ((bits << offset) & mask)
	}

	case class ChunkPos(x: Int, z: Int) {
		def withWorld(world: World) = ChunkPosD(x, z, world)
		def withDim(dim: Int): ChunkPosD = withWorld(Utils.world(dim))
	}
	object ChunkPos {
		def apply(chunk: Chunk): ChunkPos = ChunkPos(chunk.xPosition, chunk.zPosition)
	}

	case class ChunkPosD(x: Int, z: Int, world: World) {
		def noWorld = ChunkPos(x, z)
	}
	object ChunkPosD {
		def apply(chunk: Chunk): ChunkPosD = ChunkPosD(chunk.xPosition, chunk.zPosition, chunk.worldObj)
		def apply(x: Int, y: Int, dim: Int): ChunkPosD = ChunkPos(x, y).withDim(dim)
	}

	case class BlockPos(x: Int, y: Int, z: Int) {
		def withWorld(world: World) = BlockPosD(x, y, z, world)
		def withDim(dim: Int): BlockPosD = withWorld(Utils.world(dim))

		def chunk = ChunkPos(x >> 4, z >> 4)
		def chunkCoordinates = new ChunkCoordinates(x, y, z)

		def + (that: BlockPos) = BlockPos(x + that.x, y + that.y, z + that.z)
		def - (that: BlockPos) = BlockPos(x - that.x, y - that.y, z - that.z)
	}
	object BlockPos {
		def apply(tile: TileEntity): BlockPos = BlockPos(tile.xCoord, tile.yCoord, tile.zCoord)
	}

	case class BlockPosD(x: Int, y: Int, z: Int, world: World) {
		def noWorld = BlockPos(x, y, z)
		def dim = world.provider.dimensionId
	}
	object BlockPosD {
		def apply(tile: TileEntity): BlockPosD = BlockPos(tile).withWorld(tile.getWorldObj)
		def apply(x: Int, y: Int, z: Int, dim: Int): BlockPosD = BlockPos(x, y, z).withDim(dim)
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

	def enumVolume(x1: Int, y1: Int, z1: Int, x2: Int, y2: Int, z2: Int): Seq[(Int, Int, Int)] =
		for (x <- x1 to x2; y <- y1 to y2; z <- z1 to z2) yield (x, y, z)

	def enumVolume(min: BlockPos, max: BlockPos): Seq[(Int, Int, Int)] = enumVolume(min.x, min.y, min.z, max.x, max.y, max.z)

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
