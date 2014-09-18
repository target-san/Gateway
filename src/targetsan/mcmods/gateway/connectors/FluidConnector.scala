package targetsan.mcmods.gateway.connectors

import net.minecraftforge.common.util.ForgeDirection
import net.minecraftforge.fluids.{FluidStack, Fluid, FluidTankInfo, IFluidHandler}
import targetsan.mcmods.gateway.ConnectorHost

/** Satellite connector which provides interaction with adjacent fluid containers
  * Method invocations are redirected to tile adjacent to partner connector satellite
 */
trait FluidConnector extends ConnectorHost with IFluidHandler {
	override def fill(from: ForgeDirection, resource: FluidStack, doFill: Boolean): Int =
		typedTile[IFluidHandler](from) map { _.fill(from, resource, doFill) } getOrElse 0

	override def drain(from: ForgeDirection, resource: FluidStack, doDrain: Boolean): FluidStack =
		typedTile[IFluidHandler](from) map { _.drain(from, resource, doDrain) } getOrElse null

	override def drain(from: ForgeDirection, maxDrain: Int, doDrain: Boolean): FluidStack =
		typedTile[IFluidHandler](from) map { _.drain(from, maxDrain, doDrain) } getOrElse null

	override def canFill(from: ForgeDirection, fluid: Fluid): Boolean =
		typedTile[IFluidHandler](from) map { _.canFill(from, fluid) } getOrElse false

	override def canDrain(from: ForgeDirection, fluid: Fluid): Boolean =
		typedTile[IFluidHandler](from) map { _.canDrain(from, fluid) } getOrElse false

	override def getTankInfo(from: ForgeDirection): Array[FluidTankInfo] =
		typedTile[IFluidHandler](from) map { _.getTankInfo(from) } getOrElse Array.empty[FluidTankInfo]
}
