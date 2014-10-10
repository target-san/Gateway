package targetsan.mcmods.gateway.block

import net.minecraft.block.BlockContainer
import net.minecraft.block.material.Material
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.entity.Entity
import net.minecraft.util.IIcon
import net.minecraft.world.World
import targetsan.mcmods.gateway.{TileGateway, Multiblock}

class Core(val multiblock: Multiblock) extends BlockContainer(Material.rock)
	with TeleportActor
	with DropsNothing
	with Unbreakable
	with NoCreativePick
{
	disableStats()
	setBlockName("gateway:core")
	setBlockTextureName("gateway:smooth_obsidian")

	//******************************************************************************************************************
	// TileEntity
	//******************************************************************************************************************
	override def hasTileEntity(meta: Int) = true
	override def createNewTileEntity(world: World, meta: Int) = new TileGateway

	//******************************************************************************************************************
	// TeleportActor
	//******************************************************************************************************************
	override def teleportEntity(world: World, x: Int, y: Int, z: Int, entity: Entity) =
		world.getTileEntity(x, y, z).asInstanceOf[TileGateway].teleportEntity(entity)

	//******************************************************************************************************************
	// Icons and rendering
	//******************************************************************************************************************
	protected var blockTopIcon: IIcon = null

	override def registerBlockIcons(icons: IIconRegister)
	{
		super.registerBlockIcons(icons)
		blockTopIcon = icons.registerIcon("gateway:top-center")
	}

	override def getIcon(side: Int, meta: Int): IIcon =
		if (side == 1) blockTopIcon
		else           blockIcon

}
