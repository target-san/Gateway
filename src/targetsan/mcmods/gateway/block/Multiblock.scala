package targetsan.mcmods.gateway.block

import net.minecraft.block.Block
import targetsan.mcmods.gateway.Assets
import targetsan.mcmods.gateway.Utils._

object Multiblock {
	// Tired of using tuples
	case class Part(block: Block, meta: Int, offset: BlockPos)

	val PillarHeight = 3
	// Map of all blocks included into this multiblock
	import Assets._

	lazy val Parts = Vector(
		/* C  */ Part(BlockPlatform, 0, BlockPos( 0, 0,  0) ),
		/* NW */ Part(BlockPlatform, 1, BlockPos(-1, 0, -1) ),
		/* N  */ Part(BlockPlatform, 2, BlockPos( 0, 0, -1) ),
		/* NE */ Part(BlockPlatform, 3, BlockPos( 1, 0, -1) ),
		/*  E */ Part(BlockPlatform, 4, BlockPos( 1, 0,  0) ),
		/* SE */ Part(BlockPlatform, 5, BlockPos( 1, 0,  1) ),
		/* S  */ Part(BlockPlatform, 6, BlockPos( 0, 0,  1) ),
		/* SW */ Part(BlockPlatform, 7, BlockPos(-1, 0,  1) ),
		/*  W */ Part(BlockPlatform, 8, BlockPos(-1, 0,  0) ),
		// Pillar
		/*  1 */ Part(BlockPillar,   0, BlockPos( 0, 1,  0) ),
		/*  2 */ Part(BlockPillar,   0, BlockPos( 0, 2,  0) ),
		/*  3 */ Part(BlockPillar,   0, BlockPos( 0, 3,  0) )
	)
}
