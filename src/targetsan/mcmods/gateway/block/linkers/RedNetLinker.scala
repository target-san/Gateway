package targetsan.mcmods.gateway.block.linkers

import cpw.mods.fml.common.Optional.{Method, Interface}
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection
import powercrystals.minefactoryreloaded.api.rednet._
import powercrystals.minefactoryreloaded.api.rednet.connectivity._
import targetsan.mcmods.gateway._

@Interface( iface = "powercrystals.minefactoryreloaded.api.rednet.IRedNetOmniNode", modid = "MineFactoryReloaded", striprefs = true)
trait RedNetLinker extends block.BlockLinker with IRedNetOmniNode {
	@Method( modid = "MineFactoryReloaded" )
	override def getConnectionType(world: World, x: Int, y: Int, z: Int, side: ForgeDirection): RedNetConnectionType =
		linkedBlockAs[IRedNetConnection](world, x, y, z, side)
			.map {
				case (block, w, pos) => block.getConnectionType(w, pos.x, pos.y, pos.z, side)
			}
			.getOrElse(RedNetConnectionType.None)

	@Method( modid = "MineFactoryReloaded" )
	override def onInputChanged(world: World, x: Int, y: Int, z: Int, side: ForgeDirection, value: Int): Unit =
		linkedBlockAs[IRedNetInputNode](world, x, y, z, side)
			.foreach {
				case (block, w, pos) => block.onInputChanged(w, pos.x, pos.y, pos.z, side, value)
			}

	@Method( modid = "MineFactoryReloaded" )
	override def onInputsChanged(world: World, x: Int, y: Int, z: Int, side: ForgeDirection, values: Array[Int]): Unit =
		linkedBlockAs[IRedNetInputNode](world, x, y, z, side)
			.foreach {
				case (block, w, pos) => block.onInputsChanged(w, pos.x, pos.y, pos.z, side, values)
			}

	@Method( modid = "MineFactoryReloaded" )
	override def getOutputValue(world: World, x: Int, y: Int, z: Int, side: ForgeDirection, subnet: Int): Int =
		linkedBlockAs[IRedNetOutputNode](world, x, y, z, side)
			.map {
				case (block, w, pos) => block.getOutputValue(w, pos.x, pos.y, pos.z, side, subnet)
			}
			.getOrElse(0)

	@Method( modid = "MineFactoryReloaded" )
	override def getOutputValues(world: World, x: Int, y: Int, z: Int, side: ForgeDirection): Array[Int] =
		linkedBlockAs[IRedNetOutputNode](world, x, y, z, side)
			.map {
				case (block, w, pos) => block.getOutputValues(w, pos.x, pos.y, pos.z, side)
			}
			.getOrElse(Array.fill(16)(0))
}
