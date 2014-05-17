package targetsan.mcmods.gateway

import java.util.List
import net.minecraft.block.Block
import net.minecraft.util.AxisAlignedBB
import net.minecraft.world.World
import net.minecraft.entity.Entity
import java.util.Random
import net.minecraft.server.MinecraftServer

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

object BlockUtils {
    def blockVolume(world: World, x1: Int, y1: Int, z1: Int, x2: Int, y2: Int, z2: Int) =
        for {
            x <- x1 to x2
            y <- y1 to y2
            z <- z1 to z2
        }
    		yield (x, y, z)
	
	def getBlock(world: World, x: Int, y: Int, z: Int) = Block.blocksList(world.getBlockId(x, y, z))
	// Transforms block coordinates between worlds
	def otherWorld(from: World, x: Int, y: Int, z: Int, to: World): (Int, Int, Int) = {
		val factor = from.provider.getMovementFactor() / to.provider.getMovementFactor()
		(Math.round(x * factor).toInt, y, Math.round(z * factor).toInt)
	}
}

object GatewayUtils {
	val NETHER_DIM_ID = -1
	
	def world(dim: Int) = MinecraftServer.getServer().worldServerForDimension(dim)
	def nether = world(NETHER_DIM_ID)
	
	def placeExit(x: Int, y: Int, z: Int) = {
		val w = nether
		BlockUtils.blockVolume(w, x - 2, y, z - 2, x + 2, y, z + 2)
			.foreach {
				case (x, y, z) => w.setBlock(x, y, z, Block.obsidian.blockID)
			}
		BlockUtils.blockVolume(w, x - 2, y + 1, z - 2, x + 2, y + 4, z + 2)
			.foreach {
				case (x, y, z) => w.setBlock(x, y, z, Assets.blockPortal.blockID, Assets.blockPortal.SHIELD_META, 3)
			}
		w.setBlock(x, y, z, Assets.blockGateway.blockID)
	}
}