package targetsan.mcmods.gateway.connectors

import net.minecraftforge.common.util.ForgeDirection
import net.minecraftforge.fluids.{FluidStack, Fluid, FluidTankInfo, IFluidHandler}
import targetsan.mcmods.gateway.ConnectorHost

/** Satellite connector which provides interaction with adjacent fluid containers
  * Method invocations are redirected to tile adjacent to partner connector satellite
 */
trait FluidConnector extends ConnectorHost with IFluidHandler {
	def fill(from: ForgeDirection, resource: FluidStack, doFill: Boolean): Int =
		linkedTileAs[IFluidHandler](from) map { _.fill(from, resource, doFill) } getOrElse 0

	def drain(from: ForgeDirection, resource: FluidStack, doDrain: Boolean): FluidStack =
		linkedTileAs[IFluidHandler](from) map { _.drain(from, resource, doDrain) } getOrElse null

	def drain(from: ForgeDirection, maxDrain: Int, doDrain: Boolean): FluidStack =
		linkedTileAs[IFluidHandler](from) map { _.drain(from, maxDrain, doDrain) } getOrElse null

	def canFill(from: ForgeDirection, fluid: Fluid): Boolean =
		linkedTileAs[IFluidHandler](from) map { _.canFill(from, fluid) } getOrElse false

	def canDrain(from: ForgeDirection, fluid: Fluid): Boolean =
		linkedTileAs[IFluidHandler](from) map { _.canDrain(from, fluid) } getOrElse false

	def getTankInfo(from: ForgeDirection): Array[FluidTankInfo] =
		linkedTileAs[IFluidHandler](from) map { _.getTankInfo(from) } getOrElse Array.empty[FluidTankInfo]
}
