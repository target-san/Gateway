package targetsan.mcmods.gateway.linkers

import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.ForgeDirection
import targetsan.mcmods.gateway.Utils

trait RedstoneLinker {

	//******************************************************************************************************************
	// Public API
	//******************************************************************************************************************
	def getRedstoneStrongPower(side: ForgeDirection): Int =
		getPowerForSide(side)._1
	def getRedstoneWeakPower(side: ForgeDirection): Int =
		getPowerForSide(side)._2

	//******************************************************************************************************************
	// Overridables, needed for the whole hell to work
	//******************************************************************************************************************
	protected def linkedSides: Seq[ForgeDirection]
	protected def linkedTileCoords: Map[ForgeDirection, Utils.BlockPos]

	//******************************************************************************************************************
	// Internal API, used in TE
	//******************************************************************************************************************
	protected def saveRedstone(tag: NBTTagCompound): Unit = {
		tag.setInteger("redstoneOutputs", outputs)
	}

	protected def loadRedstone(tag: NBTTagCompound): Unit = {
		outputs = tag.getInteger("redstoneOutputs")
	}

	protected def readPartnerInput(side: ForgeDirection): Unit =
		linkedTileCoords get side foreach { pos =>
			setPowerForSide(
				side,
				pos.world.isBlockProvidingPowerTo(pos.x, pos.y, pos.z, side.ordinal()),
				pos.world.getIndirectPowerLevelTo(pos.x, pos.y, pos.z, side.ordinal())
			)
		}

	//******************************************************************************************************************
	// Storage
	//******************************************************************************************************************

	private var outputs = 0

	private def getPowerForSide(side: ForgeDirection): (Int, Int) = {
		val index = linkedSides indexOf side
		if (index < 0)
			return (0, 0)

		val powerByte = (outputs >> (index * 8) ) & 0xFF
		( (powerByte >> 4) & 0x0F, powerByte & 0x0F)
	}

	private def setPowerForSide(side: ForgeDirection, strong: Int, weak: Int): Unit = {
		val index = linkedSides indexOf side
		if (index < 0)
			return

		val powerByte = ((strong & 0x0F) << 4) | (weak & 0x0F)
		val powerMask = 0xFF << (index * 8)
		outputs = outputs & ~powerMask | (powerByte << (index * 8) )
	}
}
