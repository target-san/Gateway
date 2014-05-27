package org.targetsan.mcmods.gateway

import cpw.mods.fml.common.event._
import cpw.mods.fml.common.Mod
import java.io.File
import cpw.mods.fml.common.registry.GameRegistry
import cpw.mods.fml.common.registry.LanguageRegistry
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.block.Block
import net.minecraft.item.Item
import cpw.mods.fml.relauncher.IFMLLoadingPlugin
import cpw.mods.fml.relauncher.IFMLLoadingPlugin._

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

@MCVersion("1.7.2")
class GatewayCoreMod extends IFMLLoadingPlugin
{
	override def getASMTransformerClass: Array[String] = Array.empty[String]
	override def getModContainerClass: String = null
	override def getSetupClass: String = null
	override def injectData(data: java.util.Map[String, Object]) { }
	override def getAccessTransformerClass: String = null
}