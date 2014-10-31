package targetsan.mcmods.gateway.tile.linkers

import net.minecraftforge.common.util.ForgeDirection
import net.minecraftforge.fluids.{FluidStack, Fluid, FluidTankInfo, IFluidHandler}
import targetsan.mcmods.gateway._

trait FluidHandlerLinker extends tile.Linker with IFluidHandler {
	override def fill(from: ForgeDirection, resource: FluidStack, doFill: Boolean): Int =
		linkedTileAs[IFluidHandler](from) map { _.fill(from, resource, doFill) } getOrElse 0

	override def drain(from: ForgeDirection, resource: FluidStack, doDrain: Boolean): FluidStack =
		linkedTileAs[IFluidHandler](from) map { _.drain(from, resource, doDrain) } getOrElse null

	override def drain(from: ForgeDirection, maxDrain: Int, doDrain: Boolean): FluidStack =
		linkedTileAs[IFluidHandler](from) map { _.drain(from, maxDrain, doDrain) } getOrElse null

	override def canFill(from: ForgeDirection, fluid: Fluid): Boolean =
		linkedTileAs[IFluidHandler](from) map { _.canFill(from, fluid) } getOrElse false

	override def canDrain(from: ForgeDirection, fluid: Fluid): Boolean =
		linkedTileAs[IFluidHandler](from) map { _.canDrain(from, fluid) } getOrElse false

	override def getTankInfo(from: ForgeDirection): Array[FluidTankInfo] =
		linkedTileAs[IFluidHandler](from) map { _.getTankInfo(from) } getOrElse Array.empty[FluidTankInfo]
}
