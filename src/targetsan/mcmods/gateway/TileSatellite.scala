package targetsan.mcmods.gateway

import net.minecraft.tileentity.TileEntity

object TileSatellite
{
	
}

class TileSatellite extends TileEntity
{
	private def otherSat(dx: Int, dz: Int) = 
	{
		val sat = GatewayMod.BlockGateway.subBlock(worldObj.getBlockMetadata(xCoord, yCoord, zCoord)).asInstanceOf[SubBlockSatellite]
		val core = worldObj.getTileEntity(xCoord - sat.xOffset , yCoord, zCoord - sat.zOffset ).asInstanceOf[TileGateway]
		core.getEndWorld.getTileEntity(core.getEndPoint.posX + dx, core.getEndPoint.posY, core.getEndPoint.posZ + dz).asInstanceOf[TileSatellite]
	}
}