package targetsan.mcmods.gateway

import net.minecraft.item.Item
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.world.World
import net.minecraft.block.Block
import net.minecraft.util.ChunkCoordinates
import scala.collection.mutable.MutableList
import net.minecraft.server.MinecraftServer

class ItemGateIgniter(id: Int) extends Item(id) {
	maxStackSize = 1
	setMaxDamage(64)
	setCreativeTab(CreativeTabs.tabTools)
	setUnlocalizedName("gateway_igniter")
	setTextureName("gateway:igniter")
	
	private val PILLAR_HEIGHT = 3
	private val GATEWAY_SPACE_RADIUS = 2
	
	override def onItemUse(stack: ItemStack, player: EntityPlayer, world: World, blockX: Int, blockY: Int, blockZ: Int, side: Int, touchX: Float, touchY: Float, touchZ: Float): Boolean = {
	    if (stack.getItemDamage >= stack.getMaxDamage) return false
	    val block = Block.blocksList(world.getBlockId(blockX, blockY, blockZ))
	    if (block == null) return false
	    if (!block.isInstanceOf[Ignitable]) {
	    	if (world.isRemote) return true
	    	val nether = netherPos(world, blockX, blockZ)
	    	scanPortalDestination(nether._1, nether._2, nether._3)
	    	return true
	    }
	    
		world.playSoundEffect(blockX + 0.5, blockY + 0.5, blockZ + 0.5, "fire.ignite", 1.0F, Item.itemRand.nextFloat() * 0.4F + 0.8F)
	    if (block.asInstanceOf[Ignitable].igniteBlock(world, blockX, blockY, blockZ))
	    	stack.damageItem(1, player)
	    true
	}
	
	private def netherPos(world: World, x: Int, z: Int): (World, Int, Int) = {
	    val remoteWorld = GatewayUtils.nether
	    val factor = world.provider.getMovementFactor() / remoteWorld.provider.getMovementFactor()
	    (remoteWorld, Math.round(x * factor).toInt, Math.round(z * factor).toInt)
	}
	// Locate gateway destination point in specified world
	// Since we're always working with Nether, basepoint's Y is always 64, i.e. height / 2
	private def scanPortalDestination(world: World, x: Int, z: Int) = {
	    val GATEWAY_MIN_RANGE = 8
	    val ANCHOR_MAX_RANGE = 4
	    val EXIT_RANGE = 4
	    val EXIT_Y_RANGE = 16
	    // Maximum lookup zone, there should be no existing gateways in this range
	    val yRange = 0 until world.provider.getActualHeight()
	    val xRange = (x - GATEWAY_MIN_RANGE) to (x + GATEWAY_MIN_RANGE)
	    val zRange = (z - GATEWAY_MIN_RANGE) to (z + GATEWAY_MIN_RANGE)
	    // Anchor lookup zone bounds, this one is used to search for keystone anchors
	    val anchorX = (x - ANCHOR_MAX_RANGE) to (x + ANCHOR_MAX_RANGE)
	    val anchorZ = (z - ANCHOR_MAX_RANGE) to (z + ANCHOR_MAX_RANGE)
	    // Exit lookup bounds, this is a cube around base exit point
	    val exitY = {
	        val cY = world.provider.getActualHeight() / 2
	        (cY - EXIT_Y_RANGE) to (cY + EXIT_Y_RANGE)
	    }
	    val exitX = (x - EXIT_RANGE) to (x + EXIT_RANGE)
	    val exitZ = (z - EXIT_RANGE) to (z + EXIT_RANGE)
	    // Lists with chunk coordinates
	    val gateways, anchors, exits = MutableList[ChunkCoordinates]()
	    // Actual loop
	    for (y <- yRange; x <- xRange; z <- zRange)
	    {
	        // Find all existing gateways which are too near
	        if (world.getBlockId(x, y, z) == Assets.blockGateway.blockID)
	            gateways += new ChunkCoordinates(x, y, z)
	        // Find all inactive keystones, which can work as anchors
	        if (anchorX.contains(x) && anchorZ.contains(z)
                && world.getBlockId(x, y, z) == Assets.blockKeystone.blockID
                && hasEnoughSpaceAbove(world, x, y, z)
            )
	            anchors += new ChunkCoordinates(x, y, z)
	        // Finally, find all good spots for clean gateway end
	        if (anchorX.contains(x)
        		&& anchorZ.contains(z)
        		&& exitY.contains(y)
        		&& hasEnoughSpaceAbove(world, x, y, z)
        		&& isValidBasement(world, x, y, z)
    		)
	        	exits += new ChunkCoordinates(x, y, z)
	    }
	    
	    def print(pos: ChunkCoordinates) = System.out.println(s"(${pos.posX}, ${pos.posY}, ${pos.posZ})")
	    System.out.println("Existing gateways: ")
	    gateways.foreach(print(_))
	    System.out.println("Anchors: ")
	    anchors.foreach(print(_))
	    System.out.println("Exit points: ")
	    exits.foreach(print(_))
	}
	
	private def hasEnoughSpaceAbove(world: World, x: Int, y: Int, z: Int): Boolean =
		BlockUtils.blockVolume(world,
			x - GATEWAY_SPACE_RADIUS, y + 1, z - GATEWAY_SPACE_RADIUS,
			x + GATEWAY_SPACE_RADIUS, y + PILLAR_HEIGHT, z + GATEWAY_SPACE_RADIUS
		).forall {
			case (x, y, z) => isBlockEmpty(world, x, y, z)
		}
	// Treat all blocks which are explicitly air or neither solid nor liquid, or have zero collision box
	private def isBlockEmpty(world: World, x: Int, y: Int, z: Int): Boolean = {
		val block = BlockUtils.getBlock(world, x, y, z)
		block == null ||
		block.isAirBlock(world, x, y, z) ||
		(!block.blockMaterial.isSolid && !block.blockMaterial.isLiquid) ||
		block.getCollisionBoundingBoxFromPool(world, x, y, z) == null
	}
	
	private def isValidBasement(world: World, x: Int, y: Int, z: Int) = {
		val block = BlockUtils.getBlock(world, x, y, z)
		block != null &&
		block.blockMaterial.isSolid &&
		block.isBlockNormalCube(world, x, y, z) &&
		!block.hasTileEntity()
	}
		
}