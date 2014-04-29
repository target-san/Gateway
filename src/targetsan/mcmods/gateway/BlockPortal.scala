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
    setBlockBounds(0f, 0f, 0f, 0f, 0f, 0f)
   	setHardness(1000000.0F)
    setResistance(1000000.0F)
    setUnlocalizedName("portal-pillar")
    
    override def onNeighborBlockChange(world: World, x: Int, y: Int, z: Int, id: Int) {
        val newBlock = Block.blocksList(world.getBlockId(x, y - 1, z))
        if (newBlock == null || !newBlock.isInstanceOf[GatewayTile])
            world.setBlockToAir(x, y, z)
    }
    
    override def onEntityCollidedWithBlock(world: World, x: Int, y: Int, z: Int, entity: Entity) =
        teleportEntity(world, x, y, z, entity)
    
    override def teleportEntity(world: World, x: Int, y: Int, z: Int, entity: Entity) {
        if (!world.isRemote)
        	Block
        		.blocksList(world.getBlockId(x, y - 1, z))
        		.asInstanceOf[GatewayTile]
        		.teleportEntity(world, x, y - 1, z, entity)
    }
    
    override def renderAsNormalBlock = false
    override def isOpaqueCube = false
}
