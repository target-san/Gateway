package targetsan.mcmods.gateway

import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.chunk.Chunk
import net.minecraftforge.common.util.ForgeDirection

import targetsan.mcmods.gateway.Utils._
import targetsan.mcmods.gateway.linkers._

import scala.reflect.ClassTag

trait TileLinker {
	def tileAs[T: ClassTag](side: ForgeDirection): Option[T]
}

/** Resettable lazy value
 *  Used to retrieve connected tiles only when they're invalidated
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
		watchers foreach { _._2() }
		// Leave only loaded watchers in list
		watched += (pos -> watchers)
	}
}

class TileSatellite extends TileEntity with TileLinker
	with RedstoneLinker // semi-intrusive, requires minor coupling
	with FluidLinker
{
	//******************************************************************************************************************
	// Satellite's context, lazily resolved, not persisted
	//******************************************************************************************************************

	// Satellite block type
	// context-internal
	private lazy val SatBlock = GatewayMod.BlockGateway.subBlock(getBlockMetadata).asInstanceOf[SubBlockSatellite]
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
	// Coordinates of partner satellites linked to this one, mapped by side
	private lazy val LinkedPartnerCoords =
		LinkedSides.map { side =>
			val coreTile = worldObj.getTileEntity(xCoord - SatBlock.xOffset, yCoord, zCoord - SatBlock.zOffset).asInstanceOf[TileGateway]
			val linkedCorePos = coreTile.getExitPos
			val linkedWorld = coreTile.getExitWorld
			(side, new BlockPos(linkedCorePos.posX + SatBlock.xOffset - 2 * side.offsetX, linkedCorePos.posY, linkedCorePos.posZ + SatBlock.zOffset - 2 * side.offsetZ, linkedWorld))
		}
			.toMap
	// Coordinates of directly linked tile entities
	private lazy val LinkedTileCoords =
		for ( (side, pos) <- LinkedPartnerCoords)
		yield (side, new BlockPos(pos.x - side.offsetX, pos.y - side.offsetY, pos.z - side.offsetZ, pos.world) )

	//******************************************************************************************************************
	// Controlling cached references and notifying on their invalidation
	//******************************************************************************************************************

	private def loadedPartners =
		for {
			(side, pos) <- LinkedPartnerCoords
			if pos.world.blockExists(pos.x, pos.y, pos.z)
			tile <- pos.world.getTileEntity(pos.x, pos.y, pos.z).as[TileSatellite]
		}
			yield (side, tile)
	// One of neighbor blocks has changed
	// Check all loaded partners and notify them
	def onNeighborChanged(): Unit =
		if (!worldObj.isRemote) {
			// Re-read redstone inputs
			for (side <- LinkedSides)
				readPowerInput(side)
			// Notify all loaded partners about change
			for ( (side, tile) <- loadedPartners )
				tile.onPartnerNeighborChanged(side.getOpposite)
		}
	// Specified sided partner's neighbor has changed; partner function for onNeighborChanged
	private def onPartnerNeighborChanged(side: ForgeDirection): Unit = {
		onPartnerRedstoneChanged(side) // RedstoneLinker
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
		// RedstoneLinker: notify loaded partners that they would need to re-read partner reference
		for ( (side, tile) <- loadedPartners )
			tile.onPartnerUnload(side.getOpposite)
	}

	// This one is called only on first attempt to get linked tile
	private def watchLinkedTiles(): Unit =
		if (!worldObj.isRemote)
		for ( (side, pos) <- LinkedTileCoords)
			ChunkWatcher.watchBlock(
				pos,
				new BlockPos(this),
				() => LinkedTiles get side foreach { _.reset() }
			)
	// Called when this TE is unloaded or invalidated
	private def unwatchLinkedTiles(): Unit =
		if (!worldObj.isRemote)
			ChunkWatcher unwatch new BlockPos(this)

	//******************************************************************************************************************
	// State persistence
	//******************************************************************************************************************
	override def readFromNBT(tag: NBTTagCompound): Unit = {
		super.readFromNBT(tag)
		loadRedstone(tag) // RedstoneLinker
	}

	override def writeToNBT(tag: NBTTagCompound): Unit = {
		super.writeToNBT(tag)
		saveRedstone(tag) // RedstoneLinker
	}

	//******************************************************************************************************************
	// TileLinker support
	//******************************************************************************************************************
	def tileAs[T: ClassTag](side: ForgeDirection): Option[T] = LinkedTiles get side flatMap { _.get } flatMap { _.as[T] }

	//******************************************************************************************************************
	// RedstoneLinker support
	//******************************************************************************************************************

	override protected def linkedSides = LinkedSides

	private lazy val ThisBlockPos = BlockPos(xCoord, yCoord, zCoord, worldObj)
	override protected def thisBlock = ThisBlockPos
	override protected def linkedPartnerCoords = LinkedPartnerCoords

	//******************************************************************************************************************
	// Connector maps, lazily constructed
	//******************************************************************************************************************

	// Caches references to connected tiles. Bypasses partner satellites
	private lazy val LinkedTiles = {
		watchLinkedTiles() // watch for linked tiles' chunks, needed only once per this tile's lifecycle
		for ((side, pos) <- LinkedTileCoords)
			yield (side,
				new Cached(
					() => Option(pos.world.getTileEntity(pos.x - side.offsetX, pos.y, pos.z - side.offsetZ))
				)
			)
	}
}
