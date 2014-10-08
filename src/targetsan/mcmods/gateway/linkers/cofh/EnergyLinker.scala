package targetsan.mcmods.gateway.linkers.cofh

import cofh.api.energy._
import cpw.mods.fml.common.Optional._
import net.minecraftforge.common.util.ForgeDirection
import targetsan.mcmods.gateway.TileLinker

@Interface(iface = "cofh.api.energy.IEnergyHandler", modid = "CoFHCore", striprefs = true)
trait EnergyLinker extends TileLinker with IEnergyHandler {
	// IEnergyConnection
	@Method(modid = "CoFHCore")
	override def canConnectEnergy(from: ForgeDirection): Boolean =
		tileAs[IEnergyConnection](from) map { _.canConnectEnergy(from) } getOrElse false

	// Former IEnergyHandler, IEnergyProvider/IEnergyReceiver latest
	// TODO: adapt to future IEnergyProvider/IEnergyReceiver duality
	@Method(modid = "CoFHCore")
	override def getEnergyStored(from: ForgeDirection): Int =
		tileAs[IEnergyHandler](from) map { _.getEnergyStored(from) } getOrElse 0

	// TODO: adapt to future IEnergyProvider/IEnergyReceiver duality
	@Method(modid = "CoFHCore")
	override def getMaxEnergyStored(from: ForgeDirection): Int =
		tileAs[IEnergyHandler](from) map { _.getMaxEnergyStored(from) } getOrElse 0

	// IEnergyHandler before, IEnergyReceiver now
	// TODO: adapt to future IEnergyProvider/IEnergyReceiver duality
	@Method(modid = "CoFHCore")
	override def receiveEnergy(from: ForgeDirection, maxReceive: Int, simulate: Boolean): Int =
		tileAs[IEnergyHandler](from) map { _.receiveEnergy(from, maxReceive, simulate) } getOrElse 0

	// IEnergyHandler before, IEnergyProvider now
	// TODO: adapt to future IEnergyProvider/IEnergyReceiver duality
	@Method(modid = "CoFHCore")
	override def extractEnergy(from: ForgeDirection, maxExtract: Int, simulate: Boolean): Int =
		tileAs[IEnergyHandler](from) map { _.extractEnergy(from, maxExtract, simulate) } getOrElse 0

}
