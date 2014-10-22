package targetsan.mcmods.gateway

import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.server.MinecraftServer
import net.minecraft.tileentity.TileEntity
import net.minecraft.util._
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
	// Equivalent to ChunkCoordinates, but immutable
	class BlockPos private (val x: Int, val y: Int, val z: Int) {
		def toChunkCoordinates = new ChunkCoordinates(x, y, z)

		def unary_- = new BlockPos(-x, -y, -z)

		def offset(dx: Int, dy: Int, dz: Int) = new BlockPos(x + dx, y + dy, z + dz)
		def offset(that: BlockPos) = new BlockPos(x + that.x, y + that.y, z + that.z)

		def + (that: BlockPos) = offset(that)
		def - (that: BlockPos) = offset(-that)

		def to (that: BlockPos) = Volume(this, that)
	}

	object BlockPos {
		def apply(x: Int, y: Int, z: Int) = new BlockPos(x, y, z)
		def apply(tile: TileEntity) = new BlockPos(tile.xCoord, tile.yCoord, tile.zCoord)
		def apply(coords: ChunkCoordinates) = new BlockPos(coords.posX, coords.posY, coords.posZ)
	}

	class Volume private (val minX: Int, val minY: Int, val minZ: Int, val maxX: Int, val maxY: Int, val maxZ: Int) {

		val min = BlockPos(minX, minY, minZ)
		val max = BlockPos(maxX, maxY, maxZ)

		val rangeX = minX to maxX
		val rangeY = minY to maxY
		val rangeZ = minZ to maxZ

		val sizeX = rangeX.length
		val sizeY = rangeY.length
		val sizeZ = rangeZ.length

		def contains (x: Int, y: Int, z: Int): Boolean = (rangeX contains x) && (rangeY contains y) && (rangeZ contains z)
		def contains (pos: BlockPos): Boolean = contains(pos.x, pos.y, pos.z)

		def enum = enumVolume(min, max)
	}

	object Volume {
		def apply(x1: Int, y1: Int, z1: Int, x2: Int, y2: Int, z2: Int): Volume =
			new Volume(
				Math.min(x1, x2),
				Math.min(y1, y2),
				Math.min(z1, z2),
				Math.max(x1, x2),
				Math.max(y1, y2),
				Math.max(z1, z2)
			)

		def apply(pos1: BlockPos, pos2: BlockPos): Volume = Volume(pos1.x, pos1.y, pos1.z, pos2.x, pos2.y, pos2.z)
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

	def offsetToDirection(offset: BlockPos): ForgeDirection = offsetToDirection(offset.x, offset.y, offset.z)

	val NetherDimensionId = -1
	val EndDimensionId = 1

	val DefaultCooldown = 80

	val InterDimensionId = NetherDimensionId
	def interDimension = Utils.world(InterDimensionId)

	def world(dim: Int) = MinecraftServer.getServer.worldServerForDimension(dim)

	def enumVolume(x1: Int, y1: Int, z1: Int, x2: Int, y2: Int, z2: Int): Seq[BlockPos] =
		for (x <- x1 to x2; y <- y1 to y2; z <- z1 to z2) yield BlockPos(x, y, z)

	def enumVolume(min: BlockPos, max: BlockPos): Seq[BlockPos] = enumVolume(min.x, min.y, min.z, max.x, max.y, max.z)

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

	object Chat {
		def ok(player: EntityPlayer, message: String, args: Object*): Unit =
			player.addChatComponentMessage(colorChat(EnumChatFormatting.GREEN, message, args: _*))
		def warn(player: EntityPlayer, message: String, args: Object*): Unit =
			player.addChatComponentMessage(colorChat(EnumChatFormatting.YELLOW, message, args: _*))
		def error(player: EntityPlayer, message: String, args: Object*): Unit =
			player.addChatComponentMessage(colorChat(EnumChatFormatting.RED, message, args: _*))

		private def colorChat(color: EnumChatFormatting, message: String, args: Object*) =
			new ChatComponentTranslation(GatewayMod.MODID + ":" + message, args: _*).setChatStyle(new ChatStyle().setColor(color))
	}
}
