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
		if (!event.entityPlayer.worldObj.isRemote) // Works only server-side
		// We're interested in Flint'n'Steel clicking some block only
		if (event.entityPlayer != null)
		if (event.entityPlayer.getHeldItem != null)
		if (event.entityPlayer.getHeldItem.getItem == net.minecraft.init.Items.flint_and_steel)
		if (event.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK)
			GatewayMod
			.BlockGateway
			.cores
			.foldLeft[Try[Boolean]](Success(false))
			{	case (Success(false), (_, core)) =>
					core.multiblock.assemble(event.entityPlayer.worldObj, event.x, event.y, event.z, event.entityPlayer)
				case (x, _) => x
			} match {
				case Failure(error) =>
					event.entityPlayer.addChatMessage(
						new ChatComponentText(error.getMessage)
						.setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED))
					)
				case _ => ()
			}
}
