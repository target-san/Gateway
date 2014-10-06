package targetsan.mcmods.gateway.linkers

import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.ForgeDirection
import targetsan.mcmods.gateway.Cached
import targetsan.mcmods.gateway.Utils._

trait RedstoneLinker {

	//******************************************************************************************************************
	// Redstone output
	//******************************************************************************************************************
	def getRedstoneStrongPower(side: ForgeDirection): Int =
		RedstoneOutputs get side map { _.get._1 } getOrElse 0
	def getRedstoneWeakPower(side: ForgeDirection): Int =
		RedstoneOutputs get side map { _.get._2 } getOrElse 0
	// Invalidated whenever partner unloads
	private lazy val RedstonePartners =
		for ( (side, pos) <- linkedPartnerCoords)
			yield (side,
				new Cached( ()=>
					pos.world.getTileEntity(pos.x, pos.y, pos.z).as[RedstoneLinker]
				)
			)
	// invalidated whenever partner's neighbor changes
	private lazy val RedstoneOutputs =
		for ( (side, partner) <- RedstonePartners)
			yield (side,
				new Cached( () =>
					partner.get map { _.getPowerForSide(side.getOpposite) } getOrElse (0, 0)
				)
			)

	//******************************************************************************************************************
	// Overridables, needed for the whole hell to work
	//******************************************************************************************************************
	protected def linkedSides: Seq[ForgeDirection]
	protected def thisBlock: BlockPos
	protected def linkedPartnerCoords: Map[ForgeDirection, BlockPos]

	//******************************************************************************************************************
	// Internal API, used in TE
	//******************************************************************************************************************
	// Read incoming signal for specified side
	protected def readPowerInput(side: ForgeDirection): Unit =
		if (linkedSides contains side)
			setPowerForSide(
				side,
				thisBlock.world.isBlockProvidingPowerTo(thisBlock.x + side.offsetX, thisBlock.y + side.offsetY, thisBlock.z + side.offsetZ, side.getOpposite.ordinal()),
				thisBlock.world.getIndirectPowerLevelTo(thisBlock.x + side.offsetX, thisBlock.y + side.offsetY, thisBlock.z + side.offsetZ, side.getOpposite.ordinal())
			)
	// Invoke from onPartherNeighborChanged
	protected def onPartnerRedstoneChanged(side: ForgeDirection): Unit =
		RedstoneOutputs get side foreach { _.reset() }
	// Invoke from partner's onChunkUnload
	protected def onPartnerUnload(side: ForgeDirection): Unit =
		RedstonePartners get side foreach { _.reset() }
	// Call this from writeToNBT
	protected def saveRedstone(tag: NBTTagCompound): Unit = {
		tag.setInteger(InputNBTName, inputs)
	}
	// Call this from readFromNBT
	protected def loadRedstone(tag: NBTTagCompound): Unit = {
		inputs = tag.getInteger(InputNBTName)
	}

	//******************************************************************************************************************
	// NBT persistence
	//******************************************************************************************************************

	private val InputNBTName = "redstoneInputs"
	// Incoming power
	private var inputs = 0

	private def getPowerForSide(side: ForgeDirection): (Int, Int) = {
		val index = linkedSides indexOf side
		if (index < 0)
			return (0, 0)

		val powerByte = (inputs >> (index * 8) ) & 0xFF
		( (powerByte >> 4) & 0x0F, powerByte & 0x0F)
	}

	private def setPowerForSide(side: ForgeDirection, strong: Int, weak: Int): Unit = {
		val index = linkedSides indexOf side
		if (index < 0)
			return

		val powerByte = ((strong & 0x0F) << 4) | (weak & 0x0F)
		val powerMask = 0xFF << (index * 8)
		inputs = inputs & ~powerMask | (powerByte << (index * 8) )
	}
}
