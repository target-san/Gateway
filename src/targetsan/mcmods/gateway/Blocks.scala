package targetsan.mcmods.gateway
import net.minecraft.block.Block
import net.minecraft.block.BlockContainer
import net.minecraft.block.material.Material
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.entity.Entity
import net.minecraft.init.Blocks
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World

class BlockGatewayBase extends BlockContainer(Material.rock)
	with DropsNothing
	with Unbreakable
	with TeleportActor
	with MultiBlock[SubBlock]
{
	disableStats()
	setBlockName("GatewayBase")
	setStepSound(Block.soundTypePiston)
	
	val Core = 0 // Default core block
	
	registerSubBlocks(
		Core -> new SubBlockCore
	)
	
	override def hasTileEntity(meta: Int) =
		subBlock(meta).hasTileEntity(meta)

	override def createNewTileEntity(world: World, meta: Int) =
		subBlock(meta).createNewTileEntity(world, meta)

	override def onBlockAdded(world: World, x: Int, y: Int, z: Int) =
		subBlock(world, x, y, z).onBlockAdded(world, x, y, z)
	
	override def onBlockPreDestroy(world: World, x: Int, y: Int, z: Int, meta: Int) =
		subBlock(meta).onBlockPreDestroy(world, x, y, z, meta)

	override def randomDisplayTick(world: World, x: Int, y: Int, z: Int, random: java.util.Random) =
		subBlock(world, x, y, z).randomDisplayTick(world, x, y, z, random)
	
	override def teleportEntity(world: World, x: Int, y: Int, z: Int, entity: Entity) =
		subBlock(world, x, y, z).teleportEntity(world, x, y, z, entity)
	
	override def getIcon(side: Int, meta: Int) =
		subBlock(meta).getIcon(side, meta)

	override def registerBlockIcons(register: IIconRegister) =
		allSubBlocks foreach { _.registerBlockIcons(register) }
}

class SubBlockCore extends SubBlock
{
	setBlockTextureName("gateway:gateway")

	private val PortalHeight = 3
	
	override def hasTileEntity(meta: Int) = true
	override def createNewTileEntity(world: World, meta: Int) = new TileGateway

	override def onBlockAdded(world: World, x: Int, y: Int, z: Int)
	{
		// construct multiblock
		if (world.isRemote)
			return
		// Anti-liquid Nether shielding
		if (world.provider.dimensionId == Gateway.DIMENSION_ID)
			Utils.enumVolume(world, x - 1, y + 1, z - 1, x + 1, y + PortalHeight, z + 1).foreach
			{ case (x, y, z) =>
				world.setBlock(x, y, z, GatewayMod.BlockGatewayAir, GatewayMod.BlockGatewayAir.Shield, 3)
			}
		// Portal column
		for (y1 <- y+1 to y+PortalHeight )
			world.setBlock(x, y1, z, GatewayMod.BlockGatewayAir, GatewayMod.BlockGatewayAir.Portal, 3)
		
		// Nether stone platform
		if (world.provider.dimensionId == Gateway.DIMENSION_ID)
			Utils.enumVolume(world, x - 2, y, z - 2, x + 2, y, z + 2).foreach
			{ case (x, y, z) =>
				if (world.isAirBlock(x, y, z))
					world.setBlock(x, y, z, Blocks.stone)
			}
	}
	
	override def onBlockPreDestroy(world: World, x: Int, y: Int, z: Int, meta: Int)
	{
		if (world.isRemote)
			return
		
		Utils.enumVolume(world, x - 1, y + 1, z - 1, x + 1, y + PortalHeight, z + 1)
			.foreach
			{ case (x, y, z) =>
				if (world.getBlock(x, y, z) == GatewayMod.BlockGatewayAir)
					world.setBlockToAir(x, y, z)
			}

		world.getTileEntity(x, y, z).asInstanceOf[TileGateway].dispose
	}

	override def randomDisplayTick(world: World, x: Int, y: Int, z: Int, random: java.util.Random)
	{
		for (i <- 0 until 4)
			world.spawnParticle("portal", x + random.nextDouble(), y + 1.0, z + random.nextDouble(), 0.0, 1.5, 0.0)
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
	with MultiBlock[SubBlock]
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
