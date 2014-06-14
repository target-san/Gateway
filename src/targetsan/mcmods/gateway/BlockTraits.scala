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

trait MultiBlock[T >: Null]
{
	private val SubBlocksCount = 16 // block's meta is 4-bit
	private var table: Seq[T] = null
	
	protected def registerSubBlocks(blocks: (Int, T)*)(implicit manifest: Manifest[T]) =
	{
		if (table != null)
			throw new IllegalStateException("Sub-blocks table can be initialized only once")
		val array = Array.fill[T](SubBlocksCount)(null)
		blocks foreach { el => array(el._1) = el._2 }
		table = array
	}
	
	def subBlock(meta: Int) = table(meta)
	def subBlock(world: IBlockAccess, x: Int, y: Int, z: Int) = table(world.getBlockMetadata(x, y, z))
	
	protected def allSubBlocks = table.toIterable.filter(_ != null)
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
}

trait Unbreakable extends Block
{
	setBlockUnbreakable()
	setResistance(6000000.0F)
}

trait Ghostly extends Block
{
	setBlockBounds(0, 0, 0, 0, 0, 0)
	
	override def getSelectedBoundingBoxFromPool(world: World, x: Int, y: Int, z: Int) = null
	override def getCollisionBoundingBoxFromPool(world: World, x: Int, y: Int, z: Int) = null
	override def addCollisionBoxesToList(world: World, x: Int, y: Int, z: Int, mask: AxisAlignedBB, boxes: java.util.List[_], entity: Entity) { }
}
