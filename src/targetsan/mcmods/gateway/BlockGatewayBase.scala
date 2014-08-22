package targetsan.mcmods.gateway
import cpw.mods.fml.common.Mod
import net.minecraft.block.Block
import net.minecraft.block.BlockContainer
import net.minecraft.block.material.Material
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.entity.Entity
import net.minecraft.init.{Blocks, Items}
import net.minecraft.world.World
import net.minecraft.util.IIcon
import net.minecraft.entity.player.EntityPlayer

class BlockGatewayBase extends BlockContainer(Material.rock)
	with DropsNothing
	with Unbreakable
	with TeleportActor
	with MetaBlock[SubBlock]
{
	disableStats()
	setBlockName("GatewayBase")
	setStepSound(Block.soundTypePiston)
	
	val Core  = 0 // Default core block
	val SatNW = 1
	val SatN  = 2
	val SatNE = 3
	val SatE  = 4
	val SatSE = 5
	val SatS  = 6
	val SatSW = 7
	val SatW  = 8
	
	registerSubBlocks(
		Core -> new SubBlockCore(new SimpleEndpoint)
		, SatNW -> new SubBlockSatellite( -1, -1, "minecraft:obsidian")
		, SatN  -> new SubBlockSatellite(  0, -1, "minecraft:obsidian", 0)
		, SatNE -> new SubBlockSatellite(  1, -1, "minecraft:obsidian")
		, SatE  -> new SubBlockSatellite(  1,  0, "minecraft:obsidian", 1)
		, SatSE -> new SubBlockSatellite(  1,  1, "minecraft:obsidian")
		, SatS  -> new SubBlockSatellite(  0,  1, "minecraft:obsidian", 2)
		, SatSW -> new SubBlockSatellite( -1,  1, "minecraft:obsidian")
		, SatW  -> new SubBlockSatellite( -1,  0, "minecraft:obsidian", 3)
	)
	
	private def subsOfType[T <: SubBlock](implicit m: Manifest[T]) =
		allSubBlocks withFilter { i => m.erasure.isInstance(i._2) } map { i => (i._1, i._2.asInstanceOf[T]) }
	
	def cores      = subsOfType[SubBlockCore]
	def satellites = subsOfType[SubBlockSatellite]
	
	def placeCore(world: World, x: Int, y: Int, z: Int): TileGateway =
	{
		world.setBlock(x, y, z, this, Core, 3)
		world.getTileEntity(x, y, z).asInstanceOf[TileGateway]
	}
	
	override def hasTileEntity(meta: Int) =
		subBlock(meta).hasTileEntity(meta)

	override def createNewTileEntity(world: World, meta: Int) =
		subBlock(meta).createNewTileEntity(world, meta)

	override def onBlockActivated(world: World, x: Int, y: Int, z: Int, player: EntityPlayer, side: Int, xTouch: Float, yTouch: Float, zTouch: Float) =
		subBlock(world, x, y, z).onBlockActivated(world, x, y, z, player, side, xTouch, yTouch, zTouch)
	
	override def onNeighborBlockChange(world: World, x: Int, y: Int, z: Int, block: Block) = 
		subBlock(world, x, y, z).onNeighborBlockChange(world, x, y, z, block)

	override def randomDisplayTick(world: World, x: Int, y: Int, z: Int, random: java.util.Random) =
		subBlock(world, x, y, z).randomDisplayTick(world, x, y, z, random)
	
	override def teleportEntity(world: World, x: Int, y: Int, z: Int, entity: Entity) =
		subBlock(world, x, y, z).teleportEntity(world, x, y, z, entity)
	
	override def getIcon(side: Int, meta: Int) =
		subBlock(meta).getIcon(side, meta)

	override def registerBlockIcons(register: IIconRegister) =
		allSubBlocks foreach { _._2.registerBlockIcons(register) }
}

trait Endpoint
{
	val PortalPillarHeight = 3
	// This one is used only when endpoint is constructed by player, i.e. it checks presense of valid multiblock
	def canAssembleHere(world: World, x: Int, y: Int, z: Int): Boolean
	def assemble(world: World, x: Int, y: Int, z: Int): Unit
	def disassemble(world: World, x: Int, y: Int, z: Int): Unit
}

class SubBlockCore(val multiblock: Endpoint) extends SubBlock
{
	protected var blockTopIcon: IIcon = null
	
	override def hasTileEntity(meta: Int) = true
	override def createNewTileEntity(world: World, meta: Int) = new TileGateway

	override def randomDisplayTick(world: World, x: Int, y: Int, z: Int, random: java.util.Random)
	{
		for (i <- 0 until 4)
			world.spawnParticle("portal", x + random.nextDouble(), y + 1.0, z + random.nextDouble(), 0.0, 1.5, 0.0)
	}
	
	override def registerBlockIcons(icons: IIconRegister)
	{
		blockIcon = icons.registerIcon("minecraft:obsidian")
		blockTopIcon = icons.registerIcon("minecraft:portal")
	}
	
	override def getIcon(side: Int, meta: Int): IIcon =
		if (side == 1) blockTopIcon
		else           blockIcon
	
	override def teleportEntity(world: World, x: Int, y: Int, z: Int, entity: Entity)
	{
		world.getTileEntity(x, y, z).asInstanceOf[TileGateway].teleportEntity(entity)
	}
}

class SimpleEndpoint extends Endpoint
{
	override def canAssembleHere(w: World, x: Int, y: Int, z: Int) =
		w.getBlock(x, y, z) == Blocks.redstone_block &&
		// corners
		w.getBlock(x - 1, y, z - 1) == Blocks.obsidian &&
		w.getBlock(x + 1, y, z - 1) == Blocks.obsidian &&
		w.getBlock(x - 1, y, z + 1) == Blocks.obsidian &&
		w.getBlock(x + 1, y, z + 1) == Blocks.obsidian &&
		// sides
		w.getBlock(x - 1, y, z) == Blocks.glass &&
		w.getBlock(x + 1, y, z) == Blocks.glass &&
		w.getBlock(x, y, z - 1) == Blocks.glass &&
		w.getBlock(x, y, z + 1) == Blocks.glass

	override def assemble(world: World, x: Int, y: Int, z: Int) =
	{
		// Core
		world.setBlock(x, y, z, GatewayMod.BlockGatewayBase , GatewayMod.BlockGatewayBase.Core, 3)
		// Satellite platform blocks
		for ((i, sat) <- GatewayMod.BlockGatewayBase.satellites)
			world.setBlock(x + sat.xOffset, y, z + sat.zOffset, GatewayMod.BlockGatewayBase, i, 3)
		// Portal column
		for (y1 <- y+1 to y+PortalPillarHeight )
			GatewayMod.BlockGatewayAir.placePortal(world, x, y1, z)
	}

	override def disassemble(world: World, x: Int, y: Int, z: Int) =
	{
		// dispose everything above platform
		Utils.enumVolume(world, x - 1, y + 1, z - 1, x + 1, y + PortalPillarHeight, z + 1)
			.foreach
			{ case (x, y, z) =>
				if (world.getBlock(x, y, z) == GatewayMod.BlockGatewayAir)
					world.setBlockToAir(x, y, z)
			}

		// dispose platform except core; disposing core here would cause infinite loop
		for ((_, sat) <- GatewayMod.BlockGatewayBase.satellites)
		{
			val deadBlock =
				if (world.provider.dimensionId == Gateway.DIMENSION_ID) Blocks.stone    // stone for Nether
				else if (sat.isDiagonal)                                Blocks.obsidian // Obsidian for platform corners
				else                                                    Blocks.glass    // Glass for platform sides
			world.setBlock(x + sat.xOffset, y, z + sat.zOffset, deadBlock)
		}
	}
}

class SubBlockSatellite(val xOffset: Int, val zOffset: Int, textureName: String, private val side: Int = -1) extends SubBlock
{
	val isDiagonal = xOffset != 0 && zOffset != 0
	setBlockTextureName(textureName)
	
	override def onBlockActivated(world: World, x: Int, y: Int, z: Int, player: EntityPlayer, side: Int, xTouch: Float, yTouch: Float, zTouch: Float): Boolean =
	{
		if (world.isRemote ||
			isDiagonal ||
			side != 1 ||
			player.getHeldItem == null ||
			player.getHeldItem.getItem != Items.flint_and_steel
		)
			return false
		
		val tile = world
			.getTileEntity(x - xOffset, y, z - zOffset)
			.asInstanceOf[TileGateway]
		if (tile != null)
			tile.markForDispose(player, this.side)
		
		false
	}
	
	override def onNeighborBlockChange(world: World, x: Int, y: Int, z: Int, block: Block)
	{
		if (world.isRemote ||
			isDiagonal ||
			world.getBlock(x, y + 1, z) == Blocks.fire
		)
			return
		
		val tile = world
			.getTileEntity(x - xOffset, y, z - zOffset)
			.asInstanceOf[TileGateway]
		if (tile != null)
			tile.unmarkForDispose(this.side)
	}
}
