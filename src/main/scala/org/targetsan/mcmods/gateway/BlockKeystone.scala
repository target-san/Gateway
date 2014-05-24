package targetsan.mcmods.gateway

import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.world.World
import java.util.Random
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.Item

class BlockKeystone(id: Int) extends Block(id, Material.rock)
{
	disableStats()
	setHardness(50.0f)
	setResistance(2000.0f)
    setStepSound(Block.soundStoneFootstep)
    setUnlocalizedName("keystone")
    setTextureName("gateway:keystone")
    setCreativeTab(CreativeTabs.tabRedstone)
    
    override def onBlockActivated(world: World, x: Int, y: Int, z: Int, player: EntityPlayer, side: Int, touchX: Float, touchY: Float, touchZ: Float): Boolean =
    {
		// works only with Flint'n'Steel, when clicks top side
		if (player.getHeldItem().getItem().itemID != Item.flintAndSteel.itemID || side != 1)
			return false
		if (!world.isRemote && GatewayUtils.tryInitGateway(player, x, y, z))
			player.getHeldItem().damageItem(1, player)
		true
    }
}
