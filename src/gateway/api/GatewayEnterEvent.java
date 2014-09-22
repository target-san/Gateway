package gateway.api;

import cpw.mods.fml.common.eventhandler.Cancelable;
import net.minecraft.entity.Entity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

/** Fired before entity enters gateway.
 *  Allows to block teleportation
 *  or change destination coordinates, where entity will appear
 *  This event is fired only once for rider-mount pair.
 *  In this case, referenced entity is a mount
 */
@Cancelable
public class GatewayEnterEvent extends GatewayTravelEvent {
    public Vec3 destPos;
    public World destWorld;

    public GatewayEnterEvent(
        Entity entity,
        ChunkCoordinates fromBlock,
        World fromWorld,
        ChunkCoordinates toBlock,
        World toWorld,
        Vec3 destPos)
    {
        super(entity, fromBlock, fromWorld, toBlock, toWorld);
        this.destPos = destPos;
        this.destWorld = toWorld;
    }
}
