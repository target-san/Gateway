package targetsan.mcmods.gateway.block

import net.minecraft.block.{BlockContainer, Block}
import net.minecraft.block.material.Material
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.{Blocks, Items}
import net.minecraft.util.IIcon
import net.minecraft.world.{IBlockAccess, World}
import net.minecraftforge.common.util.ForgeDirection

import targetsan.mcmods.gateway._
import targetsan.mcmods.gateway.Utils._

class Edge extends BlockContainer(Material.rock)
	with DropsNothing
	with Unbreakable
	with NoCreativePick
{
	disableStats()
	setBlockName("gateway:edge")
	setBlockTextureName("gateway:smooth_obsidian")

	val isDiagonal = xOffset != 0 && zOffset != 0
	val side = (xOffset, zOffset) match {
		case (0, -1) => 0
		case (1, 0)  => 1
		case (0, 1)  => 2
		case (-1, 0) => 3
		case _ => -1
	}

	//******************************************************************************************************************
	// TileEntity
	//******************************************************************************************************************
	override def hasTileEntity(meta: Int) = true
	override def createNewTileEntity(world: World, meta: Int) = new TileSatellite

	//******************************************************************************************************************
	// World interaction
	//******************************************************************************************************************
	override def onBlockActivated(world: World, x: Int, y: Int, z: Int, player: EntityPlayer, side: Int, xTouch: Float, yTouch: Float, zTouch: Float): Boolean =
	{
		if (world.isRemote ||
			isDiagonal ||
			side != 1 ||
			player.getHeldItem == null ||
			player.getHeldItem.getItem != Items.flint_and_steel
		)
			return false

		world
			.getTileEntity(x - xOffset, y, z - zOffset)
			.as[TileGateway]
			.map { _.markForDispose(player, this.side) }

		false
	}

	override def onNeighborBlockChange(world: World, x: Int, y: Int, z: Int, block: Block)
	{
		if (world.isRemote)
			return
		// Notify tile that one of its neighbors has changed
		world
			.getTileEntity(x, y, z)
			.as[TileSatellite]
			.foreach { _.onNeighborBlockChanged() }

		// Deconstruction logic
		if (isDiagonal ||
			world.getBlock(x, y + 1, z) == Blocks.fire
		)
			return

		world
			.getTileEntity(x - xOffset, y, z - zOffset)
			.as[TileGateway]
			.foreach { _.unmarkForDispose(this.side) }
	}

	override def onNeighborChange(world: IBlockAccess, x: Int, y: Int, z: Int, tx: Int, ty: Int, tz: Int): Unit =
		world
			.getTileEntity(x, y, z)
			.as[TileSatellite]
			.foreach { _.onNeighborTileChanged(
				(tx - x, ty - y, tz - z) match {
					case ( 0, -1,  0) => ForgeDirection.DOWN
					case ( 0,  1,  0) => ForgeDirection.UP
					case ( 0,  0, -1) => ForgeDirection.NORTH
					case ( 0,  0,  1) => ForgeDirection.SOUTH
					case (-1,  0,  0) => ForgeDirection.WEST
					case ( 1,  0,  0) => ForgeDirection.EAST
					case _ => ForgeDirection.UNKNOWN
				}
			)}

	override def onBlockPreDestroy(world: World, x: Int, y: Int, z: Int, meta: Int) =
		world
			.getTileEntity(x - xOffset, y, z - zOffset)
			.as[TileGateway]
			.foreach { _.invalidate() }

	//******************************************************************************************************************
	// Icons and rendering
	//******************************************************************************************************************
	private val icons = Array.fill[IIcon](6)(null)

	override def registerBlockIcons(icons: IIconRegister) =
	{
		import ForgeDirection._
		// Load base icons
		 val topCorner = icons.registerIcon("gateway:top-corner")
		 val topSide = icons.registerIcon("gateway:top-side")
		 val topSide90 = icons.registerIcon("gateway:top-side-90")
		 val sideCorner = icons.registerIcon("gateway:side-corner")
		 val sideSide = icons.registerIcon("gateway:side-side")
		 // Default icons
		 super.registerBlockIcons(icons)
		 for (i <- VALID_DIRECTIONS)
			 this.icons(i.ordinal) = blockIcon
		 // Then override if needed
		 (xOffset, zOffset) match
		 {
			 case (-1, -1) => // North-west
				 this.icons(UP.ordinal)    = topCorner
				 this.icons(WEST.ordinal)  = sideCorner
				 this.icons(NORTH.ordinal) = sideCorner.flippedU
			 case (0, -1) => // North
				 this.icons(UP.ordinal)    = topSide
				 this.icons(NORTH.ordinal) = sideSide
			 case (1, -1) => // North-east
				 this.icons(UP.ordinal)    = topCorner.flippedU
				 this.icons(NORTH.ordinal) = sideCorner
				 this.icons(EAST.ordinal)  = sideCorner.flippedU
			 case (1, 0) => // East
				 this.icons(UP.ordinal)    = topSide90
				 this.icons(EAST.ordinal)  = sideSide
			 case (1, 1) => // South-east
				 this.icons(UP.ordinal)    = topCorner.flippedUV
				 this.icons(EAST.ordinal)  = sideCorner
				 this.icons(SOUTH.ordinal) = sideCorner.flippedU
			 case (0, 1) => // South
				 this.icons(UP.ordinal)    = topSide.flippedV
				 this.icons(SOUTH.ordinal) = sideSide
			 case (-1, 1) => // South-west
				 this.icons(UP.ordinal)    = topCorner.flippedV
				 this.icons(SOUTH.ordinal) = sideCorner
				 this.icons(WEST.ordinal)  = sideCorner.flippedU
			 case (-1, 0) => // West
				 this.icons(UP.ordinal)    = topSide90.flippedU
				 this.icons(WEST.ordinal)  = sideSide
		 }
	 }

	 override def getIcon(side: Int, meta: Int): IIcon = icons(side)
 }
