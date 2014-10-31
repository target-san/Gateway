package targetsan.mcmods.gateway.api;

import net.minecraft.entity.Entity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityEvent;

/** Describes general set of parameters which characterize entity travel through gateway
 *  Chunk coordinates represent source and destination gateway cores.
 *  All parameters should be treated as immutable
 */
public abstract class GatewayTravelEvent extends EntityEvent{
    public final ChunkCoordinates fromBlock, toBlock;
    public final World fromWorld, toWorld;

    public GatewayTravelEvent(
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
}
