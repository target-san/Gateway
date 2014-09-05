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
import net.minecraft.item.ItemStack

trait MetaBlock[T <: MetaPart] extends Block
{
	private val SubBlocksCount = 16 // block's meta is 4-bit
	private var table: Map[Int, T] = null
	
	protected def registerSubBlocks(blocks: T*)(implicit manifest: Manifest[T]) =
	{
		if (table != null)
			throw new IllegalStateException("Sub-blocks table can be initialized only once")
		val metaRange = 0 until SubBlocksCount
		if (blocks.length > SubBlocksCount)
			throw new IllegalArgumentException(s"There should be no more than ${SubBlocksCount} meta-block parts")
		table = blocks.zipWithIndex.map(x => (x._2, x._1)).toMap.filter(_ != null)
		for ((k, v) <- table)
		{
			v.meta = k
			v.metaBlock = this
		}
	}
	
	def subBlock(meta: Int) = table(meta)
	def subBlock(world: IBlockAccess, x: Int, y: Int, z: Int) = table(world.getBlockMetadata(x, y, z))
	
	protected def allSubBlocks = table
}

trait MetaPart
{
	private var _meta = -1
	def meta_=(m: Int): Unit =
		if (_meta >= 0 && _meta < 16) throw new IllegalStateException("Part's meta can be initialized only once")
		else if (m < 0 || m >= 16) throw new IllegalArgumentException("Metablock part's meta must be in range [0..15]")
		else _meta = m
	
	def meta: Int = _meta
	
	private var _metaBlock: Block = null
	def metaBlock_=(b: Block): Unit =
		if (_metaBlock != null) throw new IllegalStateException("Part's meta parent meta block reference can be initialized only once")
		else if (b == null) throw new IllegalArgumentException("Metablock reference must be non-null")
		else _metaBlock = b
		
	def metaBlock: Block = _metaBlock
	
	def place(world: World, x: Int, y: Int, z: Int, flags: Int = 3) =
		world.setBlock(x, y, z, metaBlock, meta, flags)
}
// Not actually a trait, but a base class for all sub-blocks
class SubBlock(material: Material) extends BlockContainer(material)
	with TeleportActor
	with MetaPart
{
	override def createNewTileEntity(world: World, meta: Int): TileEntity = null
	override def hasTileEntity(meta: Int) = false
	
	override def teleportEntity(w: World, x: Int, y: Int, z: Int, entity: Entity) { }
	// Workaround, default impl will lead to infinite recursion
	override def getLightValue(world: IBlockAccess, x: Int, y: Int, z: Int) = getLightValue()
}

trait TeleportActor
{
	def teleportEntity(world: World, x: Int, y: Int, z: Int, entity: Entity)
}

trait DropsNothing extends Block
{
	override def getItemDropped(meta: Int, random: java.util.Random, fortune: Int): Item = null
}
// There's no sense in picking gateway block - it works properly only when properly initialized
trait NoCreativePick extends Block
{
	override def getPickBlock(target: MovingObjectPosition, world: World, x: Int, y: Int, z: Int): ItemStack = null
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