package targetsan.mcmods.gateway.tile

import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.util.ForgeDirection
import targetsan.mcmods.gateway.Utils._

abstract class Gateway extends TileEntity
{
	//******************************************************************************************************************
	// State flags operation
	// Only field persistence and helper functions
	// Bit fields allocation should be done in child classes
	//******************************************************************************************************************
	private var stateFlags = 0
	private val STATE_FLAGS_TAG = "state-flags"

	// These two get/set bits on stateFlags
	protected def getState(offset: Int, size: Int) = getBits(stateFlags, offset, size)
	protected def setState(offset: Int, size: Int, value: Int) {
		stateFlags = setBits(stateFlags, offset, size, value)
	}
	// Shortcut funcs, they use 1 bit and treat ti as boolean
	protected def getFlag(offset: Int) = getState(offset, 1) != 0
	protected def setFlag(offset: Int, value: Boolean) = setState(offset, 1, if (value) 1 else 0)

	// Multiblock control
	def isAssembled: Boolean
	protected[tile] def isAssembled_= (value: Boolean): Unit

	override def readFromNBT(tag: NBTTagCompound): Unit = {
		super.readFromNBT(tag)
		stateFlags = tag.getInteger(STATE_FLAGS_TAG)
	}

	override def writeToNBT(tag: NBTTagCompound): Unit = {
		super.writeToNBT(tag)
		tag.setInteger(STATE_FLAGS_TAG, stateFlags)
	}
	//******************************************************************************************************************
	// Operations for non-center gateway platform tiles
	//******************************************************************************************************************
	def onActivated(player: EntityPlayer, side: ForgeDirection) {}
	def onNeighborBlockChanged() {}
	def onNeighborTileChanged(tx: Int, ty: Int, tz: Int) {}
	//******************************************************************************************************************
	// Teleporter, for core tile mostly
	//******************************************************************************************************************
	def teleport(entity: Entity) { }
}
