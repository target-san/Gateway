package targetsan.mcmods.gateway

import scala.util._
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraft.world.World
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.ChatComponentText
import net.minecraft.init.Blocks
import net.minecraft.util.ChatStyle
import net.minecraft.util.EnumChatFormatting

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
		getNetherExit(w, x, y, z) match
		{
			case Success((ex, ey, ez)) =>
				placeGatewayPair(player, w, x, y, z, Gateway.dimension, ex, ey, ez)
				player
					.addChatMessage(
						new ChatComponentText(
							s"Gateway successfully constructed from ${w.provider.getDimensionName} to ${Gateway.dimension.provider.getDimensionName}"
						)
						.setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GREEN))
					)
			case Failure(error) =>
				player
					.addChatMessage(
						new ChatComponentText(error.getMessage)
						.setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED))
					)
		}
	}
	
	private def placeGatewayPair(owner: EntityPlayer, from: World, x0: Int, y0: Int, z0: Int, to: World, x1: Int, y1: Int, z1: Int) =
	{
		val core1 = GatewayMod.BlockGatewayBase.placeCore(from, x0, y0, z0)
		val core2 = GatewayMod.BlockGatewayBase.placeCore(to,   x1, y1, z1)
		
		core1.init(core2, owner)
		core2.init(core1, owner)
	}
	
	private def isMultiblockPresent(w: World, x: Int, y: Int, z: Int) =
		// center
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
	
	private def getNetherExit(from: World, x: Int, y: Int, z: Int): Try[(Int, Int, Int)] =
	{
		val to = Gateway.dimension
		// Forbid constructing gateways from nether
		if (from.provider.dimensionId == Gateway.DIMENSION_ID)
			return Failure(new Exception("Gateways cannot be constructed from Nether"))
		// Compute destination coordinates; TODO: implement volume lookup for optimal location 
		def mapCoord(c: Int) = Math.round(c * from.provider.getMovementFactor() / to.provider.getMovementFactor()).toInt
		
		val ex = mapCoord(x)
		val ey = (to.provider.getActualHeight - 1) / 2
		val ez = mapCoord(z)
		// Check if destination point is free
		if (!isDestinationFree(to, ex, ey, ez))
			return Failure(new Exception("Gateway cannot be constructed here - there's another gateway too near on the other side"))

		Success((ex, ey, ez))
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
