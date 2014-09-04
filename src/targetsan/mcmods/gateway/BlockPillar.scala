package targetsan.mcmods.gateway
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.entity.Entity
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraft.util.IIcon
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.util.AxisAlignedBB

class BlockPillar extends Block(Material.portal)
	with DropsNothing
	with NotACube
	with Unbreakable
	with NotCollidable
	with NotActivable
	with TeleportActor
{
	disableStats()
	setBlockName("GatewayAir")
	setBlockTextureName("gateway:pillar-side")
	
	protected val topIconName = "gateway:pillar-top"
	protected var topIcon: IIcon = null
	
	override def onEntityCollidedWithBlock(world: World, x: Int, y: Int, z: Int, entity: Entity) =
	{
		teleportEntity(world, x, y, z, entity)
	}
	
	override def teleportEntity(world: World, x: Int, y: Int, z: Int, entity: Entity) =
	{
		val below = world.getBlock(x, y - 1, z)
		if (below.isInstanceOf[TeleportActor])
			below.asInstanceOf[TeleportActor].teleportEntity(world, x, y - 1, z, entity)
	}
	
	override def getRenderBlockPass = 1
	
	override def shouldSideBeRendered(world: IBlockAccess, x: Int, y: Int, z: Int, side: Int): Boolean =
		side match
		{
		case 0 => false
		case 1 => world.getBlock(x, y, z) != GatewayMod.BlockPillar
		case _ => super.shouldSideBeRendered(world, x, y, z, side)
		}
	
	override def getIcon(side: Int, meta: Int): IIcon =
		if (side == 0 || side == 1) topIcon
		else super.getIcon(side, meta)
	
	override def registerBlockIcons(register: IIconRegister)
	{
		super.registerBlockIcons(register)
		topIcon = register.registerIcon(topIconName)
	}
}
