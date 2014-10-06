package targetsan.mcmods.gateway.linkers

import net.minecraftforge.common.util.ForgeDirection
import net.minecraftforge.fluids.{Fluid, FluidStack, FluidTankInfo, IFluidHandler}
import targetsan.mcmods.gateway.TileLinker

trait FluidLinker extends TileLinker with IFluidHandler {
	override def drain(from: ForgeDirection, resource: FluidStack, doDrain: Boolean): FluidStack =
		tileAs[IFluidHandler](from) map { _.drain(from, resource, doDrain) } getOrElse null

	override def drain(from: ForgeDirection, maxDrain: Int, doDrain: Boolean): FluidStack =
		tileAs[IFluidHandler](from) map { _.drain(from, maxDrain, doDrain) } getOrElse null

	override def canFill(from: ForgeDirection, fluid: Fluid): Boolean =
		tileAs[IFluidHandler](from) map { _.canFill(from, fluid) } getOrElse false

	override def canDrain(from: ForgeDirection, fluid: Fluid): Boolean =
		tileAs[IFluidHandler](from) map { _.canDrain(from, fluid) } getOrElse false

	override def fill(from: ForgeDirection, resource: FluidStack, doFill: Boolean): Int =
		tileAs[IFluidHandler](from) map { _.fill(from, resource, doFill) } getOrElse 0

	override def getTankInfo(from: ForgeDirection): Array[FluidTankInfo] =
		tileAs[IFluidHandler](from) map { _.getTankInfo(from) } getOrElse Array.empty[FluidTankInfo]
}
