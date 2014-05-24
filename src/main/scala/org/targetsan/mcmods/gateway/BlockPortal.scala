package targetsan.mcmods.gateway

import net.minecraft.block.Block
import net.minecraft.block.material._
import net.minecraft.world.World
import net.minecraft.entity.Entity

class BlockPortal(id: Int) extends Block(id, Material.portal)
	with Immobile
	with Intangible
	with GatewayTile
{
	val PORTAL_META = 0
	val SHIELD_META = 1
	
    disableStats()
    setBlockBounds(0f, 0f, 0f, 0f, 0f, 0f)
   	setBlockUnbreakable()
    setResistance(6000000.0F)
    setUnlocalizedName("portal-pillar")
    
    override def onNeighborBlockChange(world: World, x: Int, y: Int, z: Int, id: Int) {
		if (world.getBlockMetadata(x, y, z) != PORTAL_META)
			return
        val newBlock = Block.blocksList(world.getBlockId(x, y - 1, z))
        if (newBlock == null || !newBlock.isInstanceOf[GatewayTile])
            world.setBlockToAir(x, y, z)
    }
    
    override def onEntityCollidedWithBlock(world: World, x: Int, y: Int, z: Int, entity: Entity) =
    	if (world.getBlockMetadata(x, y, z) == PORTAL_META)
    		teleportEntity(world, x, y, z, entity)
    
    override def teleportEntity(world: World, x: Int, y: Int, z: Int, entity: Entity) {
        val block = Block.blocksList(world.getBlockId(x, y - 1, z))
        if (block.isInstanceOf[GatewayTile]) 
        	block.asInstanceOf[GatewayTile].teleportEntity(world, x, y - 1, z, entity)
    }
    
    override def isBlockReplaceable(w: World, x: Int, y: Int, z: Int) = w.getBlockMetadata(x, y, z) == SHIELD_META

    override def renderAsNormalBlock = false
    override def isOpaqueCube = false
}
