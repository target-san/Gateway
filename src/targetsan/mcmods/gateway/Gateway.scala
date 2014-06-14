package targetsan.mcmods.gateway

import cpw.mods.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraft.world.World
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.ChatComponentText
import net.minecraft.init.Blocks

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
		GatewayMod.BlockGatewayBase
			.placeCore(w, x, y, z)
			.init(ex, ey, ez, player)
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
