package targetsan.mcmods.gateway

import net.minecraft.block.Block
import net.minecraft.util._
import net.minecraft.world.World
import net.minecraft.entity.Entity
import net.minecraft.server.MinecraftServer
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.EnumChatFormatting
import net.minecraft.util.ChunkCoordinates
import net.minecraft.item.ItemStack
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import cpw.mods.fml.common.registry.GameRegistry
import net.minecraft.init.{Blocks, Items}

object Utils
{
	private val GatewayDeadZoneRadius = 7
	
	def world(dim: Int) = MinecraftServer.getServer().worldServerForDimension(dim)
	
	def mapToWorld(x: Int, z: Int, from: World, to: World) =
	{
		def mapCoord(c: Int) = Math.round(c * from.provider.getMovementFactor() / to.provider.getMovementFactor()).toInt 
		(mapCoord(x), mapCoord(z))
	}
	
	def tryPlaceGateway(w: World, x: Int, y: Int, z: Int, player: EntityPlayer)
	{
		// Check if there's multiblock present
		if (// corners
			w.getBlock(x - 1, y, z - 1) != Blocks.obsidian
		 || w.getBlock(x + 1, y, z - 1) != Blocks.obsidian
		 || w.getBlock(x - 1, y, z + 1) != Blocks.obsidian
		 || w.getBlock(x + 1, y, z + 1) != Blocks.obsidian
		 // sides
		 || w.getBlock(x - 1, y, z) != Blocks.glass
		 || w.getBlock(x + 1, y, z) != Blocks.glass
		 || w.getBlock(x, y, z - 1) != Blocks.glass
		 || w.getBlock(x, y, z + 1) != Blocks.glass
		 // center
		 || w.getBlock(x, y, z) != Blocks.redstone_block
		)
			return
		 
		if (w.provider.dimensionId == Gateway.DIMENSION_ID)
		{
			player.addChatMessage(new ChatComponentText("Gateways cannot be constructed from Nether"))
			return
		}
		// Check dead zone on the other side
		if (!isDestinationFree(w, x, y, z))
		{
			player.addChatMessage(new ChatComponentText("Gateway cannot be constructed here - there's another gateway too near on the other side"))
			return
		}
		// Construct gateways on both sides
		w.setBlock(x, y, z, GatewayMod.BlockGatewayBase)
		val (ex, ey, ez) = getExit(w, x, y, z)
		w.getTileEntity(x, y, z).asInstanceOf[TileGateway].init(ex, ey, ez, player)
		player.addChatMessage(new ChatComponentText(s"Gateway successfully constructed from ${w.provider.getDimensionName} to ${Gateway.dimension.provider.getDimensionName}"))
	}
    // Checks if there are no active gateways in the nether too near
	private def isDestinationFree(from: World, x: Int, y: Int, z: Int): Boolean =
	{
		val nether = Gateway.dimension
		// Gateway exits in nether should have at least 7 blocks square between them
		val (exitX, exitZ) = mapToWorld(x, z, from, nether)
		enumVolume(nether,
				exitX - GatewayDeadZoneRadius, 0, exitZ - GatewayDeadZoneRadius,
				exitX + GatewayDeadZoneRadius, nether.provider.getActualHeight - 1, exitZ + GatewayDeadZoneRadius
			)
			.forall { case (x, y, z) => nether.getBlock(x, y, z) != GatewayMod.BlockGatewayBase }
	}
	
	private def getExit(w: World, x: Int, y: Int, z: Int): (Int, Int, Int) =
	{
		val nether = Gateway.dimension
		val (ex, ez) = mapToWorld(x, z, w, nether)
		val ey = (nether.provider.getActualHeight - 1) / 2
		
		(ex, ey, ez)
	}

	def enumVolume(world: World, x1: Int, y1: Int, z1: Int, x2: Int, y2: Int, z2: Int) =
        for (x <- x1 to x2; y <- y1 to y2; z <- z1 to z2) yield (x, y, z)
        
	/** Moves entity along the vector 
	 * 
	 */
	def moveByBlockAnchors(entity: Entity, source: ChunkCoordinates, dest: ChunkCoordinates) =
    	entity.setLocationAndAngles(
			entity.posX + dest.posX - source.posX,
			entity.posY + dest.posY - source.posY,
			entity.posZ + dest.posZ - source.posZ,
			entity.rotationYaw,
			entity.rotationPitch
		)
	/** This function is used to calculate entity's position after moving through a block
	 *  Entity is considered to touch block at the start of move, and it's really necessary
	 *  for the computation to be correct. The move itself is like entity has moved in XZ plane
	 *  through block till it stops touching the one. The move vector is the entity's velocity.
	 *  If entity's XZ velocity is zero, then the vector from entity center to block center is taken
	 *  @param entity Entity to move
	 *  @param block  Block which entity must move through
	 *  @return       Exit point
	 */
	def getEntityThruBlockExit(entity: Entity, block: (Int, Int, Int)): (Double, Double, Double) =
	{
    	val eps = 0.001
    	val (x, z) = (entity.posX - block._1, entity.posZ - block._3)
	    // guard against zero velocity
	    val (dx, dz) =
    		if ( entity.motionX * entity.motionX + entity.motionZ * entity.motionZ > eps * eps) (entity.motionX, entity.motionZ)
    		else (0.5 - x, 0.5 - z)
    	val (x1, z1) = getEntityThruBlockExit(x, z, entity.width, dx, dz)
    	(x1 + block._1, entity.posY, z1 + block._3)
	}
	/** Searches for entity's suitable XZ exit position out of gateway
	 *  Assumes that gateway block is at (0, 0), so recalculate entity coordinates
	 */
	private def getEntityThruBlockExit(x: Double, z: Double, width: Double, dx: Double, dz: Double): (Double, Double) = {
	    // FPU calculation precision
	    val eps = 0.001
	    // Compute line equation from move vector
	    val a = -dz
	    val b = dx
	    val c = x * dz - z * dx
	    // Side coordinates for larger box, which edge would contain new entity center
	    val collisionEps = 0.05
	    val left = - (width / 2 + collisionEps)
	    val right = -left + 1

	    def findCoord1(coef1: Double, coef2: Double, coef3: Double, coord2: Double): Option[Double] =
	        if (coef1.abs < eps) None // no sense in dealing with tiny coefficients
	        else {
	            val coord1 = - (coef2 * coord2 + coef3) / coef1
	            if (left <= coord1 && coord1 <= right) Some(coord1)
	            else None
	        }
	    
	    def sameDir(x1: Double, z1: Double): Boolean = dx * (x1 - x) + dz * (z1 - z) > 0 
	     
	    def pointFromX(x: Double): Option[(Double, Double)] =
	        for (z <- findCoord1(b, a, c, x) if sameDir(x, z)) yield (x, z)
	        
	    def pointFromZ(z: Double): Option[(Double, Double)] =
	        for (x <- findCoord1(a, b, c, z) if sameDir(x, z) ) yield (x, z)
	    
	    List(pointFromX(left), pointFromX(right), pointFromZ(left), pointFromZ(right)) 
	    	.flatten // get rid of inexistent points
	    	.head
	}
	/* I will need this code a bit later
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
	
	private def placeGatewayBlock(world: World, x: Int, y: Int, z: Int, owner: EntityPlayer, exitX: Int, exitY: Int, exitZ: Int, exitDim: Int)
	{
		world.setBlock(x, y, z, Assets.blockGateway.blockID)
		world.getTileEntity(x, y, z).asInstanceOf[TileGateway].setGatewayInfo(exitX, exitY, exitZ, exitDim, owner)
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
	*/
}