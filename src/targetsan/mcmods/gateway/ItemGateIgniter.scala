package targetsan.mcmods.gateway

import net.minecraft.item.Item
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.world.World
import net.minecraft.block.Block
import net.minecraft.util.ChunkCoordinates

import scala.collection.mutable.MutableList

class ItemGateIgniter(id: Int) extends Item(id) {
	maxStackSize = 1
	setMaxDamage(64)
	setCreativeTab(CreativeTabs.tabTools)
	setUnlocalizedName("gateway_igniter")
	setTextureName("gateway:igniter")
	
	override def onItemUse(stack: ItemStack, player: EntityPlayer, world: World, blockX: Int, blockY: Int, blockZ: Int, side: Int, touchX: Float, touchY: Float, touchZ: Float): Boolean = {
	    if (stack.getItemDamage >= stack.getMaxDamage) return false
	    val block = Block.blocksList(world.getBlockId(blockX, blockY, blockZ))
	    if (block == null || !block.isInstanceOf[Ignitable]) return false
	    
		world.playSoundEffect(blockX + 0.5, blockY + 0.5, blockZ + 0.5, "fire.ignite", 1.0F, Item.itemRand.nextFloat() * 0.4F + 0.8F)
	    if (block.asInstanceOf[Ignitable].igniteBlock(world, blockX, blockY, blockZ))
	    	stack.damageItem(1, player)
	    true
	}
	// Locate gateway destination point in specified world
	// Since we're always working with Nether, basepoint's Y is always 64, i.e. height / 2
	private def scanPortalDestination(world: World, x: Int, z: Int) = {
	    val GATEWAY_MIN_RANGE = 8
	    val ANCHOR_MAX_RANGE = 4
	    // Maximum lookup zone, there should be no existing gateways in this range
	    val yRange = world.provider.getHeight() to 0 // goes top-to-bottom, for some lookup and mark reasons
	    val xRange = (x - GATEWAY_MIN_RANGE) to (x + GATEWAY_MIN_RANGE)
	    val zRange = (z - GATEWAY_MIN_RANGE) to (z + GATEWAY_MIN_RANGE)
	    // Anchor lookup zone bounds, this one is used to search for keystone anchors
	    val anchorX = (x - ANCHOR_MAX_RANGE) to (x + ANCHOR_MAX_RANGE)
	    val anchorZ = (z - ANCHOR_MAX_RANGE) to (z + ANCHOR_MAX_RANGE)
	    // Exit lookup bounds, this is a cube around base exit point
	    val exitY = {
	        val cY = world.provider.getHeight() / 2
	        (cY - ANCHOR_MAX_RANGE) to (cY + ANCHOR_MAX_RANGE)
	    }
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
            )
	            anchors += new ChunkCoordinates(x, y, z)
	        // Finally, find all good spots for clean gateway end
	        if (anchorX.contains(x) && anchorZ.contains(z) && exitY.contains(y)) {
	            
	        }
	    }
	}
}