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
import cpw.mods.fml.common.registry.GameRegistry
import java.util.Random
import net.minecraft.client.renderer.texture.IconRegister
import net.minecraft.util.Icon
import net.minecraft.block.BlockContainer

class BlockGateway(id: Int) extends BlockContainer(id, Material.rock)
	with Immobile
	with GatewayTile
{
    disableStats()
	setBlockUnbreakable()
    setResistance(6000000.0F)
    setStepSound(Block.soundStoneFootstep)
    setUnlocalizedName("gateway")
    
    private var activeSideIcon: Icon = null
    
	override def onBlockAdded(world: World, x: Int, y: Int, z: Int)
    {
        tile(world, x, y, z).onCreate
	}
    
    override def registerIcons(icons: IconRegister)
    {
        blockIcon = icons.registerIcon("gateway:gateway")
        activeSideIcon = icons.registerIcon("gateway:gateway_a")
    }
    
    override def getIcon(side: Int, meta: Int) =
        if (side == 1) activeSideIcon
        else blockIcon
	
	override def randomDisplayTick(world: World, x: Int, y: Int, z: Int, random: Random)
    {
	    for (i <- 0 until 4)
	        world.spawnParticle("portal", x + random.nextDouble(), y + 1.0, z + random.nextDouble(), 0.0, 1.5, 0.0)
	}
    
    override def createNewTileEntity(world: World) = new TileGateway
    // This block doesn't drop in any way
    override def idPicked(w: World, x: Int, y: Int, z: Int) = 0
    override def idDropped(p1: Int, random: Random, p2: Int) = 0
    
	// Teleports specified entity to other gateway
	override def teleportEntity(world: World, x: Int, y: Int, z: Int, entity: Entity)
    {
    	tile(world, x, y, z).teleportEntity(entity)
	}
	
	protected def tile(world: World, x: Int, y: Int, z: Int) =
	    world.getBlockTileEntity(x, y, z).asInstanceOf[TileGateway]
}