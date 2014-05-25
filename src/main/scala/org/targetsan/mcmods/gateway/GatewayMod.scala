package targetsan.mcmods.gateway

import cpw.mods.fml.common.event._
import cpw.mods.fml.common.Mod
import java.io.File
import cpw.mods.fml.common.registry.GameRegistry
import cpw.mods.fml.common.registry.LanguageRegistry
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.block.Block
import net.minecraft.item.Item

@Mod(modid = "gateway", useMetadata = true, modLanguage = "scala")
object GatewayMod {
	val MODID = "gateway"
    
    @Mod.EventHandler
	def init(event: FMLInitializationEvent) {
    }
    @Mod.EventHandler
	def postInit(event: FMLPostInitializationEvent) {
    }
}
