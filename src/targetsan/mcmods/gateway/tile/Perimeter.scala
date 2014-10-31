package targetsan.mcmods.gateway.tile

import net.minecraft.block.Block
import net.minecraft.init.{Blocks, Items}
import net.minecraft.world.World
import targetsan.mcmods.gateway._
import targetsan.mcmods.gateway.Utils._

import net.minecraft.entity.player.EntityPlayer
import net.minecraftforge.common.util.ForgeDirection

import scala.reflect.ClassTag

class Perimeter extends Gateway with Linker{
	//******************************************************************************************************************
	// Some calculated fields
	//******************************************************************************************************************
	private lazy val ThisOffset =
		block.Multiblock.Parts // Index by block type plus meta?
			.find { elem => elem._2.block == Assets.BlockPlatform && elem._2.meta == tileMeta }
			.map { _._1}
			.get
	private lazy val CorePos = BlockPos(this) - ThisOffset
	private def coreTile = worldObj.getTileEntity(CorePos.x, CorePos.y, CorePos.z).as[Core]

	private lazy val LinkedPositions =
		List (
			ThisOffset.x match {
				case -1 => ForgeDirection.WEST
				case  1 => ForgeDirection.EAST
				case _ => ForgeDirection.UNKNOWN
			},
			ThisOffset.z match {
				case -1 => ForgeDirection.NORTH
				case  1 => ForgeDirection.SOUTH
				case _ => ForgeDirection.UNKNOWN
			}
		)
		.filter { _ != ForgeDirection.UNKNOWN }
		.map { s => (s, ThisOffset - BlockPos(s) * 3 + coreTile.get.getPartnerPos) }
		.toMap

	private lazy val LinkedWorld = coreTile.get.getPartnerWorld

	//******************************************************************************************************************
	// Linker
	//******************************************************************************************************************
	def linkedBlockAs[T: ClassTag](side: ForgeDirection): Option[(T, World, BlockPos)] =
		if (!isAlive) None
		else LinkedPositions
			.get(side)
			.flatMap { pos =>
				LinkedWorld
					.getBlock(pos.x, pos.y, pos.z).as[T]
					.map { (_, LinkedWorld, pos)}
			}

	// Returns linked tile entity if it's presend and of the required type
	def linkedTileAs[T: ClassTag](side: ForgeDirection): Option[T] =
		if (!isAlive) None
		else LinkedPositions get side flatMap { p => LinkedWorld.getTileEntity(p.x, p.y, p.z).as[T] }
	//******************************************************************************************************************
	// State flag field parts
	//******************************************************************************************************************
	// Multiblock assembled marker
	// size: 1 offset: 0
	override def isAssembled = getFlag(0)
	override def isAssembled_= (value: Boolean): Unit = {
		setFlag(0, value)
		if (value) // if we're assembling, then read metadata and store for future use
			setState(4, 4, getBlockMetadata)
	}

	private def tileMeta = getState(4, 4)
	private def isAlive = isAssembled && !worldObj.isRemote

	//******************************************************************************************************************
	// Lifetime
	//******************************************************************************************************************
	override def invalidate(): Unit = {
		super.invalidate()
		if (!isAlive) return
		// This one isn't a part of multiblock anymore
		isAssembled = false
		// Invalidate core, so we're starting disassembly sequence
		coreTile foreach { _.invalidate() }
	}
	//******************************************************************************************************************
	// Logic overrides
	//******************************************************************************************************************
	override def onActivated(player: EntityPlayer, side: ForgeDirection): Unit = {
		super.onActivated(player, side)
		if (!isAlive) return

		if (player != null)
		if (player.getHeldItem != null)
		if (player.getHeldItem.getItem == Items.flint_and_steel)
			coreTile foreach { _.startDisposeFrom(player, BlockPos(this)) }
	}

	override def onNeighborBlockChanged(): Unit = {
		super.onNeighborBlockChanged()
		if (!isAlive) return

		if (worldObj.getBlock(xCoord, yCoord + 1, zCoord) != Blocks.fire)
			coreTile foreach { _.stopDisposeFrom(BlockPos(this)) }

		for {
			(side, _) <- LinkedPositions
			(block, world, pos) <- linkedBlockAs[Block](side)
		}
			block.onNeighborBlockChange(world, pos.x, pos.y, pos.z, Assets.BlockPlatform)
	}

	override def onNeighborTileChanged(tx: Int, ty: Int, tz: Int): Unit = {
		super.onNeighborTileChanged(tx, ty, tz)
		if (!isAlive) return
		// Relay this event to linked block
		val side = offsetToDirection(BlockPos(tx, ty, tz) - BlockPos(this))

		for ((block, world, pos) <- linkedBlockAs[Block](side))
		{
			val pos1 = pos + BlockPos(side)
			block.onNeighborChange(world, pos.x, pos.y, pos.z, pos1.x, pos1.y, pos1.z)
		}
	}
}
