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
	final case class BlockType(block: Block, meta: Int) {
		def isAt(world: World, pos: BlockPos) =
			world.getBlock(pos.x, pos.y, pos.z) == block &&
			{ if (0 to 15 contains meta) world.getBlockMetadata(pos.x, pos.y, pos.z) == meta else true }

		def place(world: World, pos: BlockPos) =
			world.setBlock(pos.x, pos.y, pos.z, block, meta, 3)
	}

	object BlockType {
		val air = BlockType(Blocks.air, 0)
		def at(world: World, pos: BlockPos) =
			BlockType(world.getBlock(pos.x, pos.y, pos.z), world.getBlockMetadata(pos.x, pos.y, pos.z))
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
	def disassemble(world: World, center: BlockPos, replacers: Map[BlockPos, BlockType] = Map.empty): Unit =
		for ( offset <- Parts.keys ++ replacers.keys) {
			val pos = center + offset
			(Parts get offset, replacers get offset) match {
				case (Some(_), x) => x.getOrElse(BlockType.air).place(world, pos)
				case (_, Some(oldBlock)) if oldBlock != BlockType.air => oldBlock.place(world, pos)
				case _ => ()
			}
		}

	val PillarHeight = 3
	// Map of all blocks included into this multiblock
	lazy val Parts = Map[BlockPos, BlockType](
		/* C  */ BlockPos(0, 0, 0) -> BlockType(Assets.BlockPlatform, 0),
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

	private[block] val TileEntities: PartialFunction[BlockType, TileEntity] = {
		case BlockType(Assets.BlockPlatform, 0) => new tile.Core
		case BlockType(Assets.BlockPlatform, m) if 1 to 8 contains m => new tile.Perimeter
	}
	// Used only in nether, adds air blocks around
	private lazy val NetherParts =
		Volume(-1, 1, -1, 1, PillarHeight, 1).enum.map( _ -> BlockType(Blocks.air, 0) ).toMap ++: Parts

	private def perimeter(meta: Int, dx: Int, dz: Int) =
		BlockPos(dx, 0, dz) -> BlockType(Assets.BlockPlatform, meta)

	private def pillar(dy: Int) =
		BlockPos(0, dy, 0) -> BlockType(Assets.BlockPillar, 0)

	private def rawAssemble(parts: Map[BlockPos,BlockType], center: BlockPos, world: World) =
		for ( (offset, part) <- parts ) yield {
			val pos = center + offset
			val oldType =
				if (world.isAirBlock(pos.x, pos.y, pos.z)) BlockType.air // a bit'o hack I suppose. Airy blocks are not restored.
				else BlockType.at(world, pos)
			part.place(world, pos)
			(offset, oldType) // old blocks are stored with offsets from core, not actual positions
		}
}
