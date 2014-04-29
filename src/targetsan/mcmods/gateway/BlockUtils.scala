package targetsan.mcmods.gateway

import java.util.List

import net.minecraft.block.Block
import net.minecraft.util.AxisAlignedBB
import net.minecraft.world.World
import net.minecraft.entity.Entity

object Blocks {
    var gateway: BlockGateway = null
    var portal: BlockPortal = null
}
// Block can't be moved by pistons
trait Immobile extends Block {
    override def getMobilityFlag = 2 // Block can't be moved by piston
}
// Block can't be interacted and not an obstacle
trait Intangible extends Block {
    override def getSelectedBoundingBoxFromPool(world: World, x: Int, y: Int, z: Int): AxisAlignedBB = null
    override def getCollisionBoundingBoxFromPool(world: World, x: Int, y: Int, z: Int): AxisAlignedBB = null
    override def addCollisionBoxesToList(world: World, x: Int, y: Int, z: Int, mask: AxisAlignedBB, list: java.util.List[_], entity: Entity) { }
}
// Adds entity teleportation logic
trait GatewayTile {
    def teleportEntity(world: World, x: Int, y: Int, z: Int, entity: Entity): Unit
}
