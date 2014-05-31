package targetsan.mcmods.gateway

import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.block.{Block, BlockContainer}
import net.minecraft.world.World
import net.minecraft.tileentity.TileEntity
import net.minecraft.block.material.Material
import net.minecraft.util.IIcon
import java.util.Random
import net.minecraft.item.Item

class BlockGateway extends BlockContainer(Material.rock)
{
	setBlockName("Gateway")
	setBlockTextureName("gateway:gateway")
	setBlockUnbreakable()
	setResistance(6000000.0F)
	disableStats()
	setStepSound(Block.soundTypePiston)
	
	private var topIcon: IIcon = null
	
	override def createNewTileEntity(world: World, meta: Int) = new TileGateway
	
	override def registerBlockIcons(icons: IIconRegister)
	{
		super.registerBlockIcons(icons)
		topIcon = icons.registerIcon("gateway:gateway_a")
	}
	
	override def getItemDropped(meta: Int, random: Random, fortune: Int): Item = null
	
	override def getIcon(side: Int, meta: Int) = 
		if (side == 1) topIcon
		else blockIcon
}