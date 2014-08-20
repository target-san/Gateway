package targetsan.mcmods.gateway
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.entity.Entity
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World

class BlockGatewayAir extends Block(Material.portal)
	with DropsNothing
	with NotACube
	with Unbreakable
	with Ghostly
	with TeleportActor
	with MetaBlock[SubBlock]
{
	disableStats()
	setBlockName("GatewayAir")
	setBlockTextureName("minecraft:stone")
	
	val Portal = 0
	val Shield = 1
	
	registerSubBlocks(
		Portal -> new GatewayPortal,
		Shield -> new GatewayShield
	)
	
	def placePortal(world: World, x: Int, y: Int, z: Int) =
		world.setBlock(x, y, z, this, Portal, 3)
	
	def placeShield(world: World, x: Int, y: Int, z: Int) =
		world.setBlock(x, y, z, this, Shield, 3)

	override def onEntityCollidedWithBlock(world: World, x: Int, y: Int, z: Int, entity: Entity) =
		subBlock(world, x, y, z).onEntityCollidedWithBlock(world, x, y, z, entity)
	
	override def teleportEntity(world: World, x: Int, y: Int, z: Int, entity: Entity) =
		subBlock(world, x, y, z).teleportEntity(world, x, y, z, entity)
		
	override def isReplaceable(world: IBlockAccess, x: Int, y: Int, z: Int) =
		subBlock(world, x, y, z).isReplaceable(world, x, y, z)
}

// Represents actual 'portal' block, which reacts on collisions
class GatewayPortal extends SubBlock
{
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
// Anti-liquid shield, spawns in Nether, replaceable by player
class GatewayShield extends SubBlock
{
	override def isReplaceable(world: IBlockAccess, x: Int, y: Int, z: Int) = true
}
