package targetsan.mcmods.gateway

import net.minecraft.block.{Block, BlockObsidian}
import net.minecraft.block.material.Material
import net.minecraft.entity.Entity
import net.minecraft.world.World
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.world.IBlockAccess
import net.minecraft.server.MinecraftServer
import net.minecraft.entity.EntityList
import net.minecraft.entity.player.EntityPlayer

class BlockGateway(blockId: Int) extends BlockObsidian(blockId) {
	setHardness(50.0F)
    setResistance(2000.0F)
    setStepSound(Block.soundStoneFootstep)
    setUnlocalizedName("gateway")
    setTextureName("gateway:gateway")
    setCreativeTab(CreativeTabs.tabRedstone)
    
	override def onBlockActivated(world: World, x: Int, y: Int, z: Int, player: EntityPlayer, w: Int, touchX: Float, touchY: Float, touchZ: Float): Boolean = {
	    if (world.isRemote)
	        return true
	    
	    val dim = if (world.provider.dimensionId == DimensionManager.NETHER_ID) DimensionManager.OVERWORLD_ID else DimensionManager.NETHER_ID
	    val remoteWorld = MinecraftServer.getServer.worldServerForDimension(dim)
	    val factor = world.provider.getMovementFactor() / remoteWorld.provider.getMovementFactor()
	    val destX = Math.round(x * factor).toInt
	    val destZ = Math.round(z * factor).toInt
	    
	    for ((x1, y1, z1) <- findBlock(remoteWorld, destX, y, destZ, 8, (wp, xp, yp, zp) => { wp.getBlockId(xp, yp, zp) == blockId } ))
	    {
	        System.out.println(s"Found gateway at $x1, $y1, $z1, $dim")
	        Teleporter.teleport(player, x1.toDouble + 0.5, y1.toDouble + 1.0, z1.toDouble + 0.5, dim)
	    }
	    true
	}
	
	private def findBlock(world: IBlockAccess, x: Int, y: Int, z: Int, eps: Int, pred: (IBlockAccess, Int, Int, Int) => Boolean): Option[(Int, Int, Int)] = {
	    for {
	        x1 <- x - eps to x + eps
	        y1 <- y - eps to y + eps
	        z1 <- z - eps to z + eps
	    }
	    if (pred(world, x1, y1, z1))
	        return Some((x1, y1, z1))
    	None
	}
}
