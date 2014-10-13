package targetsan.mcmods.gateway.block

import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.entity.Entity
import net.minecraft.world.{IBlockAccess, World}

import targetsan.mcmods.gateway.Utils._

class Pillar extends Block(Material.portal)
	with TeleportActor
	with NotCollidable
	with NotActivable
	with DropsNothing
	with Unbreakable
	with NotACube
	with NoCreativePick
{
	disableStats()
	setBlockName("gateway:pillar")
	setBlockTextureName("gateway:pillar")
	setLightLevel(1.0F)

	//******************************************************************************************************************
	// TeleportActor
	//******************************************************************************************************************
	override def onEntityCollidedWithBlock(world: World, x: Int, y: Int, z: Int, entity: Entity) =
		teleportEntity(world, x, y, z, entity)

	override def teleportEntity(world: World, x: Int, y: Int, z: Int, entity: Entity): Unit =
		world
			.getBlock(x, y - 1, z)
			.as[TeleportActor]
			.foreach { _.teleportEntity(world, x, y - 1, z, entity) }

	//******************************************************************************************************************
	// Rendering
	//******************************************************************************************************************
	override def shouldSideBeRendered(world: IBlockAccess, x: Int, y: Int, z: Int, side: Int): Boolean =
		side match {
			case 0 => false
			case 1 => world.getBlock(x, y, z) != this
			case _ => super.shouldSideBeRendered(world, x, y, z, side)
		}
}
