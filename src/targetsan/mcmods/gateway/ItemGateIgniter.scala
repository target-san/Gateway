package targetsan.mcmods.gateway

import net.minecraft.item.Item
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.world.World

class ItemGateIgniter(id: Int) extends Item(id) {
	maxStackSize = 1
	setMaxDamage(64)
	setCreativeTab(CreativeTabs.tabTools)
	setUnlocalizedName("gateway_igniter")
	setTextureName("gateway:igniter")
	
	override def onItemUse(stack: ItemStack, player: EntityPlayer, world: World, blockX: Int, blockY: Int, blockZ: Int, side: Int, touchX: Float, touchY: Float, touchZ: Float): Boolean =
	    if (stack.getItemDamage < stack.getMaxDamage)
	    {
	    	world.playSoundEffect(blockX + 0.5, blockY + 0.5, blockZ + 0.5, "fire.ignite", 1.0F, Item.itemRand.nextFloat() * 0.4F + 0.8F);
	        stack.damageItem(1, player)
	    	true
	    }
	    else false
}