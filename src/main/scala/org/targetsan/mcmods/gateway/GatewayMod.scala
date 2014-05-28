package org.targetsan.mcmods.gateway

import cpw.mods.fml.common.Mod
import cpw.mods.fml.common.event.FMLInitializationEvent
import cpw.mods.fml.common.event.FMLPostInitializationEvent
import cpw.mods.fml.relauncher.IFMLLoadingPlugin
import cpw.mods.fml.relauncher.IFMLLoadingPlugin.MCVersion
import net.minecraft.launchwrapper.IClassTransformer
import org.objectweb.asm._
import cpw.mods.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper
import org.objectweb.asm.commons.RemappingClassAdapter

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
	override def getSetupClass: String = null //getClass.getName
	override def injectData(data: java.util.Map[String, Object]) { }
	override def getAccessTransformerClass: String = null
}

class FlintAndSteelPatcher extends IClassTransformer
{
	override def transform(name: String, transformedName: String, bytes: Array[Byte]): Array[Byte] =
		if (transformedName == "net.minecraft.item.ItemFlintAndSteel") applyPatch(bytes)
		else bytes
	
	private def applyPatch(bytes: Array[Byte]): Array[Byte] =
	{
		val reader = new ClassReader(bytes)
		val writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS)
		val patcher = new ClassPatcher(writer)
		val remapper = new RemappingClassAdapter(patcher, FMLDeobfuscatingRemapper.INSTANCE)
		
		reader.accept(remapper, ClassReader.EXPAND_FRAMES)
		writer.toByteArray()
	}
	
	private class ClassPatcher(cv: ClassVisitor) extends ClassVisitor(Opcodes.ASM4, cv)
	{
		override def visitMethod(access: Int, name: String, desc: String, signature: String, exceptions: Array[String]): MethodVisitor =
		{
			System.out.println(s"Found method $name $desc") // our target method is SRG func_77648_a == onItemUse
			
			super.visitMethod(access, name, desc, signature, exceptions)
		}
	}
}

