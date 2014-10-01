package targetsan.mcmods.gateway

import cpw.mods.fml.common.Mod
import net.minecraft.block.Block
import net.minecraft.block.BlockContainer
import net.minecraft.block.material.Material
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.entity.{EnumCreatureType, Entity}
import net.minecraft.init.{Blocks, Items}
import net.minecraft.world.World
import net.minecraft.util.IIcon
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.world.IBlockAccess
import net.minecraftforge.common.util.ForgeDirection
import net.minecraft.util.MovingObjectPosition
import net.minecraft.util.Vec3
import net.minecraft.util.AxisAlignedBB

import Utils._

class BlockGateway extends BlockContainer(Material.rock)
	with DropsNothing
	with Unbreakable 
	with TeleportActor 
	with MetaBlock[SubBlock]
 	with NotACube
 	with NoCreativePick
{
	disableStats()
	setBlockName("GatewayBase")
	setStepSound(Block.soundTypePiston)
	
	val RedstoneCore  = new SubBlockCore(RedstoneCoreMultiblock)
	val MirrorCore = new SubBlockCore(NetherMultiblock)
	val Pillar = new SubBlockPillar
	
	registerSubBlocks(
		Pillar,
		new SubBlockSatellite( -1, -1),
		new SubBlockSatellite(  0, -1),
		new SubBlockSatellite(  1, -1),
		new SubBlockSatellite(  1,  0),
		new SubBlockSatellite(  1,  1),
		new SubBlockSatellite(  0,  1),
		new SubBlockSatellite( -1,  1),
		new SubBlockSatellite( -1,  0),
		RedstoneCore, 
		MirrorCore 
	)
	
	private def subsOfType[T <: SubBlock](implicit m: Manifest[T]) = 
		allSubBlocks withFilter { i => m.runtimeClass.isInstance(i._2) } map { i => (i._1, i._2.asInstanceOf[T]) }
	
	def cores      = subsOfType[SubBlockCore]
	def satellites = subsOfType[SubBlockSatellite]
	
	// Tile entity events
	override def hasTileEntity(meta: Int) =
		subBlock(meta).hasTileEntity(meta)

	override def createNewTileEntity(world: World, meta: Int) =
		subBlock(meta).createNewTileEntity(world, meta)

	// Block events
	override def onBlockActivated(world: World, x: Int, y: Int, z: Int, player: EntityPlayer, side: Int, xTouch: Float, yTouch: Float, zTouch: Float) =
		subBlock(world, x, y, z).onBlockActivated(world, x, y, z, player, side, xTouch, yTouch, zTouch)
	
	override def onNeighborBlockChange(world: World, x: Int, y: Int, z: Int, block: Block) = 
		subBlock(world, x, y, z).onNeighborBlockChange(world, x, y, z, block)

	override def onBlockPreDestroy(world: World, x: Int, y: Int, z: Int, meta: Int) = 
		subBlock(meta).onBlockPreDestroy(world, x, y, z, meta)
	
	override def onEntityCollidedWithBlock(world: World, x: Int, y: Int, z: Int, entity: Entity) =
		subBlock(world, x, y, z).onEntityCollidedWithBlock(world, x, y, z, entity)

	// Teleporter
	override def teleportEntity(world: World, x: Int, y: Int, z: Int, entity: Entity) =
		subBlock(world, x, y, z).teleportEntity(world, x, y, z, entity)
	
	// Support for raytrace and selection box overrides
	override def getSelectedBoundingBoxFromPool(world: World, x: Int, y: Int, z: Int) =
		subBlock(world, x, y, z).getSelectedBoundingBoxFromPool(world, x, y, z)

	override def collisionRayTrace(world: World, x: Int, y: Int, z: Int, startVec: Vec3, endVec: Vec3): MovingObjectPosition =
		subBlock(world, x, y, z).collisionRayTrace(world, x, y, z, startVec, endVec)
	
	// Block geometry
	override def getCollisionBoundingBoxFromPool(world: World, x: Int, y: Int, z: Int) =
		subBlock(world, x, y, z).getCollisionBoundingBoxFromPool(world, x, y, z)

	override def addCollisionBoxesToList(world: World, x: Int, y: Int, z: Int, mask: AxisAlignedBB, boxes: java.util.List[_], entity: Entity) =
		subBlock(world, x, y, z).addCollisionBoxesToList(world, x, y, z, mask, boxes, entity)
	
	override def isNormalCube(world: IBlockAccess, x: Int, y: Int, z: Int): Boolean =
		subBlock(world, x, y, z).isNormalCube(world, x, y, z)
	
	// Light
	override def getLightValue(world: IBlockAccess, x: Int, y: Int, z: Int) =
		subBlock(world, x, y, z).getLightValue(world, x, y, z)
	
	// Render and other fanciness
	override def randomDisplayTick(world: World, x: Int, y: Int, z: Int, random: java.util.Random) =
		subBlock(world, x, y, z).randomDisplayTick(world, x, y, z, random)
	
	override def shouldSideBeRendered(world: IBlockAccess, x: Int, y: Int, z: Int, side: Int): Boolean =
	{
		val direction = ForgeDirection.getOrientation(side)
		// We receive neighbor's coordinates, so need un-offset to receive current block's coords and get valid meta
		subBlock(world, x - direction.offsetX, y - direction.offsetY, z - direction.offsetZ)
			.shouldSideBeRendered(world, x, y, z, side)
	}
		
	override def getIcon(side: Int, meta: Int) =
		subBlock(meta).getIcon(side, meta)

	override def registerBlockIcons(register: IIconRegister) =
		allSubBlocks foreach { _._2.registerBlockIcons(register) }
	// Cannot be destroyed by enderdragons, withers and other mobs
	override def canEntityDestroy(world: IBlockAccess, x: Int, y: Int, z: Int, entity: Entity) = false
	// Nothing spawns here
	override def canCreatureSpawn(creature: EnumCreatureType, world: IBlockAccess, x: Int, y: Int, z: Int) = false

	//******************************************************************************************************************
	// Redstone delegation
	//******************************************************************************************************************
	override def canProvidePower = true

	override def isProvidingStrongPower(world: IBlockAccess, x: Int, y: Int, z: Int, side: Int) =
		subBlock(world, x, y, z).isProvidingStrongPower(world, x, y, z, side)

	override def isProvidingWeakPower(world: IBlockAccess, x: Int, y: Int, z: Int, side: Int) =
		subBlock(world, x, y, z).isProvidingWeakPower(world, x, y, z, side)
}

class SubBlockCore(val multiblock: Multiblock) extends SubBlock(Material.rock)
{
	protected var blockTopIcon: IIcon = null
	
	override def hasTileEntity(meta: Int) = true
	override def createNewTileEntity(world: World, meta: Int) = new TileGateway

	override def registerBlockIcons(icons: IIconRegister)
	{
		blockIcon = icons.registerIcon("gateway:smooth_obsidian")
		blockTopIcon = icons.registerIcon("gateway:top-center")
	}
	
	override def getIcon(side: Int, meta: Int): IIcon =
		if (side == 1) blockTopIcon
		else           blockIcon
	
	override def teleportEntity(world: World, x: Int, y: Int, z: Int, entity: Entity) =
		world.getTileEntity(x, y, z).asInstanceOf[TileGateway].teleportEntity(entity)
}

class SubBlockSatellite(val xOffset: Int, val zOffset: Int) extends SubBlock(Material.rock)
{
	val isDiagonal = xOffset != 0 && zOffset != 0
	val side = (xOffset, zOffset) match {
		case (0, -1) => 0
		case (1, 0)  => 1
		case (0, 1)  => 2
		case (-1, 0) => 3
		case _ => -1
	}
	setBlockTextureName("gateway:smooth_obsidian")
	
	private val icons = Array.fill[IIcon](6)(null)

	override def hasTileEntity(meta: Int) = true
	override def createNewTileEntity(world: World, meta: Int) = new TileSatellite

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
			.foreach { _.onNeighborChanged() }

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
	
	override def onBlockPreDestroy(world: World, x: Int, y: Int, z: Int, meta: Int)
	{
		val tile = world
			.getTileEntity(x - xOffset, y, z - zOffset)
		if (tile != null)
			tile.invalidate()
	}

	override def registerBlockIcons(icons: IIconRegister)
	{
		import ForgeDirection._
		import Utils._
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

	//******************************************************************************************************************
	// Redstone connector support
	//******************************************************************************************************************
	override def isProvidingStrongPower(world: IBlockAccess, x: Int, y: Int, z: Int, side: Int) =
		world.getTileEntity(x, y, z).as[TileSatellite] map { _.getRedstoneStrongPower(ForgeDirection.getOrientation(side)) } getOrElse 0


	override def isProvidingWeakPower(world: IBlockAccess, x: Int, y: Int, z: Int, side: Int) =
		world.getTileEntity(x, y, z).as[TileSatellite] map { _.getRedstoneWeakPower(ForgeDirection.getOrientation(side)) } getOrElse 0
}

class SubBlockPillar extends SubBlock(Material.air)
	with NotCollidable
	with NotActivable
{
	setBlockTextureName("gateway:pillar")
	setLightLevel(1.0F)
	
	override def onEntityCollidedWithBlock(world: World, x: Int, y: Int, z: Int, entity: Entity) =
	{
		teleportEntity(world, x, y, z, entity)
	}
	
	override def teleportEntity(world: World, x: Int, y: Int, z: Int, entity: Entity) =
	{
		val below = world.getBlock(x, y - 1, z)
		below.as[TeleportActor] foreach { _.teleportEntity(world, x, y - 1, z, entity) }
	}
	
	override def shouldSideBeRendered(world: IBlockAccess, x: Int, y: Int, z: Int, side: Int): Boolean =
		side match
		{
		case 0 => false
		case 1 => world.getBlock(x, y, z) != GatewayMod.BlockGateway
		case _ => super.shouldSideBeRendered(world, x, y, z, side)
		}
}
