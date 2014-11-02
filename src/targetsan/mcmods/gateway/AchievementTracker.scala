package targetsan.mcmods.gateway

import cpw.mods.fml.common.eventhandler.SubscribeEvent
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.stats.AchievementList
import targetsan.mcmods.gateway.Utils._

// Triggers some vanilla travel events
object AchievementTracker {
	@SubscribeEvent
	def onPlayerTravel(event: api.GatewayTravelEvent.Leave): Unit =
		for (rider <- enumRiders(event.entity); player <- rider.as[EntityPlayer]) {
			val fromId = event.fromWorld.provider.dimensionId
			val toId = event.toWorld.provider.dimensionId
			val trigger = player.triggerAchievement _

			if (toId == NetherDimensionId) trigger(AchievementList.portal)
			if (toId == EndDimensionId) trigger(AchievementList.theEnd)
			if (fromId == EndDimensionId) trigger(AchievementList.theEnd2)
		}
}
