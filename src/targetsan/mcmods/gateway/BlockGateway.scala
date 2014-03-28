package targetsan.mcmods.gateway

import net.minecraft.block.{Block, BlockObsidian}
import net.minecraft.block.material.Material
import net.minecraft.entity.Entity
import net.minecraft.world.World
import net.minecraft.creativetab.CreativeTabs

class BlockGateway(blockId: Int) extends BlockObsidian(blockId) {
	setHardness(50.0F)
    setResistance(2000.0F)
    setStepSound(Block.soundStoneFootstep)
    setUnlocalizedName("gateway")
    setTextureName("gateway")
    setCreativeTab(CreativeTabs.tabRedstone)
    
    override def onEntityWalking(world: World, x: Int, y: Int, z: Int, entity: Entity) {
	    
	}
}
