package targetsan.mcmods.gateway

import net.minecraft.block.Block
import net.minecraft.util._
import net.minecraft.world.World
import net.minecraft.entity.Entity
import net.minecraft.server.MinecraftServer
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.EnumChatFormatting
import net.minecraft.util.ChunkCoordinates
import net.minecraft.item.ItemStack
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import cpw.mods.fml.common.registry.GameRegistry
import net.minecraft.init.{Blocks, Items}

object Utils
{
	private val GatewayDeadZoneRadius = 7
	
	def world(dim: Int) = MinecraftServer.getServer().worldServerForDimension(dim)
	
	def mapToWorld(x: Int, z: Int, from: World, to: World) =
	{
		def mapCoord(c: Int) = Math.round(c * from.provider.getMovementFactor() / to.provider.getMovementFactor()).toInt 
		(mapCoord(x), mapCoord(z))
	}
	
	def tryPlaceGateway(w: World, x: Int, y: Int, z: Int, player: EntityPlayer)
	{
		// Check if there's multiblock present
		if (// corners
			w.getBlock(x - 1, y, z - 1) != Blocks.obsidian
		 || w.getBlock(x + 1, y, z - 1) != Blocks.obsidian
		 || w.getBlock(x - 1, y, z + 1) != Blocks.obsidian
		 || w.getBlock(x + 1, y, z + 1) != Blocks.obsidian
		 // sides
		 || w.getBlock(x - 1, y, z) != Blocks.glass
		 || w.getBlock(x + 1, y, z) != Blocks.glass
		 || w.getBlock(x, y, z - 1) != Blocks.glass
		 || w.getBlock(x, y, z + 1) != Blocks.glass
		 // center
		 || w.getBlock(x, y, z) != Blocks.redstone_block
		)
			return
		 
		if (w.provider.dimensionId == Gateway.DIMENSION_ID)
		{
			player.addChatMessage(new ChatComponentText("Gateways cannot be constructed from Nether"))
			return
		}
		// Check dead zone on the other side
		if (!isDestinationFree(w, x, y, z))
		{
			player.addChatMessage(new ChatComponentText("Gateway cannot be constructed here - there's another gateway too near on the other side"))
			return
		}
		// Construct gateways on both sides
		w.setBlock(x, y, z, GatewayMod.BlockGatewayBase)
		val (ex, ey, ez) = getExit(w, x, y, z)
		w.getTileEntity(x, y, z).asInstanceOf[TileGateway].init(ex, ey, ez, player)
		player.addChatMessage(new ChatComponentText(s"Gateway successfully constructed from ${w.provider.getDimensionName} to ${Gateway.dimension.provider.getDimensionName}"))
	}
    // Checks if there are no active gateways in the nether too near
	private def isDestinationFree(from: World, x: Int, y: Int, z: Int): Boolean =
	{
		val nether = Gateway.dimension
		// Gateway exits in nether should have at least 7 blocks square between them
		val (exitX, exitZ) = mapToWorld(x, z, from, nether)
		enumVolume(nether,
				exitX - GatewayDeadZoneRadius, 0, exitZ - GatewayDeadZoneRadius,
				exitX + GatewayDeadZoneRadius, nether.provider.getActualHeight - 1, exitZ + GatewayDeadZoneRadius
			)
			.forall { case (x, y, z) => nether.getBlock(x, y, z) != GatewayMod.BlockGatewayBase }
	}
	
	private def getExit(w: World, x: Int, y: Int, z: Int): (Int, Int, Int) =
	{
		val nether = Gateway.dimension
		val (ex, ez) = mapToWorld(x, z, w, nether)
		val ey = (nether.provider.getActualHeight - 1) / 2
		
		(ex, ey, ez)
	}

	def enumVolume(world: World, x1: Int, y1: Int, z1: Int, x2: Int, y2: Int, z2: Int) =
        for (x <- x1 to x2; y <- y1 to y2; z <- z1 to z2) yield (x, y, z)
}
