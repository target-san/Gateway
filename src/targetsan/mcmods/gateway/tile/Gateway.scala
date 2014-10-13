package targetsan.mcmods.gateway.tile

import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.util.ForgeDirection

abstract class Gateway extends TileEntity
{
	//******************************************************************************************************************
	// Operations for non-center gateway platform tiles
	//******************************************************************************************************************
	def onActivated(player: EntityPlayer, side: ForgeDirection) {}
	def onNeighborBlockChanged() {}
	def onNeighborTileChanged(tx: Int, ty: Int, tz: Int) {}
	//******************************************************************************************************************
	// Teleporter, for core tile mostly
	//******************************************************************************************************************
	def teleport(entity: Entity) { }
}
