package targetsan.mcmods.gateway

import net.minecraft.world.IBlockAccess
import net.minecraft.entity.Entity
import net.minecraft.world.World
import net.minecraft.block.Block
import net.minecraft.item.Item
import net.minecraft.util.AxisAlignedBB
import net.minecraft.block.BlockContainer
import net.minecraft.tileentity.TileEntity
import net.minecraft.block.material.Material
import net.minecraft.util.Vec3
import net.minecraft.util.MovingObjectPosition

trait MetaBlock[T >: Null]
{
	private val SubBlocksCount = 16 // block's meta is 4-bit
	private var table: Map[Int, T] = null
	
	protected def registerSubBlocks(blocks: (Int, T)*)(implicit manifest: Manifest[T]) =
	{
		if (table != null)
			throw new IllegalStateException("Sub-blocks table can be initialized only once")
		val metaRange = 0 until SubBlocksCount
		if (blocks exists { i => !(metaRange contains i._1) })
			throw new IllegalArgumentException(s"Sub-block indices must be in range [0..${SubBlocksCount})")
		table = blocks.toMap
	}
	
	def subBlock(meta: Int) = table(meta)
	def subBlock(world: IBlockAccess, x: Int, y: Int, z: Int) = table(world.getBlockMetadata(x, y, z))
	
	protected def allSubBlocks = table
}
// Not actually a trait, but a base class for all sub-blocks
class SubBlock extends BlockContainer(Material.air) with TeleportActor
{
	override def createNewTileEntity(world: World, meta: Int): TileEntity = null
	override def hasTileEntity(meta: Int) = false
	
	override def teleportEntity(w: World, x: Int, y: Int, z: Int, entity: Entity) { }
}

trait TeleportActor
{
	def teleportEntity(world: World, x: Int, y: Int, z: Int, entity: Entity)
}

trait DropsNothing extends Block
{
	override def getItemDropped(meta: Int, random: java.util.Random, fortune: Int): Item = null
}

trait NotACube extends Block
{
	override def renderAsNormalBlock = false
	override def isOpaqueCube = false
	override def isBlockNormalCube = false
	override def getRenderBlockPass = 1
}

trait Unbreakable extends Block
{
	setBlockUnbreakable()
	setResistance(6000000.0F)
}

trait NotCollidable extends Block
{
	override def getCollisionBoundingBoxFromPool(world: World, x: Int, y: Int, z: Int) = null
	override def addCollisionBoxesToList(world: World, x: Int, y: Int, z: Int, mask: AxisAlignedBB, boxes: java.util.List[_], entity: Entity) { }
}

trait NotActivable extends Block
{
	override def getSelectedBoundingBoxFromPool(world: World, x: Int, y: Int, z: Int) = null
	override def collisionRayTrace(world: World, x: Int, y: Int, z: Int, startVec: Vec3, endVec: Vec3): MovingObjectPosition = null
}