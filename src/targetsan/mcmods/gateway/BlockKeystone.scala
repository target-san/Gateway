package targetsan.mcmods.gateway

import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.world.World
import java.util.Random

class BlockKeystone(id: Int) extends Block(id, Material.rock)
	with Ignitable
{
	disableStats()
	setHardness(50.0f)
	setResistance(2000.0f)
    setStepSound(Block.soundStoneFootstep)
    setUnlocalizedName("keystone")
    setTextureName("gateway:keystone")
    setCreativeTab(CreativeTabs.tabRedstone)

    // Ignitable
    override protected def doIgniteAction(world: World, x: Int, y: Int, z: Int) {
        world.setBlock(x, y, z, Assets.blockGateway.blockID)
        world.notifyBlockChange(x, y, z, Assets.blockGateway.blockID)
    }
}
