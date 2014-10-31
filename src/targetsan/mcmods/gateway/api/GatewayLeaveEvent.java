package targetsan.mcmods.gateway.api;

import net.minecraft.entity.Entity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;

/** Notifies that entity has just came out of gateway
 */
public class GatewayLeaveEvent extends GatewayTravelEvent {
    public GatewayLeaveEvent(Entity entity, ChunkCoordinates fromBlock, World fromWorld, ChunkCoordinates toBlock, World toWorld) {
        super(entity, fromBlock, fromWorld, toBlock, toWorld);
    }
}
