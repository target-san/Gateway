package targetsan.mcmods.gateway
import net.minecraft.entity.Entity
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.block.{Block, BlockContainer}
import net.minecraft.world.World
import net.minecraft.tileentity.TileEntity
import net.minecraft.block.material.Material
import net.minecraft.util.IIcon
import net.minecraft.item.Item
import net.minecraft.entity.Entity
import net.minecraft.util.AxisAlignedBB

class BlockGatewayBase extends BlockContainer(Material.rock)
	with DropsNothing
	with Unbreakable
	with TeleportActor
{
	disableStats()
	setBlockName("GatewayBase")
	setBlockTextureName("gateway:gateway")
	setStepSound(Block.soundTypePiston)
	
	override def createNewTileEntity(world: World, meta: Int) = new TileGateway
	override def onBlockAdded(world: World, x: Int, y: Int, z: Int)
	{
		if (world.isRemote)
			return
		for (y1 <- y+1 to y+3 )
			world.setBlock(x, y1, z, GatewayMod.BlockGatewayAir)
	}
	
	override def randomDisplayTick(world: World, x: Int, y: Int, z: Int, random: java.util.Random)
	{
		for (i <- 0 until 4)
			world.spawnParticle("portal", x + random.nextDouble(), y + 1.0, z + random.nextDouble(), 0.0, 1.5, 0.0)
	}
	
	override def onBlockPreDestroy(world: World, x: Int, y: Int, z: Int, meta: Int)
	{
		if (world.isRemote)
			return
		for (y1 <- y+1 to y+3 )
			world.setBlockToAir(x, y1, z)
	}

	override def teleportEntity(world: World, x: Int, y: Int, z: Int, entity: Entity)
	{
		world.getTileEntity(x, y, z).asInstanceOf[TileGateway].teleportEntity(entity)
	}
}

class BlockGatewayAir extends Block(Material.portal)
	with DropsNothing
	with NotACube
	with Unbreakable
	with Ghostly
	with TeleportActor
{
	disableStats()
	setBlockName("GatewayAir")
	
	override def onEntityCollidedWithBlock(world: World, x: Int, y: Int, z: Int, entity: Entity)
	{
		teleportEntity(world, x, y, z, entity)
	}
	
	override def teleportEntity(world: World, x: Int, y: Int, z: Int, entity: Entity)
	{
		val below = world.getBlock(x, y - 1, z)
		if (below.isInstanceOf[TeleportActor])
			below.asInstanceOf[TeleportActor].teleportEntity(world, x, y - 1, z, entity)
	}
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