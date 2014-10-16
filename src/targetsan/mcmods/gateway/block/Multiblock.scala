package targetsan.mcmods.gateway.block

import net.minecraft.block.Block
import targetsan.mcmods.gateway.Assets
import targetsan.mcmods.gateway.Utils._

object Multiblock {
	val PillarHeight = 3
	// Map of all blocks included into this multiblock
	lazy val Parts = Vector[(BlockPos, (Block, Int))](
		/* C  */ BlockPos( 0, 0,  0) -> (Assets.BlockPlatform, 0),
		/* NW */ BlockPos(-1, 0, -1) -> (Assets.BlockPlatform, 1),
		/* N  */ BlockPos( 0, 0, -1) -> (Assets.BlockPlatform, 2),
		/* NE */ BlockPos( 1, 0, -1) -> (Assets.BlockPlatform, 3),
		/*  E */ BlockPos( 1, 0,  0) -> (Assets.BlockPlatform, 4),
		/* SE */ BlockPos( 1, 0,  1) -> (Assets.BlockPlatform, 5),
		/* S  */ BlockPos( 0, 0,  1) -> (Assets.BlockPlatform, 6),
		/* SW */ BlockPos(-1, 0,  1) -> (Assets.BlockPlatform, 7),
		/*  W */ BlockPos(-1, 0,  0) -> (Assets.BlockPlatform, 8),
		// Pillar
		/*  1 */ BlockPos( 0, 1,  0) -> (Assets.BlockPillar,  0),
		/*  2 */ BlockPos( 0, 2,  0) -> (Assets.BlockPillar,  0),
		/*  3 */ BlockPos( 0, 3,  0) -> (Assets.BlockPillar,  0)
	)
}
