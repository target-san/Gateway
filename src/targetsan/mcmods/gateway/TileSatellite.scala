package targetsan.mcmods.gateway

import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.util.ForgeDirection

object TileSatellite
{
	def connectSides(sat: SubBlockSatellite) =
		List(
			sat.xOffset match {
				case -1 => ForgeDirection.WEST
				case 1 => ForgeDirection.EAST
				case _ => ForgeDirection.UNKNOWN
			},
			sat.zOffset match {
				case -1 => ForgeDirection.NORTH
				case 1 => ForgeDirection.SOUTH
				case _ => ForgeDirection.UNKNOWN
			}
		)
		.filter(_ != ForgeDirection.UNKNOWN)
}

class TileSatellite extends TileEntity
{
	import TileSatellite._
	
	private lazy val SatBlock = GatewayMod.BlockGateway.subBlock(getBlockMetadata()).asInstanceOf[SubBlockSatellite]
	private lazy val ConnectedSides = connectSides(SatBlock) 
	
	private val ConnectedSats = new Cached( () =>
		{
			val core = worldObj.getTileEntity(xCoord - SatBlock.xOffset, yCoord, zCoord - SatBlock.zOffset).asInstanceOf[TileGateway]
			val endPoint = core.getEndPoint
			val endWorld = core.getEndWorld
			
			def otherSat(dx: Int, dz: Int) = 
				endWorld
				.getTileEntity(endPoint.posX + dx, endPoint.posY, endPoint.posZ + dz)
				.asInstanceOf[TileSatellite]

			ConnectedSides map { s => otherSat(-s.offsetX, -s.offsetZ) }
		}
	)
	
	override def onChunkUnload
	{ // This should notify linked partners that this TE instance will be unloaded shortly
		ConnectedSats.value foreach { _.ConnectedSats.reset }
	}
}
