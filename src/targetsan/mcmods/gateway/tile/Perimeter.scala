package targetsan.mcmods.gateway.tile

import targetsan.mcmods.gateway
import targetsan.mcmods.gateway._
import targetsan.mcmods.gateway.Utils._

import net.minecraft.entity.player.EntityPlayer
import net.minecraftforge.common.util.ForgeDirection
import targetsan.mcmods.gateway.block.Multiblock

class Perimeter extends Gateway {
	//******************************************************************************************************************
	// Some calculated fields
	//******************************************************************************************************************
	private lazy val CorePos =
		Multiblock.Parts
			.find { elem => elem._2._1 == Assets.BlockPlatform && elem._2._2 == tileSide }
			.map { BlockPos(this) - _._1 }

	//******************************************************************************************************************
	// State flag field parts
	//******************************************************************************************************************
	// Multiblock assembled marker
	// size: 1 offset: 0
	override def isAssembled = getFlag(0)
	override def isAssembled_= (value: Boolean): Unit = {
		setFlag(0, value)
		if (value) // if we're assembling, then read metadata and store for future use
			setState(1, 3, getBlockMetadata - 1)
	}

	private def tileSide = getState(1, 3) + 1
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
		for {
			pos <- CorePos
			tile <- worldObj.getTileEntity(pos.x, pos.y, pos.z).as[tile.Core]
		}
			tile.invalidate()
	}
	//******************************************************************************************************************
	// Logic overrides
	//******************************************************************************************************************
	override def onActivated(player: EntityPlayer, side: ForgeDirection): Unit = {
		super.onActivated(player, side)
		if (!isAlive) return
	}
	override def onNeighborBlockChanged(): Unit = {
		super.onNeighborBlockChanged()
		if (!isAlive) return
	}
	override def onNeighborTileChanged(tx: Int, ty: Int, tz: Int): Unit = {
		super.onNeighborTileChanged(tx, ty, tz)
		if (!isAlive) return
	}
}
