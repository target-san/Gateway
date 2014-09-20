package targetsan.mcmods.gateway

import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.util.ForgeDirection
import scala.reflect.ClassTag
import targetsan.mcmods.gateway.connectors._

trait ConnectorHost {
	// Retrieve list of connectable sides
	// i.e. the sides that are active on this connector
	def sides: Seq[ForgeDirection]
	// Retrieve tile entity which is considered connected
	// to the specified side
	def connectedTile(side: ForgeDirection): Option[TileEntity]
	// Helper func, performs retrieval with type cast
	def typedTile[T](side: ForgeDirection)(implicit tag: ClassTag[T]): Option[T] =
		for {
			tile <- connectedTile(side)
			typed <- tag.unapply(tile)
		}
			yield typed
}

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

class TileSatellite extends TileEntity with ConnectorHost
	with FluidConnector
{
	import TileSatellite._
	
	private lazy val SatBlock = GatewayMod.BlockGateway.subBlock(getBlockMetadata).asInstanceOf[SubBlockSatellite]
	private lazy val ConnectedSides = connectSides(SatBlock) 
	
	private val ConnectedSats = new Cached( () =>
		{
			val core = worldObj.getTileEntity(xCoord - SatBlock.xOffset, yCoord, zCoord - SatBlock.zOffset).asInstanceOf[TileGateway]
			val endTile = core.getExitTile

			def otherSat(dx: Int, dz: Int) = 
				endTile.getWorldObj
				.getTileEntity(endTile.xCoord + dx, endTile.yCoord, endTile.zCoord + dz)
				.asInstanceOf[TileSatellite]

			ConnectedSides.map( s => (s, otherSat(-s.offsetX, -s.offsetZ) ) ).toMap
		}
	)

	protected def invalidatePartners() =
		ConnectedSats.reset()

	override def onChunkUnload()
	{ // This should notify linked partners that this TE instance will be unloaded shortly
		ConnectedSats.get foreach { _._2.invalidatePartners() }
	}

	def sides = ConnectedSides
	def connectedTile(side: ForgeDirection) =
		for {
			t <- ConnectedSats.get.get(side)
			tile <- Option(t.getWorldObj.getTileEntity(t.xCoord - side.offsetX, t.yCoord - side.offsetY, t.zCoord - side.offsetZ))
		}
			yield tile
}
