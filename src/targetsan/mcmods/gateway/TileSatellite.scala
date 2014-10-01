package targetsan.mcmods.gateway

import net.minecraft.tileentity.TileEntity
import net.minecraft.util.ChunkCoordinates
import net.minecraftforge.common.util.ForgeDirection
import scala.reflect.ClassTag
import targetsan.mcmods.gateway.connectors._
import Utils._

trait ConnectorHost {
	def linkedSides: Seq[ForgeDirection]

	def linkedTile(side: ForgeDirection): Option[TileEntity]

	def linkedTileAs[T: ClassTag](side: ForgeDirection): Option[T] =
		linkedTile(side) flatMap { _.as[T] }
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
	def onNeighborChanged(): Unit =
		loadedPartners foreach {
			t =>
				t._2.LinkedTiles.reset()
				t._2.IncomingPower.reset()
		}
	// Flushes cached tile references, when any of them is unloaded
	// TODO: more fine-grained control over what's flushed here
	def onWatchedChunkUnload(): Unit = {
		LinkedTiles.reset()
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
	def linkedSides: Seq[ForgeDirection] = LinkedSides
	def linkedTile(side: ForgeDirection) = LinkedTiles.get get side

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
			(side, new ChunkCoordinates(linkedCorePos.posX + SatBlock.xOffset - 2 * side.offsetX, linkedCorePos.posY, linkedCorePos.posZ + SatBlock.zOffset - 2 * side.offsetZ))
		}
		.toMap

	private lazy val LinkedTileCoords =
		for ( (side, pos) <- LinkedPartnerCoords)
			yield (side, new ChunkCoordinates(pos.posX - side.offsetX, pos.posY - side.offsetY, pos.posZ - side.offsetZ) )
	// All partner tiles which are loaded at the moment
	// Used for lazy notification
	// There's no sense in notifying unloaded partners about cache flush
	private def loadedPartners =
		for {
			(side, pos) <- LinkedPartnerCoords
			if LinkedWorld.blockExists(pos.posX, pos.posY, pos.posZ)
			tile <- LinkedWorld.getTileEntity(pos.posX, pos.posY, pos.posZ).as[TileSatellite]
		}
			yield (side, tile)

	//******************************************************************************************************************
	// Connector maps, lazily constructed
	//******************************************************************************************************************

	// Caches references to connected tiles. Bypasses partner satellites
	private val LinkedTiles = new Cached(
		() =>
			for {
				(side, pos) <- LinkedTileCoords
				tile <- Option(LinkedWorld.getTileEntity(pos.posX - side.offsetX, pos.posY, pos.posZ - side.offsetZ))
			}
				yield (side, tile)
		)
	// Caches values of incoming redstone power
	private val IncomingPower = new Cached(
		() =>
			for ( (side, pos) <- LinkedTileCoords )
				yield (side,
					(	LinkedWorld.isBlockProvidingPowerTo(pos.posX, pos.posY, pos.posZ, side.ordinal()),
						LinkedWorld.getIndirectPowerLevelTo(pos.posX, pos.posY, pos.posZ, side.ordinal())
					)
				)
		)
}
