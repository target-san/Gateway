package targetsan.mcmods.gateway

import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.util.ForgeDirection
import scala.reflect.ClassTag
import targetsan.mcmods.gateway.connectors._
import Utils._

trait ConnectorHost {
	// Retrieve list of connectable sides
	// i.e. the sides that are active on this connector
	def sides: Seq[ForgeDirection]
	// Retrieve tile entity which is considered connected
	// to the specified side
	def connectedTile(side: ForgeDirection): Option[TileEntity]
	// Helper func, performs retrieval with type cast
	def typedTile[T: ClassTag](side: ForgeDirection): Option[T] =
		connectedTile(side) flatMap { _.as[T] }
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

/** Uses relatively cheap tag function to update heavyweight main value
 *  Used to retrieve connected tiles at most once per tick
 */
class Tagged[T, TagT]( private val tagfunc: () => TagT, private val init: () => T) {
	private var tag: TagT = tagfunc()
	private var value: Option[T] = None

	def get: T = {
		val newtag = tagfunc()
		if (tag != newtag)
		{
			tag = newtag
			value = None
		}

		if (value.isEmpty)
			value = Some(init())
		value.get
	}
}

class TileSatellite extends TileEntity with ConnectorHost
	with FluidConnector
{
	import TileSatellite._
	
	private lazy val SatBlock = GatewayMod.BlockGateway.subBlock(getBlockMetadata).asInstanceOf[SubBlockSatellite]
	private lazy val ConnectedSides = connectSides(SatBlock) 
	
	private val ConnectedSats = new Tagged(
		() => worldObj.getTotalWorldTime,
		() => // Did this as a for-comprehension for safety - no crashes,
			  // just connected tiles not accessible on any trouble {
		{
			for {
				core <- worldObj // locate this satellite's core
					.getTileEntity(xCoord - SatBlock.xOffset, yCoord, zCoord - SatBlock.zOffset)
					.as[TileGateway]
					.view
				endPos <- Option(core.getExitPos).view
				endCore <- core // locate exit core
					.getExitWorld
					.getTileEntity(endPos.posX, endPos.posY, endPos.posZ)
					.as[TileGateway]
					.view
				side <- ConnectedSides // enum sides
				sat <- endCore // locate connected satellites
					.getWorldObj
					.getTileEntity(endCore.xCoord - side.offsetX, endCore.yCoord, endCore.zCoord - side.offsetZ)
					.as[TileSatellite]
			}
				yield (side, sat)
		}.toMap
	)

	private val ConnectedTiles = new Tagged(
		() => worldObj.getTotalWorldTime,
		() =>
			for {
				(side, sat) <- ConnectedSats.get
				tile <- Option(// tile entity adjacent to satellite
					sat
					.getWorldObj
					.getTileEntity(sat.xCoord - side.offsetX, sat.yCoord, sat.zCoord - side.offsetZ)
				)
			}
				yield (side, tile)
	)

	def sides = ConnectedSides
	def connectedTile(side: ForgeDirection) = ConnectedTiles.get.get(side)
}
