package targetsan.mcmods.gateway

import net.minecraft.tileentity.TileEntity
import net.minecraft.entity.Entity
import net.minecraft.world.World
import net.minecraft.server.MinecraftServer
import net.minecraft.world.IBlockAccess
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks

class TileGateway extends TileEntity
{
	private val PILLAR_HEIGHT = 3
	
	private var exitX = 0
	private var exitY = 0
	private var exitZ = 0
	private var exitDim = 0
	private var owner = ""
		
	def init(x: Int, y: Int, z: Int, player: EntityPlayer)
	{
		if (worldObj.isRemote)
			return
		// This init can be called only from non-gateway dimension
		if (worldObj.provider.dimensionId == Gateway.DIMENSION_ID)
			throw new IllegalStateException("Tile cannot be initialized in such a way from Nether")
		initBase(x, y, z, player)
		exitDim = Gateway.DIMENSION_ID
		// When gateway tile is properly initialized, we construct exitpoint on the other side
		val gateworld = Gateway.dimension
		gateworld.setBlock(x, y, z, GatewayMod.BlockGatewayBase)
		gateworld.getTileEntity(x, y, z).asInstanceOf[TileGateway].init(xCoord, yCoord, zCoord, worldObj.provider.dimensionId, player)
	}
    
	private def init(x: Int, y: Int, z: Int, dim: Int, player: EntityPlayer)
	{
		if (worldObj.provider.dimensionId != Gateway.DIMENSION_ID)
			throw new IllegalStateException("Tile can be initialized in such a way only from Nether")
		initBase(x, y, z, player)
		exitDim = dim
	}
	
	private def initBase(ex: Int, ey: Int, ez: Int, player: EntityPlayer)
	{
		if (!owner.isEmpty()) // owner and other params are set only once
			throw new IllegalStateException("Gateway parameters are set only once")
		exitX = ex
		exitY = ey
		exitZ = ez
		owner = player.getGameProfile().getId()
		worldObj.markTileEntityChunkModified(xCoord, yCoord, zCoord, this)
	}
    
	def dispose()
	{
		if (worldObj.isRemote)
			return
		// This would trigger removal of the gateway's endpoint located in Nether
		if (worldObj.provider.dimensionId != Gateway.DIMENSION_ID)
			Gateway.dimension.setBlock(exitX, exitY, exitZ, Blocks.stone)
			
		owner = null
		exitX = 0
		exitY = 0
		exitZ = 0
		exitDim = 0
	}
	
	def teleportEntity(entity: Entity)
	{
		/*
	    if (worldObj.isRemote) // Performed only server-side
	    	return
	    
	    if (owner.isEmpty)
	    	return
	    	//throw new IllegalStateException(s"Gateway at (${xCoord}, ${yCoord}, ${zCoord}, ${worldObj.provider.dimensionId}) isn't initialized and thus cannot teleport")
	    
	    val exitWorld = Utils.world(exitDim)
	    
	    val exitTile = exitWorld.getTileEntity(exitX, exitY, exitZ)
	    if (exitTile == null || !exitTile.isInstanceOf[TileGateway])
	    	return
	    */
    	//val (destX, destY, destZ) = findExitPos(entity, xCoord, yCoord, zCoord, exitX, exitY, exitZ)
	    //Teleporter.teleport(entity, destX, destY, destZ, exitDim) // Disabled till the time I'll have proper teleport code
	}
	
	// NBT
	override def readFromNBT(tag: NBTTagCompound)
	{
		if (tag == null)
			return
		super.readFromNBT(tag)
		val pos = tag.getIntArray("exitPos")
		exitX = pos(0)
		exitY = pos(1)
		exitZ = pos(2)
		exitDim = pos(3)
		owner = tag.getString("owner")
	}
	
	override def writeToNBT(tag: NBTTagCompound)
	{
		if (tag == null)
			return
		super.writeToNBT(tag)
		tag.setIntArray("exitPos", Array(exitX, exitY, exitZ, exitDim))
		tag.setString("owner", owner)
	}
}
