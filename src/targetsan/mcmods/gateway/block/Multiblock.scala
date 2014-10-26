package targetsan.mcmods.gateway.block

import cpw.mods.fml.common.eventhandler.SubscribeEvent
import net.minecraft.block.Block
import net.minecraft.init.Blocks
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import targetsan.mcmods.gateway._
import targetsan.mcmods.gateway.Utils._

object Multiblock {
	// Tired of using tuples
	case class Part(block: Block, meta: Int, offset: BlockPos, tile: Option[() => TileEntity])

	final case class BlockType(block: Block, meta: Int) {
		def isAt(world: World, pos: BlockPos) =
			world.getBlock(pos.x, pos.y, pos.z) == block &&
			{ if (0 to 15 contains meta) world.getBlockMetadata(pos.x, pos.y, pos.z) == meta else true }
	}

	@SubscribeEvent
	def onFlintAndSteelPreUse(event: PlayerInteractEvent): Unit =
		if (!event.world.isRemote) // Works only server-side
        if (event.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK)
		// We're interested in Flint'n'Steel clicking some block only
		if (event.entityPlayer != null)
		if (event.entityPlayer.getHeldItem != null)
		if (event.entityPlayer.getHeldItem.getItem == net.minecraft.init.Items.flint_and_steel)
		if (ConstructFrom forall { // Check blocks in vicinity against multiblock pattern
			case (offset, test) => test(event.world, BlockPos(event.x, event.y, event.z) + offset)
		})
		ExitLocator.netherExit(event.world, BlockPos(event.x, event.y, event.z)) match {
			case Left(msg) => Chat.error(event.entityPlayer, msg)
			case Right((toPos, toWorld)) =>
				val fromPos = BlockPos(event.x, event.y, event.z)
				val fromWorld = event.world
				// Construct raw blocks
				rawAssemble(Parts, fromPos, event.world)
				rawAssemble(NetherParts, toPos, toWorld)
				// Initialize stuff
				val fromTile = fromWorld.getTileEntity(fromPos.x, fromPos.y, fromPos.z).as[tile.Core].get
				val toTile   = toWorld.getTileEntity(toPos.x, toPos.y, toPos.z).as[tile.Core].get

				fromTile.init(event.entityPlayer, toTile)
				toTile.init(event.entityPlayer, fromTile)

				Chat.ok(event.entityPlayer, "ok.gateway-constructed", fromWorld.provider.getDimensionName, toWorld.provider.getDimensionName)
		}
	// Used as a list, but logically is a map
	private lazy val ConstructFrom = Map[BlockPos, (World, BlockPos) => Boolean](
		BlockPos(0, 0, 0) -> isSimpleBlock(Blocks.redstone_block),
		obsidian( -1, -1),
		obsidian(  1, -1),
		obsidian( -1,  1),
		obsidian(  1,  1),
		glass   ( -1,  0),
		glass   (  1,  0),
		glass   (  0, -1),
		glass   (  0,  1)
	)

	private def isSimpleBlock(block: Block) = BlockType(block, 0).isAt _

	private def obsidian(dx: Int, dz: Int) = BlockPos(dx, 0, dz) -> isSimpleBlock(Blocks.obsidian)
	private def glass(dx: Int, dz: Int) = BlockPos(dx, 0, dz) -> isSimpleBlock(Blocks.glass)
	// Using common replacement procedure for now
	def disassemble(world: World, pos: BlockPos): Unit =
		for ( part <- Parts) {
			val p = pos + part.offset
			part.block match {
				case Assets.BlockPillar =>
					world.setBlockToAir(p.x, p.y, p.z)
				case Assets.BlockPlatform =>
					world.setBlock(p.x, p.y, p.z,
						if      (part.meta == 0)     Blocks.netherrack
						else if (part.meta % 2 == 0) Blocks.gravel
						else                         Blocks.obsidian
					)
				case _ => ()
			}
		}

	val PillarHeight = 3
	// Map of all blocks included into this multiblock
	lazy val Parts = Vector(
		/* C  */ Part( Assets.BlockPlatform, 0, BlockPos(0, 0, 0), Some( () => new tile.Core) ),
		/* NW */ perimeter( meta = 1, dx = -1, dz = -1),
		/* N  */ perimeter( meta = 2, dx =  0, dz = -1),
		/* NE */ perimeter( meta = 3, dx =  1, dz = -1),
		/*  E */ perimeter( meta = 4, dx =  1, dz =  0),
		/* SE */ perimeter( meta = 5, dx =  1, dz =  1),
		/* S  */ perimeter( meta = 6, dx =  0, dz =  1),
		/* SW */ perimeter( meta = 7, dx = -1, dz =  1),
		/*  W */ perimeter( meta = 8, dx = -1, dz =  0),
		// Pillar
		/*  1 */ pillar   ( dy = 1 ),
		/*  2 */ pillar   ( dy = 2 ),
		/*  3 */ pillar   ( dy = 3 )
	)
	// Used only in nether, adds air blocks around
	private lazy val NetherParts =
		Parts ++ {
			for ( dx <- -1 to 1; dy <-  1 to PillarHeight; dz <- -1 to 1; if dx != 0 && dz != 0)
				yield Part(Blocks.air, 0, BlockPos(dx, dy, dz), None)
		}

	private def perimeter(meta: Int, dx: Int, dz: Int) =
		Part( Assets.BlockPlatform, meta, BlockPos(dx,  0, dz), Some( () => new tile.Perimeter) )

	private def pillar(dy: Int) =
		Part( Assets.BlockPillar,   0,    BlockPos( 0, dy,  0), None )

	private def rawAssemble(parts: Seq[Part], pos: BlockPos, world: World) =
		for ( p <- parts )
			world.setBlock(pos.x + p.offset.x, pos.y + p.offset.y, pos.z + p.offset.z, p.block, p.meta, 3)
}
