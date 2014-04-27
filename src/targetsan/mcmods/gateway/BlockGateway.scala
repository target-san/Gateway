package targetsan.mcmods.gateway

import net.minecraft.block.{Block, BlockObsidian}
import net.minecraft.block.material.Material
import net.minecraft.entity.Entity
import net.minecraft.world.World
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.world.IBlockAccess
import net.minecraft.server.MinecraftServer
import net.minecraft.entity.EntityList
import net.minecraft.entity.player.EntityPlayer

class BlockGateway(blockId: Int) extends BlockObsidian(blockId) {
	setHardness(50.0F)
    setResistance(2000.0F)
    setStepSound(Block.soundStoneFootstep)
    setUnlocalizedName("gateway")
    setTextureName("gateway:gateway")
    setCreativeTab(CreativeTabs.tabRedstone)
    
	override def onBlockActivated(world: World, x: Int, y: Int, z: Int, player: EntityPlayer, w: Int, touchX: Float, touchY: Float, touchZ: Float): Boolean = {
	    if (world.isRemote)
	        return true
	        
	    findGatewayAndDo(world, x, y, z) {
	        case (x, y, z, w) =>
	        	System.out.println(s"Found gateway at $x, $y, $z, $w")
	        	Teleporter.teleport(player, x.toDouble + 0.5, y.toDouble + 1.0, z.toDouble + 0.5, w)
	    }
	    true
	}
	
	override def onEntityWalking(world: World, x: Int, y: Int, z: Int, entity: Entity) {
	    if (!world.isRemote)
	    	findGatewayAndDo(world, x, y, z) {
	        case (x1, y1, z1, w1) =>
	            val (destX, destY, destZ) = findExitPos(entity, x, y, z, x1, y1, z1)
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
	
	private def findGateway(world: IBlockAccess, x: Int, y: Int, z: Int) =
	    findBlock(world, x, y, z, 8, (wp, xp, yp, zp) => { wp.getBlockId(xp, yp, zp) == blockId } )
	
	private def findBlock(world: IBlockAccess, x: Int, y: Int, z: Int, eps: Int, pred: (IBlockAccess, Int, Int, Int) => Boolean): Option[(Int, Int, Int)] = {
	    for {
	        x1 <- x - eps to x + eps
	        y1 <- y - eps to y + eps
	        z1 <- z - eps to z + eps
	    }
	    if (pred(world, x1, y1, z1))
	        return Some((x1, y1, z1))
    	None
	}
	
	private def findExitPos(entity: Entity, x0: Int, y0: Int, z0: Int, x1: Int, y1: Int, z1: Int): (Double, Double, Double) = {
	    val (entx, enty, entz) = (entity.posX - x0, entity.posY - y0, entity.posZ - z0)
	    val destY = y1 + enty
	    val (destX, destZ) = findExitPos(entx, entz, entity.width, entity.motionX, entity.motionZ)
	    (destX, destY, destZ)
	}
	/** Searches for entity's suitable XZ exit position out of gateway
	 *  Assumes that gateway block is at (0, 0), so recalculate entity coordinates
	 */
	private def findExitPos(x: Double, z: Double, width: Double, dx: Double, dz: Double): (Double, Double) = {
	    // FPU calculation precision
	    val epsilon = 0.001
	    // guard against zero velocity
	    val (dx0, dz0) =
    		if ( dx * dx + dz * dz > epsilon * epsilon) (dx, dz)
    		else (0.5 - x, 0.5 - z)
	    // Compute line equation from move vector
	    val a = -dz0
	    val b = dx0
	    val c = x * dz0 - z * dx0
	    
	    def findCoord1(coef1: Double, coef2: Double, coef3: Double, coord2: Double): Option[Double] =
	        if (coef1.abs > epsilon) Some(- (coef2 * coord2 + coef3) / coef1) // no sense in dealing with tiny coefficients
	        else None
	        
	    def pointFromX(x: Double): Option[(Double, Double)] =
	        for (z <- findCoord1(b, a, c, x)) yield (x, z)
	        
	    def pointFromZ(z: Double): Option[(Double, Double)] =
	        for (x <- findCoord1(a, b, c, z)) yield (x, z)
	    
	    def signedDist(x1: Double, z1: Double): Double = {
	        val dx1 = x1 - x
	        val dz1 = z1 - z
	        (dx1 * dx1 + dz1 * dz1) * Math.signum(dx0 * dx1 + dz0 * dz1)
	    }
	    // Center coordinates for 4 boxes equal in size to entity box; each of them touches one of gateway's sides
	    val left = - width / 2
	    val right = -left + 1
	    
	    val point = List(pointFromX(left), pointFromX(right), pointFromZ(left), pointFromZ(right))
	    	.flatten // get rid of inexistent points
	    	.map { case (x1, z1) => (x1, z1, signedDist(x1, z1)) } // calculate distances
	    	.filter( point => point._3 > 0 && point._3.abs > epsilon * epsilon) // drop negative and close to origin ones
	    	.sortBy(_._3) // sort by distance
	    	.head // take first one
	    
	    (point._1, point._2) // because there's no way to make 2-tuple from 3-one in-place
	}
}
