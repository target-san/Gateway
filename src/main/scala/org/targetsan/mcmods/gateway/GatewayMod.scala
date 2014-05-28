package org.targetsan.mcmods.gateway

import cpw.mods.fml.common.Mod
import cpw.mods.fml.common.event.FMLInitializationEvent
import cpw.mods.fml.common.event.FMLPostInitializationEvent
import cpw.mods.fml.relauncher.IFMLLoadingPlugin
import cpw.mods.fml.relauncher.IFMLLoadingPlugin.MCVersion
import net.minecraft.launchwrapper.IClassTransformer

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
	override def getASMTransformerClass: Array[String] = Array("org.targetsan.mcmods.gateway.FlintAndSteelPatcher")
	override def getModContainerClass: String = null
	override def getSetupClass: String = null
	override def injectData(data: java.util.Map[String, Object]) { }
	override def getAccessTransformerClass: String = null
}

class FlintAndSteelPatcher extends IClassTransformer
{
	override def transform(name: String, transformedName: String, bytes: Array[Byte]): Array[Byte] =
		if (transformedName == "net.minecraft.item.ItemFlintAndSteel") applyPatch(bytes)
		else bytes
	
	private def applyPatch(bytes: Array[Byte]): Array[Byte] = bytes
}
