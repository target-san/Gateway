package targetsan.mcmods.gateway.block

import net.minecraft.block.Block
import net.minecraft.entity.{EnumCreatureType, Entity}
import net.minecraft.item.{Item, ItemStack}
import net.minecraft.util.{AxisAlignedBB, MovingObjectPosition, Vec3}
import net.minecraft.world.{IBlockAccess, World}

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

	// No idea where else to put this
	override def canCreatureSpawn(t :EnumCreatureType, world: IBlockAccess, x: Int, y: Int, z: Int) = false
	// Damn! No stupid enderdragon would break my portals!
	override def canEntityDestroy(world: IBlockAccess, x: Int, y: Int, z: Int, entity: Entity) = false
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