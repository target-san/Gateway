package targetsan.mcmods.gateway

import java.util.List
import net.minecraft.block.Block
import net.minecraft.util.AxisAlignedBB
import net.minecraft.world.World
import net.minecraft.entity.Entity
import java.util.Random

// Block can't be moved by pistons
trait Immobile extends Block {
    override def getMobilityFlag = 2 // Block can't be moved by piston
}
// Block can't be interacted and not an obstacle
trait Intangible extends Block {
    override def getSelectedBoundingBoxFromPool(world: World, x: Int, y: Int, z: Int): AxisAlignedBB = null
    override def getCollisionBoundingBoxFromPool(world: World, x: Int, y: Int, z: Int): AxisAlignedBB = null
    override def addCollisionBoxesToList(world: World, x: Int, y: Int, z: Int, mask: AxisAlignedBB, list: java.util.List[_], entity: Entity) { }
}
// Adds entity teleportation logic
trait GatewayTile {
    def teleportEntity(world: World, x: Int, y: Int, z: Int, entity: Entity): Unit
}

trait Ignitable extends Block {
    private val IGNITED_FLAG = 0x08
    private val IGNITION_TIMEOUT = 200
    
	override def randomDisplayTick(world: World, x: Int, y: Int, z: Int, random: Random) {
	    if (world.getBlockMetadata(x, y, z) != 0)
	    	for (i <- 0 until 4)
	    		world.spawnParticle("largesmoke", x + random.nextDouble(), y + 1.0, z + random.nextDouble(), 0.0, 0.0, 0.0)
	}
	
	override def updateTick(world: World, x: Int, y: Int, z: Int, random: Random) {
	    unIgnite(world, x, y ,z)
	}
	
	private def unIgnite(world: World, x: Int, y: Int, z: Int) =
	    world.setBlockMetadataWithNotify(x, y, z, world.getBlockMetadata(x, y, z) & ~IGNITED_FLAG, 3)
	
	def igniteBlock(world: World, x: Int, y: Int, z: Int): Boolean = {
	    val meta = world.getBlockMetadata(x, y, z)
	    if ((meta & IGNITED_FLAG) != 0) {
	        unIgnite(world, x, y, z)
	        doIgniteAction(world, x, y, z)
	        true
	    }
	    else {
	        world.setBlockMetadataWithNotify(x, y, z, world.getBlockMetadata(x, y, z) | IGNITED_FLAG, 3)
	        world.scheduleBlockUpdate(x, y, x, this.blockID, IGNITION_TIMEOUT)
	        false
	    }
	}
	
	protected def doIgniteAction(world: World, x: Int, y: Int, z: Int)
}
