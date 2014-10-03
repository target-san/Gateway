package targetsan.mcmods.gateway

import net.minecraft.tileentity.TileEntity
import net.minecraft.util.ChunkCoordinates
import net.minecraft.world.chunk.Chunk
import net.minecraftforge.common.util.ForgeDirection
import scala.collection.mutable
import scala.reflect.ClassTag
import targetsan.mcmods.gateway.connectors._
import Utils._

import scala.util.Try

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

object ChunkWatcher {
	private var watched = Map.empty[ChunkPos, Set[(BlockPos, () => Unit)]]

	def watchBlock(block: BlockPos, watcher: BlockPos, onUnload: () => Unit): Unit =
		watched += (block.chunk -> (watched.getOrElse(block.chunk, Set.empty) + (watcher -> onUnload)) )

	def unwatch(watcher: BlockPos): Unit =
		watched = watched mapValues { _ filter { _._1 != watcher} } filter { _._2.nonEmpty }

	def onChunkUnload(chunk: Chunk): Unit = {
		val pos = new ChunkPos(chunk)
		// Get list of loaded watchers
		val watchers = watched.getOrElse(pos, Set.empty) filter {
				case (block, _) => block.world.blockExists(block.x, block.y, block.z)
			}
		// Notify all of them that their watched chunk is unloaded
		for ( (watcher, onUnload) <- watchers )
			onUnload()
		// Leave only loaded watchers in list
		watched += (pos -> watchers)
	}
}

class TileSatellite extends TileEntity with ConnectorHost
	with FluidConnector
{
	//******************************************************************************************************************
	// Satellite's context, lazily resolved, not persisted
	//******************************************************************************************************************

	// Satellite block type
	// context-internal
	private lazy val SatBlock = GatewayMod.BlockGateway.subBlock(getBlockMetadata).asInstanceOf[SubBlockSatellite]
	// Core TE's reference, not stored at the moment
	// context-internal
	private def coreTile = worldObj.getTileEntity(xCoord - SatBlock.xOffset, yCoord, zCoord - SatBlock.zOffset).asInstanceOf[TileGateway]

	// Context values used in other parts and displayed outside

	// Sides which link through gateway
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
	// Destination world for this gateway
	private lazy val LinkedWorld = coreTile.getExitWorld // Theoretically, doesn't change during single session
	// Coordinates of partner satellites linked to this one, mapped by side
	private lazy val LinkedPartnerCoords =
		LinkedSides.map { side =>
			val linkedCorePos = coreTile.getExitPos
			(side, new ChunkCoordinates(linkedCorePos.posX + SatBlock.xOffset - 2 * side.offsetX, linkedCorePos.posY, linkedCorePos.posZ + SatBlock.zOffset - 2 * side.offsetZ))
		}
			.toMap
	// Coordinates of directly linked tile entities
	private lazy val LinkedTileCoords =
		for ( (side, pos) <- LinkedPartnerCoords)
		yield (side, new ChunkCoordinates(pos.posX - side.offsetX, pos.posY - side.offsetY, pos.posZ - side.offsetZ) )

	//******************************************************************************************************************
	// Controlling cached references and notifying on their invalidation
	//******************************************************************************************************************

	// One of neighbor blocks has changed
	// Check all loaded partners and notify them
	def onNeighborChanged(): Unit =
		if (!worldObj.isRemote)
		for {
			(side, pos) <- LinkedPartnerCoords
			if LinkedWorld.blockExists(pos.posX, pos.posY, pos.posZ)
			tile <- LinkedWorld.getTileEntity(pos.posX, pos.posY, pos.posZ).as[TileSatellite]
		}
			tile.onPartnerNeighborChanged(side.getOpposite)
	// Specified sided partner's neighbor has changed; partner function for onNeighborChanged
	private def onPartnerNeighborChanged(side: ForgeDirection): Unit = {
		LinkedTiles get side foreach { _.reset() }
		// Transfer change notification to corresponding linked block
		worldObj.notifyBlockOfNeighborChange(xCoord + side.offsetX, yCoord + side.offsetY, zCoord + side.offsetZ, getBlockType)
	}

	override def invalidate(): Unit = {
		super.invalidate()
		unwatchLinkedTiles()
	}
	// Remove this tile from chunk watchers
	override def onChunkUnload(): Unit = {
		super.onChunkUnload()
		unwatchLinkedTiles()
	}

	// This one is called only on first attempt to get linked tile
	private def watchLinkedTiles(): Unit =
		for ( (side, pos) <- LinkedTileCoords)
			ChunkWatcher.watchBlock(
				new BlockPos(pos, LinkedWorld),
				new BlockPos(this),
				() => LinkedTiles get side foreach { _.reset() }
			)
	// Called when this TE is unloaded or invalidated
	private def unwatchLinkedTiles(): Unit = {
		ChunkWatcher unwatch new BlockPos(this)
	}

	//******************************************************************************************************************
	// Redstone support
	// Not in trait because references IncomingPower
	//******************************************************************************************************************
	def getRedstoneStrongPower(side: ForgeDirection): Int = 0
	def getRedstoneWeakPower(side: ForgeDirection): Int = 0

	//******************************************************************************************************************
	// ConnectorHost support
	//******************************************************************************************************************
	def linkedSides: Seq[ForgeDirection] = LinkedSides
	def linkedTile(side: ForgeDirection) = LinkedTiles get side flatMap { _.get }

	//******************************************************************************************************************
	// Connector maps, lazily constructed
	//******************************************************************************************************************

	// Caches references to connected tiles. Bypasses partner satellites
	private lazy val LinkedTiles = {
		watchLinkedTiles() // watch for linked tiles' chunks, needed only once per this tile's lifecycle
		for ((side, pos) <- LinkedTileCoords)
			yield (side,
				new Cached(
					() => Option(LinkedWorld.getTileEntity(pos.posX - side.offsetX, pos.posY, pos.posZ - side.offsetZ))
				)
			)
	}
}
