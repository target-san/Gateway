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
	@SubscribeEvent
	def onFlintAndSteelPreUse(event: PlayerInteractEvent): Unit =
		if (!event.entityPlayer.worldObj.isRemote) // Works only server-side
		// We're interested in Flint'n'Steel clicking some block only
		if (event.entityPlayer != null)
		if (event.entityPlayer.getHeldItem != null)
		if (event.entityPlayer.getHeldItem.getItem == net.minecraft.init.Items.flint_and_steel)
		if (event.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) {
			// TODO: start multiblock construction here
		}

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
			}
		}

	// Tired of using tuples
	case class Part(block: Block, meta: Int, offset: BlockPos, tile: Option[() => TileEntity])

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

	private def perimeter(meta: Int, dx: Int, dz: Int) =
		Part( Assets.BlockPlatform, meta, BlockPos(dx,  0, dz), Some( () => new tile.Perimeter) )

	private def pillar(dy: Int) =
		Part( Assets.BlockPillar,   0,    BlockPos( 0, dy,  0), None )

}
