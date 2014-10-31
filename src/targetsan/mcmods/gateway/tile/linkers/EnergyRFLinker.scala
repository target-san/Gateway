package targetsan.mcmods.gateway.tile.linkers

import cofh.api.energy.{IEnergyConnection, IEnergyHandler}
import cpw.mods.fml.common.Optional.{Method, Interface}
import net.minecraftforge.common.util.ForgeDirection
import targetsan.mcmods.gateway._

@Interface( iface = "cofh.api.energy.IEnergyHandler", modid = "CoFHCore", striprefs = true)
trait EnergyRFLinker extends tile.Linker with IEnergyHandler {
	@Method(modid = "CoFHCore")
	override def extractEnergy(from: ForgeDirection, value: Int, simulate: Boolean): Int =
		linkedTileAs[IEnergyHandler](from) map { _.extractEnergy(from, value, simulate) } getOrElse 0

	@Method(modid = "CoFHCore")
	override def getEnergyStored(from: ForgeDirection): Int =
		linkedTileAs[IEnergyHandler](from) map { _.getEnergyStored(from) } getOrElse 0

	@Method(modid = "CoFHCore")
	override def getMaxEnergyStored(from: ForgeDirection): Int =
		linkedTileAs[IEnergyHandler](from) map { _.getMaxEnergyStored(from) } getOrElse 0

	@Method(modid = "CoFHCore")
	override def receiveEnergy(from: ForgeDirection, value: Int, simulate: Boolean): Int =
		linkedTileAs[IEnergyHandler](from) map { _.receiveEnergy(from, value, simulate) } getOrElse 0

	@Method(modid = "CoFHCore")
	override def canConnectEnergy(from: ForgeDirection): Boolean =
		linkedTileAs[IEnergyConnection](from) map { _.canConnectEnergy(from) } getOrElse false
}
