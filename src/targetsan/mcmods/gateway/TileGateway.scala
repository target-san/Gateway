package targetsan.mcmods.gateway

import net.minecraft.tileentity.TileEntity
import net.minecraft.entity.Entity
import net.minecraft.world.World
import net.minecraft.server.MinecraftServer
import net.minecraft.world.IBlockAccess

class TileGateway extends TileEntity {
	private val PILLAR_HEIGHT = 3
    
    def onCreate {
	    for (y1 <- yCoord + 1 to yCoord + PILLAR_HEIGHT)
	        worldObj.setBlock(xCoord, y1, zCoord, Assets.blockPortal.blockID)
    }
    
	def teleportEntity(entity: Entity) {
	    if (!worldObj.isRemote)
	    	findGatewayAndDo(worldObj, xCoord, yCoord, zCoord) {
	        case (x1, y1, z1, w1) =>
	            val (destX, destY, destZ) = findExitPos(entity, xCoord, yCoord, zCoord, x1, y1, z1)
	            Teleporter.teleport(entity, destX, destY, destZ, w1)
	    	}
	}
	
	private def findGatewayAndDo(world: World, x: Int, y: Int, z: Int)(func: (Int, Int, Int, Int) => Unit) {
	    val dim = if (world.provider.dimensionId == DimensionManager.NETHER_ID) DimensionManager.OVERWORLD_ID else DimensionManager.NETHER_ID
	    val remoteWorld = MinecraftServer.getServer.worldServerForDimension(dim)
	    val factor = world.provider.getMovementFactor() / remoteWorld.provider.getMovementFactor()
	    val destX = Math.round(x * factor).toInt
	    val destZ = Math.round(z * factor).toInt
	    
	    for ((x1, y1, z1) <- findGateway(remoteWorld, destX, y, destZ))
	        func(x1, y1, z1, dim)
	}
	
	private def findGateway(world: World, x: Int, y: Int, z: Int) =
	    BlockUtils.findBlocks(
            world, x - 8, y - 8, z - 8, x + 8, y + 8, z + 8,
            (wp, xp, yp, zp) => { wp.getBlockId(xp, yp, zp) == Assets.blockGateway.blockID }
        ).headOption
	
	private def findExitPos(entity: Entity, x0: Int, y0: Int, z0: Int, x1: Int, y1: Int, z1: Int): (Double, Double, Double) = {
	    val (entx, enty, entz) = (entity.posX - x0, entity.posY - y0, entity.posZ - z0)
	    val (destX, destZ) = findExitPos(entx, entz, entity.width, entity.motionX, entity.motionZ)
	    (destX + x1, enty + y1, destZ + z1)
	}
	/** Searches for entity's suitable XZ exit position out of gateway
	 *  Assumes that gateway block is at (0, 0), so recalculate entity coordinates
	 */
	private def findExitPos(x: Double, z: Double, width: Double, dx: Double, dz: Double): (Double, Double) = {
	    // FPU calculation precision
	    val eps = 0.001
	    // guard against zero velocity
	    val (dx0, dz0) =
    		if ( dx * dx + dz * dz > eps * eps) (dx, dz)
    		else (0.5 - x, 0.5 - z)
	    // Compute line equation from move vector
	    val a = -dz0
	    val b = dx0
	    val c = x * dz0 - z * dx0
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
	    
	    def sameDir(x1: Double, z1: Double): Boolean = dx0 * (x1 - x) + dz0 * (z1 - z) > 0 
	     
	    def pointFromX(x: Double): Option[(Double, Double)] =
	        for (z <- findCoord1(b, a, c, x) if sameDir(x, z)) yield (x, z)
	        
	    def pointFromZ(z: Double): Option[(Double, Double)] =
	        for (x <- findCoord1(a, b, c, z) if sameDir(x, z) ) yield (x, z)
	    
	    List(pointFromX(left), pointFromX(right), pointFromZ(left), pointFromZ(right)) 
	    	.flatten // get rid of inexistent points
	    	.head
	}
}