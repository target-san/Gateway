package targetsan.mcmods.gateway.block

import net.minecraft.block.{BlockContainer, Block}
import net.minecraft.block.material.Material
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.IIcon
import net.minecraft.world.{IBlockAccess, World}
import net.minecraftforge.common.util.ForgeDirection

import targetsan.mcmods.gateway._
import targetsan.mcmods.gateway.Utils._

/** Represents gateway's solid platform tiles
 *  Platform is intended to be 3x3
 */
class Platform extends BlockContainer(Material.rock)
	with TeleportActor
	with DropsNothing
	with Unbreakable
	with NoCreativePick
{
	disableStats()
	setBlockName("gateway:platform")
	setBlockTextureName("gateway:smooth_obsidian")

	private lazy val Tiles = Multiblock.Parts
		.filter { p => p.block == Assets.BlockPlatform && p.tile.nonEmpty }
		.map { p => (p.meta, p.tile.get) }
		.toMap

	//******************************************************************************************************************
	// TileEntity
	//******************************************************************************************************************
	override def hasTileEntity(meta: Int) = Tiles contains meta
	override def createNewTileEntity(world: World, meta: Int) =
		Tiles get meta map { _() } orNull

	private def getTile(w: IBlockAccess, x: Int, y: Int, z: Int) = w.getTileEntity(x, y, z).as[tile.Gateway]

	//******************************************************************************************************************
	// TeleportActor
	//******************************************************************************************************************
	override def teleportEntity(world: World, x: Int, y: Int, z: Int, entity: Entity) =
		getTile(world, x, y, z) foreach { _.teleport(entity) }

	//******************************************************************************************************************
	// World interaction
	//******************************************************************************************************************
	override def onBlockActivated(world: World, x: Int, y: Int, z: Int, player: EntityPlayer, side: Int, xTouch: Float, yTouch: Float, zTouch: Float): Boolean =
	{
		getTile(world, x, y, z) foreach { _.onActivated(player, ForgeDirection.getOrientation(side)) }
		false
	}

	override def onNeighborBlockChange(world: World, x: Int, y: Int, z: Int, block: Block) =
		getTile(world, x, y, z) foreach { _.onNeighborBlockChanged() }

	override def onNeighborChange(world: IBlockAccess, x: Int, y: Int, z: Int, tx: Int, ty: Int, tz: Int): Unit =
		getTile(world, x, y, z) foreach { _.onNeighborTileChanged( tx, ty, tz ) }

	//******************************************************************************************************************
	// Icons and rendering
	//******************************************************************************************************************
	private var icons: Seq[Seq[IIcon]] = null

	override def registerBlockIcons(icons: IIconRegister) =
	{
		// Load base icons
		val topCenter = icons.registerIcon("gateway:top-center")
		val topCorner = icons.registerIcon("gateway:top-corner")
		val topSide = icons.registerIcon("gateway:top-side")
		val topSide90 = icons.registerIcon("gateway:top-side-90")
		val sideCorner = icons.registerIcon("gateway:side-corner")
		val sideSide = icons.registerIcon("gateway:side-side")
		// Default icons
		super.registerBlockIcons(icons)

		this.icons = Vector(
		//                 DOWN       UP                   NORTH                SOUTH                WEST                 EAST
		/* CORE */ Vector( blockIcon, topCenter,           blockIcon,           blockIcon,           blockIcon,           blockIcon),
		/* NW   */ Vector( blockIcon, topCorner,           sideCorner.flippedU, blockIcon,           sideCorner,          blockIcon),
		/* N    */ Vector( blockIcon, topSide,             sideSide,            blockIcon,           blockIcon,           blockIcon),
		/* NE   */ Vector( blockIcon, topCorner.flippedU,  sideCorner,          blockIcon,           blockIcon,           sideCorner.flippedU),
		/*  E   */ Vector( blockIcon, topSide90,           blockIcon,           blockIcon,           blockIcon,           sideSide),
		/* SE   */ Vector( blockIcon, topCorner.flippedUV, blockIcon,           sideCorner.flippedU, blockIcon,           sideCorner),
		/* S    */ Vector( blockIcon, topSide.flippedV,    blockIcon,           sideSide,            blockIcon,           blockIcon),
		/* SW   */ Vector( blockIcon, topCorner.flippedV,  blockIcon,           sideCorner,          sideCorner.flippedU, blockIcon),
		/*  W   */ Vector( blockIcon, topSide90.flippedU,  blockIcon,           blockIcon,           sideSide,            blockIcon)
		)
	}

	override def getIcon(side: Int, meta: Int): IIcon = icons(meta)(side)
}
