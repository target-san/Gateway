package gateway.api

import cpw.mods.fml.common.eventhandler.Cancelable
import net.minecraft.entity.Entity
import net.minecraft.util.ChunkCoordinates
import net.minecraftforge.event.entity.EntityEvent

/** Fired when any entity travels through any gateway.
 *  Provides entity itself, source gateway TE, destination gateway TE and destination position
 *
 */
@Cancelable
class GatewayTravelEvent(entity: Entity, val fromPos: ChunkCoordinates) extends EntityEvent(entity) {

}
