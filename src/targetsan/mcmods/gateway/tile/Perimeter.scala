package targetsan.mcmods.gateway.tile

import net.minecraft.init.{Blocks, Items}
import targetsan.mcmods.gateway._
import targetsan.mcmods.gateway.Utils._

import net.minecraft.entity.player.EntityPlayer
import net.minecraftforge.common.util.ForgeDirection

class Perimeter extends Gateway {
	//******************************************************************************************************************
	// Some calculated fields
	//******************************************************************************************************************
	private lazy val CorePos =
		block.Multiblock.Parts // Index by block type plus meta?
			.find { elem => elem.block == Assets.BlockPlatform && elem.meta == tileMeta }
			.map { BlockPos(this) - _.offset }
	private def coreTile = CorePos flatMap { p => worldObj.getTileEntity(p.x, p.y, p.z).as[Core] }

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
		// TODO: relay to linked tile, when linking is implemented
	}
	override def onNeighborTileChanged(tx: Int, ty: Int, tz: Int): Unit = {
		super.onNeighborTileChanged(tx, ty, tz)
		if (!isAlive) return
		// TODO: relay to linked tile, when linking is implemented
	}
}
