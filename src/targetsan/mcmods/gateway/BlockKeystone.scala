package targetsan.mcmods.gateway

import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.creativetab.CreativeTabs

class BlockKeystone(id: Int) extends Block(id, Material.rock)
{
	disableStats()
	setHardness(50.0f)
	setResistance(2000.0f)
    setStepSound(Block.soundStoneFootstep)
    setUnlocalizedName("keystone")
    setTextureName("gateway:keystone")
    setCreativeTab(CreativeTabs.tabRedstone)
}