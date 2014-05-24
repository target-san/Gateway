package targetsan.mcmods.gateway

import java.util.List
import net.minecraft.block.Block
import net.minecraft.util.AxisAlignedBB
import net.minecraft.world.World
import net.minecraft.entity.Entity
import java.util.Random
import net.minecraft.server.MinecraftServer
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.EnumChatFormatting

// Block can't be moved by pistons
trait Immobile extends Block
{
    override def getMobilityFlag = 2 // Block can't be moved by piston
}
// Block can't be interacted and not an obstacle
trait Intangible extends Block
{
    override def getSelectedBoundingBoxFromPool(world: World, x: Int, y: Int, z: Int): AxisAlignedBB = null
    override def getCollisionBoundingBoxFromPool(world: World, x: Int, y: Int, z: Int): AxisAlignedBB = null
    override def addCollisionBoxesToList(world: World, x: Int, y: Int, z: Int, mask: AxisAlignedBB, list: java.util.List[_], entity: Entity) { }
}
// Adds entity teleportation logic
trait GatewayTile
{
    def teleportEntity(world: World, x: Int, y: Int, z: Int, entity: Entity): Unit
}

object BlockUtils
{
    def blockVolume(world: World, x1: Int, y1: Int, z1: Int, x2: Int, y2: Int, z2: Int) =
        for {
            x <- x1 to x2
            y <- y1 to y2
            z <- z1 to z2
        }
    		yield (x, y, z)
	
	def getBlock(world: World, x: Int, y: Int, z: Int) = Block.blocksList(world.getBlockId(x, y, z))
}

object GatewayUtils
{
	val NETHER_DIM_ID = -1
	
	def world(dim: Int) = MinecraftServer.getServer().worldServerForDimension(dim)
	def netherWorld = world(NETHER_DIM_ID)
	
	def tryInitGateway(player: EntityPlayer, x: Int, y: Int, z: Int): Boolean =
	{
		if (player.worldObj.provider.dimensionId == NETHER_DIM_ID)
		{
			player.addChatMessage(EnumChatFormatting.RED + "Gateways cannot be initiated from Nether")
			return false
		}
		if (!canGatewayBeHere(player, x, y, z))
		{
			player.addChatMessage(EnumChatFormatting.RED + "There's another gateway exit in nether closer than 7 blocks")
			return false
		}
		placeGatewayAt(player, x, y, z)
		true
	}
	// Returns false if gateway can't be instantiated here
	private def canGatewayBeHere(player: EntityPlayer, x: Int, y: Int, z: Int): Boolean =
	{
		val nether = netherWorld
		// Gateway exits in nether should have at least 7 blocks square between them
		val (exitX, exitY, exitZ) = netherExit(player.worldObj, x, y, z)
		BlockUtils.blockVolume(netherWorld, exitX - 7, exitY, exitZ - 7, exitX + 7, exitY, exitZ + 7)
			.forall { case (x, y, z) => nether.getBlockId(x, y, z) != Assets.blockGateway.blockID }
	}
	
	private def calcCoord(c: Int, from: World, to: World) = Math.round(c * from.provider.getMovementFactor() / to.provider.getMovementFactor()).toInt
	
	private def netherExit(world: World, x: Int, y: Int, z: Int) =
	{
		val nether = netherWorld
		(calcCoord(x, world, nether), nether.provider.getActualHeight() / 2, calcCoord(z, world, nether))
	}
	
	private def placeGatewayBlock(world: World, x: Int, y: Int, z: Int, owner: EntityPlayer, exitX: Int, exitY: Int, exitZ: Int, exitDim: Int)
	{
		world.setBlock(x, y, z, Assets.blockGateway.blockID)
		world.getBlockTileEntity(x, y, z).asInstanceOf[TileGateway].setGatewayInfo(exitX, exitY, exitZ, exitDim, owner)
	}
	
	private def placeGatewayAt(player: EntityPlayer, x: Int, y: Int, z: Int) =
	{
		val EXIT_RADIUS = 2
		val EXIT_HEIGHT = 4
		
		val world = player.worldObj
		val nether = netherWorld
		// Calculate exit point
		val (exitX, exitY, exitZ) = netherExit(world, x, y, z)
		// Place gateway entrance, directed to nether
		placeGatewayBlock(world, x, y, z, player, exitX, exitY, exitZ, NETHER_DIM_ID)
		// Place nether side safety measures
		BlockUtils.blockVolume(nether, exitX - EXIT_RADIUS, exitY, exitZ - EXIT_RADIUS, exitX + EXIT_RADIUS, exitY, exitZ + EXIT_RADIUS)
			.foreach { case (x, y, z) => nether.setBlock(x, y, z, Block.obsidian.blockID) }
		BlockUtils.blockVolume(nether, exitX - EXIT_RADIUS, exitY + 1, exitZ - EXIT_RADIUS, exitX + EXIT_RADIUS, exitY + EXIT_HEIGHT, exitZ + EXIT_RADIUS)
			.foreach { case (x, y, z) => nether.setBlock(x, y, z, Assets.blockPortal.blockID, Assets.blockPortal.SHIELD_META, 3) }
		// Place nether side
		placeGatewayBlock(nether, exitX, exitY, exitZ, player, x, y, z, world.provider.dimensionId)
	}
}