package targetsan.mcmods.gateway.tile.linkers

import cofh.api.transport.IItemDuct
import cpw.mods.fml.common.Optional.{Method, Interface}
import net.minecraft.item.ItemStack
import net.minecraftforge.common.util.ForgeDirection
import targetsan.mcmods.gateway._

@Interface( iface = "cofh.api.transport.IItemDuct", modid = "CoFHCore", striprefs = true)
trait ItemDuctLinker extends tile.Linker with IItemDuct {
	@Method(modid = "CoFHCore")
	override def insertItem(from: ForgeDirection, item: ItemStack): ItemStack =
		linkedTileAs[IItemDuct](from) map { _.insertItem(from, item) } getOrElse item
}
