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
import net.minecraft.entity.player.EntityPlayer
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraft.init.Blocks
import net.minecraft.util.ChatComponentText
import net.minecraft.world.IBlockAccess

object Gateway
{
	val DIMENSION_ID = -1 // Nether
	def dimension = Utils.world(DIMENSION_ID)
	
    @SubscribeEvent
    def onFlintAndSteelPreUse(event: PlayerInteractEvent): Unit =
    {
		if (event.entityPlayer.worldObj.isRemote) // Works only server-side
			return
		// We're interested in Flint'n'Steel clicking some block only
		if (event.entityPlayer == null ||
			event.entityPlayer.getHeldItem == null ||
			event.entityPlayer.getHeldItem.getItem != net.minecraft.init.Items.flint_and_steel ||
			event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK
		)
			return
		// Try place gateway here
		tryPlaceGateway(event.entityPlayer.worldObj, event.x, event.y, event.z, event.entityPlayer)
    }

	private def tryPlaceGateway(w: World, x: Int, y: Int, z: Int, player: EntityPlayer)
	{
		// Check if there's multiblock present
		if (!isMultiblockPresent(w, x, y, z))
			return
		
		val to = Gateway.dimension
		val (ex, ey, ez) = getExit(w, x, y, z)
		if (w.provider.dimensionId == Gateway.DIMENSION_ID)
		{
			player.addChatMessage(new ChatComponentText("Gateways cannot be constructed from Nether"))
			return
		}
		// Check dead zone on the other side
		if (!isDestinationFree(to, ex, ey, ez))
		{
			player.addChatMessage(new ChatComponentText("Gateway cannot be constructed here - there's another gateway too near on the other side"))
			return
		}
		// Construct gateways on both sides
		w.setBlock(x, y, z, GatewayMod.BlockGatewayBase)
		w.getTileEntity(x, y, z).asInstanceOf[TileGateway].init(ex, ey, ez, player)
		player.addChatMessage(new ChatComponentText(s"Gateway successfully constructed from ${w.provider.getDimensionName} to ${Gateway.dimension.provider.getDimensionName}"))
	}
	
	private def isMultiblockPresent(w: World, x: Int, y: Int, z: Int) =
		// corners
		w.getBlock(x - 1, y, z - 1) == Blocks.obsidian &&
		w.getBlock(x + 1, y, z - 1) == Blocks.obsidian &&
		w.getBlock(x - 1, y, z + 1) == Blocks.obsidian &&
		w.getBlock(x + 1, y, z + 1) == Blocks.obsidian &&
		// sides
		w.getBlock(x - 1, y, z) == Blocks.glass &&
		w.getBlock(x + 1, y, z) == Blocks.glass &&
		w.getBlock(x, y, z - 1) == Blocks.glass &&
		w.getBlock(x, y, z + 1) == Blocks.glass &&
		 // center
		w.getBlock(x, y, z) == Blocks.redstone_block
	
	private def getExit(from: World, x: Int, y: Int, z: Int): (Int, Int, Int) =
	{
		val to = Gateway.dimension
		def mapCoord(c: Int) = Math.round(c * from.provider.getMovementFactor() / to.provider.getMovementFactor()).toInt 
		(mapCoord(x), (to.provider.getActualHeight - 1) / 2, mapCoord(z))
	}
    // Checks if there are no active gateways in the nether too near
	private def isDestinationFree(to: World, x: Int, y: Int, z: Int): Boolean =
	{
		val Radius = 7
		Utils.enumVolume(to,
				x - Radius, 0, z - Radius,
				x + Radius, to.provider.getActualHeight - 1, z + Radius
			)
			.forall { case (x, y, z) => to.getBlock(x, y, z) != GatewayMod.BlockGatewayBase }
	}
}

class BlockGatewayBase extends BlockContainer(Material.rock)
	with DropsNothing
	with Unbreakable
	with TeleportActor
	with MultiBlock[Block]
{
	disableStats()
	setBlockName("GatewayBase")
	setBlockTextureName("gateway:gateway")
	setStepSound(Block.soundTypePiston)
	
	val Core = 0 // Default core, 
	
	private val PortalHeight = 3
	
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
	with MultiBlock[Block with TeleportActor]
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

class SubBlock extends BlockContainer(Material.air) with TeleportActor
{
	override def createNewTileEntity(world: World, meta: Int): TileEntity = null
	override def hasTileEntity(meta: Int) = false
	
	override def teleportEntity(w: World, x: Int, y: Int, z: Int, entity: Entity) { }
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
	
	protected def subBlock(meta: Int) = table(meta)
	protected def subBlock(world: IBlockAccess, x: Int, y: Int, z: Int) = table(world.getBlockMetadata(x, y, z))
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
