package targetsan.mcmods.gateway.api;

import cpw.mods.fml.common.eventhandler.Cancelable;
import net.minecraft.entity.Entity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityEvent;

/** Describes general set of parameters which characterize entity travel through gateway
 *  Chunk coordinates represent source and destination gateway cores.
 *  All parameters should be treated as immutable
 */
public abstract class GatewayTravelEvent extends EntityEvent{
    public final ChunkCoordinates fromBlock, toBlock;
    public final World fromWorld, toWorld;

    protected GatewayTravelEvent(
            Entity entity,
            ChunkCoordinates fromBlock,
            World fromWorld,
            ChunkCoordinates toBlock,
            World toWorld)
    {
        super(entity);
        this.fromBlock = fromBlock;
        this.fromWorld = fromWorld;
        this.toBlock = toBlock;
        this.toWorld = toWorld;
    }

    /** Fired before entity enters gateway.
     *  Allows to block teleportation
     *  or change destination coordinates, where entity will appear
     *  This event is fired only once for rider-mount pair.
     *  In this case, referenced entity is a mount
     */
    @Cancelable
    public static class Enter extends GatewayTravelEvent {
        public Vec3 destPos;
        public World destWorld;

        public Enter(
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

    public static class Leave extends GatewayTravelEvent {
        public Leave(Entity entity, ChunkCoordinates fromBlock, World fromWorld, ChunkCoordinates toBlock, World toWorld) {
            super(entity, fromBlock, fromWorld, toBlock, toWorld);
        }
    }
}
