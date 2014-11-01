package targetsan.mcmods.gateway

import cpw.mods.fml.common.eventhandler.SubscribeEvent
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.stats.AchievementList
import targetsan.mcmods.gateway.Utils._

// Triggers some vanilla travel events
object AchievementTracker {
	@SubscribeEvent
	def onPlayerTravel(event: api.GatewayTravelEvent.Leave): Unit =
		for (player <- event.entity.as[EntityPlayer])
			(event.fromWorld.provider.dimensionId, event.toWorld.provider.dimensionId) match {
				case (_, NetherDimensionId) => player.triggerAchievement(AchievementList.portal)
				case (_, EndDimensionId) => player.triggerAchievement(AchievementList.theEnd)
				case (EndDimensionId, _) => player.triggerAchievement(AchievementList.theEnd2)
				case _ => ()
			}
}
