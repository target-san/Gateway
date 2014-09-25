package targetsan.mcmods.gateway

import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.util.ForgeDirection
import scala.reflect.ClassTag
import targetsan.mcmods.gateway.connectors._
import Utils._

trait ConnectorHost {
	def tiles: Map[ForgeDirection, TileEntity]

	def tile(side: ForgeDirection): Option[TileEntity] = tiles get side

	def tileAs[T: ClassTag](side: ForgeDirection): Option[T] =
		tiles get side flatMap { _.as[T] }

	def tilesAs[T: ClassTag]: Map[ForgeDirection, T] =
		for {
			(key, value) <- tiles
			typed <- value.as[T]
		}
			yield (key, typed)
}

/** Uses relatively cheap tag function to update heavyweight main value
 *  Used to retrieve connected tiles at most once per tick
 */
class Tagged[T]( private val tagfunc: () => Long, private val init: () => T) {
	private var tag = 0L
	private var value: Option[T] = None

	def get: T = {
		val newtag = tagfunc()
		if (tag != newtag)
		{
			tag = newtag
			value = None
		}

		if (value.isEmpty)
			value = Some(init())
		value.get
	}
}

class TileSatellite extends TileEntity with ConnectorHost
	with FluidConnector
{
	//******************************************************************************************************************
	// ConnectorHost support
	//******************************************************************************************************************
	def tiles = ConnectedTiles.get

	//******************************************************************************************************************
	// Some private data
	//******************************************************************************************************************
	private lazy val SatBlock = GatewayMod.BlockGateway.subBlock(getBlockMetadata).asInstanceOf[SubBlockSatellite]
	private lazy val ConnectedSides =
		List(
			SatBlock.xOffset match {
				case -1 => ForgeDirection.WEST
				case 1 => ForgeDirection.EAST
				case _ => ForgeDirection.UNKNOWN
			},
			SatBlock.zOffset match {
				case -1 => ForgeDirection.NORTH
				case 1 => ForgeDirection.SOUTH
				case _ => ForgeDirection.UNKNOWN
			}
		)
			.filter(_ != ForgeDirection.UNKNOWN)

	//******************************************************************************************************************
	// Connector maps, lazily constructed
	//******************************************************************************************************************
	private val ConnectedSats = new Tagged(
		() => worldObj.getTotalWorldTime,
		() => // Did this as a for-comprehension for safety - no crashes,
			  // just connected tiles not accessible on any trouble {
		{
			for {
				core <- worldObj // locate this satellite's core
					.getTileEntity(xCoord - SatBlock.xOffset, yCoord, zCoord - SatBlock.zOffset)
					.as[TileGateway]
					.view
				endPos <- Option(core.getExitPos).view
				endCore <- core // locate exit core
					.getExitWorld
					.getTileEntity(endPos.posX, endPos.posY, endPos.posZ)
					.as[TileGateway]
					.view
				side <- ConnectedSides // enum sides
				sat <- endCore // locate connected satellites
					.getWorldObj
					.getTileEntity(endCore.xCoord - side.offsetX, endCore.yCoord, endCore.zCoord - side.offsetZ)
					.as[TileSatellite]
			}
				yield (side, sat)
		}.toMap
	)

	private val ConnectedTiles = new Tagged(
		() => worldObj.getTotalWorldTime,
		() =>
			for {
				(side, sat) <- ConnectedSats.get
				tile <- Option(// tile entity adjacent to satellite
					sat
					.getWorldObj
					.getTileEntity(sat.xCoord - side.offsetX, sat.yCoord, sat.zCoord - side.offsetZ)
				)
			}
				yield (side, tile)
	)
}
