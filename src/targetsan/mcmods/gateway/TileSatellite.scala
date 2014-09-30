package targetsan.mcmods.gateway

import net.minecraft.tileentity.TileEntity
import net.minecraft.util.ChunkCoordinates
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
class Cached[T](private val init: () => T) {
	private var value: Option[T] = None

	def get: T = {
		if (value.isEmpty)
			value = Some(init())
		value.get
	}

	def reset(): Unit =
	{
		value = None
	}
}

class TileSatellite extends TileEntity with ConnectorHost
	with FluidConnector
{
	//******************************************************************************************************************
	// Controlling cached references and notifying on their invalidation
	//******************************************************************************************************************
	def notifyPartnersOfTilesChanged(): Unit =
		Partners.get map {
			case (_, t) =>
				t.Partners.reset()
				t.LinkedTiles.reset()
		}
	// This one is invoked only from underlying block's onNeighborBlockChange
	def notifyPartnersOfRedstoneChanged(): Unit =
		Partners.get map { _._2.IncomingPower.reset() }

	override def onChunkUnload(): Unit =
	{
		notifyPartnersOfTilesChanged()
	}

	//******************************************************************************************************************
	// Redstone support
	// Not in trait because references IncomingPower
	//******************************************************************************************************************
	def getRedstoneStrongPower(side: ForgeDirection): Int =
		IncomingPower.get get side map { _._1 } getOrElse 0
	def getRedstoneWeakPower(side: ForgeDirection): Int =
		IncomingPower.get get side map { _._2 } getOrElse 0

	//******************************************************************************************************************
	// ConnectorHost support
	//******************************************************************************************************************
	def tiles = LinkedTiles.get

	//******************************************************************************************************************
	// Several lazy values which don't change during tile's lifetime, but cannot be initialized in constructor
	//******************************************************************************************************************

	private lazy val SatBlock = GatewayMod.BlockGateway.subBlock(getBlockMetadata).asInstanceOf[SubBlockSatellite]
	private def coreTile = worldObj.getTileEntity(xCoord - SatBlock.xOffset, yCoord, zCoord - SatBlock.zOffset).asInstanceOf[TileGateway]

	private lazy val LinkedSides =
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

	private lazy val LinkedWorld = coreTile.getExitWorld // Theoretically, doesn't change during single session
	private lazy val LinkedPartnerCoords =
		LinkedSides.map { side =>
			val linkedCorePos = coreTile.getExitPos
			(side, new ChunkCoordinates(linkedCorePos.posX - 2 * side.offsetX, linkedCorePos.posY, linkedCorePos.posZ - 2 * side.offsetZ))
		}
		.toMap

	private lazy val LinkedTileCoords =
		for ( (side, pos) <- LinkedPartnerCoords)
			yield (side, new ChunkCoordinates(pos.posX - side.offsetX, pos.posY - side.offsetY, pos.posZ - side.offsetZ) )

	//******************************************************************************************************************
	// Connector maps, lazily constructed
	// Should be invalidated when:
	// 1. Redstone on the other side changes
	// 2. Tile entity on the other
	//******************************************************************************************************************
	private val Partners = new Cached(
		() => // Did this as a for-comprehension for safety - no crashes,
			  // just connected tiles not accessible on any trouble {
			for {
				(side, pos) <- LinkedPartnerCoords
				tile <- LinkedWorld.getTileEntity(pos.posX, pos.posY, pos.posZ).as[TileSatellite]
			}
				yield (side, tile)
	)

	private val LinkedTiles = new Cached(
		() =>
			for {
				(side, pos) <- LinkedTileCoords
				tile <- Option(LinkedWorld.getTileEntity(pos.posX - side.offsetX, pos.posY, pos.posZ - side.offsetZ))
			}
				yield (side, tile)
		)

	private val IncomingPower = new Cached(
		() =>
			for {
				(side, sat) <- Partners.get
			}
				yield (side,
					(	sat.getWorldObj.isBlockProvidingPowerTo(sat.xCoord - side.offsetX, sat.yCoord, sat.zCoord - side.offsetZ, side.ordinal()),
						sat.getWorldObj.getIndirectPowerLevelTo(sat.xCoord - side.offsetX, sat.yCoord, sat.zCoord - side.offsetZ, side.ordinal())
					)
				)
	)
}
