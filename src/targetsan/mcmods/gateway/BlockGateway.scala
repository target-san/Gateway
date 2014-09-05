package targetsan.mcmods.gateway
import cpw.mods.fml.common.Mod
import net.minecraft.block.Block
import net.minecraft.block.BlockContainer
import net.minecraft.block.material.Material
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.entity.Entity
import net.minecraft.init.{Blocks, Items}
import net.minecraft.world.World
import net.minecraft.util.IIcon
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.world.IBlockAccess
import net.minecraft.util.Facing
import net.minecraftforge.common.util.ForgeDirection
import net.minecraft.util.MovingObjectPosition
import net.minecraft.util.Vec3
import net.minecraft.util.AxisAlignedBB

class BlockGateway extends BlockContainer(Material.rock)
	with DropsNothing
	with Unbreakable 
	with TeleportActor 
	with MetaBlock[SubBlock] 	with NotACube
{
	disableStats()
	setBlockName("GatewayBase")
	setStepSound(Block.soundTypePiston)
	
	val RedstoneCore  = new SubBlockCore(RedstoneCoreMultiblock)
	val MirrorCore = new SubBlockCore(NetherMultiblock)
	val Pillar = new SubBlockPillar
	
	registerSubBlocks(
		RedstoneCore, 
		new SubBlockSatellite( -1, -1, "minecraft:obsidian"),
		new SubBlockSatellite(  0, -1, "minecraft:obsidian", 0),
		new SubBlockSatellite(  1, -1, "minecraft:obsidian"),
		new SubBlockSatellite(  1,  0, "minecraft:obsidian", 1),
		new SubBlockSatellite(  1,  1, "minecraft:obsidian"),
		new SubBlockSatellite(  0,  1, "minecraft:obsidian", 2),
		new SubBlockSatellite( -1,  1, "minecraft:obsidian"),
		new SubBlockSatellite( -1,  0, "minecraft:obsidian", 3),
		MirrorCore, 
		Pillar 
	)
	
	private def subsOfType[T <: SubBlock](implicit m: Manifest[T]) = 
		allSubBlocks withFilter { i => m.erasure.isInstance(i._2) } map { i => (i._1, i._2.asInstanceOf[T]) }
	
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
}

class SubBlockCore(val multiblock: Multiblock) extends SubBlock(Material.rock)
{
	protected var blockTopIcon: IIcon = null
	
	override def hasTileEntity(meta: Int) = true
	override def createNewTileEntity(world: World, meta: Int) = new TileGateway

	override def registerBlockIcons(icons: IIconRegister)
	{
		blockIcon = icons.registerIcon("minecraft:obsidian")
		blockTopIcon = icons.registerIcon("minecraft:portal")
	}
	
	override def getIcon(side: Int, meta: Int): IIcon =
		if (side == 1) blockTopIcon
		else           blockIcon
	
	override def teleportEntity(world: World, x: Int, y: Int, z: Int, entity: Entity) =
		world.getTileEntity(x, y, z).asInstanceOf[TileGateway].teleportEntity(entity)
}

class SubBlockSatellite(val xOffset: Int, val zOffset: Int, textureName: String, private val side: Int = -1) extends SubBlock(Material.rock)
{
	val isDiagonal = xOffset != 0 && zOffset != 0
	setBlockTextureName(textureName)
	
	override def onBlockActivated(world: World, x: Int, y: Int, z: Int, player: EntityPlayer, side: Int, xTouch: Float, yTouch: Float, zTouch: Float): Boolean =
	{
		if (world.isRemote ||
			isDiagonal ||
			side != 1 ||
			player.getHeldItem == null ||
			player.getHeldItem.getItem != Items.flint_and_steel
		)
			return false
		
		val tile = world
			.getTileEntity(x - xOffset, y, z - zOffset)
			.asInstanceOf[TileGateway]
		if (tile != null)
			tile.markForDispose(player, this.side)
		
		false
	}
	
	override def onNeighborBlockChange(world: World, x: Int, y: Int, z: Int, block: Block)
	{
		if (world.isRemote ||
			isDiagonal ||
			world.getBlock(x, y + 1, z) == Blocks.fire
		)
			return
		
		val tile = world
			.getTileEntity(x - xOffset, y, z - zOffset)
			.asInstanceOf[TileGateway]
		if (tile != null)
			tile.unmarkForDispose(this.side)
	}
	
	override def onBlockPreDestroy(world: World, x: Int, y: Int, z: Int, meta: Int)
	{
		val tile = world
			.getTileEntity(x - xOffset, y, z - zOffset)
		if (tile != null)
			tile.invalidate()
	}
}

class SubBlockPillar extends SubBlock(Material.air)
	with NotCollidable
	with NotActivable
{
	setBlockTextureName("gateway:pillar")
	
	override def onEntityCollidedWithBlock(world: World, x: Int, y: Int, z: Int, entity: Entity) =
	{
		teleportEntity(world, x, y, z, entity)
	}
	
	override def teleportEntity(world: World, x: Int, y: Int, z: Int, entity: Entity) =
	{
		val below = world.getBlock(x, y - 1, z)
		if (below.isInstanceOf[TeleportActor])
			below.asInstanceOf[TeleportActor].teleportEntity(world, x, y - 1, z, entity)
	}
	
	override def shouldSideBeRendered(world: IBlockAccess, x: Int, y: Int, z: Int, side: Int): Boolean =
		side match
		{
		case 0 => false
		case 1 => world.getBlock(x, y, z) != GatewayMod.BlockGateway
		case _ => super.shouldSideBeRendered(world, x, y, z, side)
		}
}
