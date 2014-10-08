package targetsan.mcmods.gateway.linkers.cofh

import cofh.api.transport.IItemDuct
import cpw.mods.fml.common.Optional._
import net.minecraft.item.ItemStack
import net.minecraftforge.common.util.ForgeDirection
import targetsan.mcmods.gateway.TileLinker

@Interface(iface = "cofh.api.transport.IItemDuct", modid = "CoFHCore", striprefs = true)
trait ItemDuctLinker extends TileLinker with IItemDuct{
	@Method(modid = "CoFHCore")
	override def insertItem(from: ForgeDirection, item: ItemStack): ItemStack =
		tileAs[IItemDuct](from) map { _.insertItem(from, item) } getOrElse null
}
