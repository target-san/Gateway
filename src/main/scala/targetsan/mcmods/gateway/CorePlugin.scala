package targetsan.mcmods.gateway

import cpw.mods.fml.relauncher.IFMLLoadingPlugin
import cpw.mods.fml.relauncher.IFMLLoadingPlugin.MCVersion
import net.minecraft.launchwrapper.IClassTransformer
import org.objectweb.asm._
import cpw.mods.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper
import org.objectweb.asm.commons.RemappingClassAdapter

object GatewayCoreMod
{
	var isDebug: Boolean = false
}

@MCVersion("1.7.2")
class GatewayCoreMod extends IFMLLoadingPlugin
{
	override def getASMTransformerClass: Array[String] = Array("targetsan.mcmods.gateway.FlintAndSteelPatcher")
	override def getModContainerClass: String = null
	override def getSetupClass: String = null
	override def getAccessTransformerClass: String = null

	override def injectData(data: java.util.Map[String, Object])
	{
		// For now we assume this flag is always present
		GatewayCoreMod.isDebug = !data.get("runtimeDeobfuscationEnabled").asInstanceOf[Boolean]
	}
}

class FlintAndSteelPatcher extends IClassTransformer
{
	private val patcher = new ItemFlintAndSteelPatcher(null)
	
	override def transform(name: String, transformedName: String, bytes: Array[Byte]): Array[Byte] =
		if (patcher.canApply(name, transformedName, bytes)) applyPatch(bytes)
		else bytes
	
	private def applyPatch(bytes: Array[Byte]): Array[Byte] =
	{
		val reader = new ClassReader(bytes)
		val writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS)
		val patcher = new ItemFlintAndSteelPatcher(writer)
		
		reader.accept(patcher, ClassReader.EXPAND_FRAMES)
		writer.toByteArray()
	}
	
	private class ItemFlintAndSteelPatcher(cv: ClassVisitor) extends ClassVisitor(Opcodes.ASM4, cv)
	{
		private val patcher = new OnItemUsePatcher(null)
		
		val CLASS_NAME = "net.minecraft.item.ItemFlintAndSteel"
		def canApply(name: String, transformedName: String, bytes: Array[Byte]) = transformedName == CLASS_NAME
		
		override def visitMethod(access: Int, name: String, desc: String, signature: String, exceptions: Array[String]): MethodVisitor =
		{
			val methodVisitor = super.visitMethod(access, name, desc, signature, exceptions)
			if (patcher.canApply(access, name, desc, signature, exceptions))
				new OnItemUsePatcher(methodVisitor)
			else
				methodVisitor
		}

		private class OnItemUsePatcher(mv: MethodVisitor) extends MethodVisitor(Opcodes.ASM4, mv)
		{
			private val CLASS = "net.minecraft.item.ItemFlintAndSteel"
			private val NAME = if (GatewayCoreMod.isDebug) "onItemUse" else "func_77648_a"
			private val DESC = "(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/world/World;IIIIFFF)Z"
				
			private val remapper = FMLDeobfuscatingRemapper.INSTANCE
			
			def canApply(access: Int, name: String, desc: String, signature: String, exceptions: Array[String]) =
				remapper.mapMethodName(CLASS, name, desc) == NAME && remapper.mapDesc(desc) == DESC
		
			override def visitCode
			{
				import Opcodes._
				super.visitCode
				if (mv == null) return
				
				val start = new Label()
				mv.visitLabel(start)
				// load args onto stack
				mv.visitVarInsn(ALOAD, 1)
				mv.visitVarInsn(ALOAD, 2)
				mv.visitVarInsn(ILOAD, 4)
				mv.visitVarInsn(ILOAD, 5)
				mv.visitVarInsn(ILOAD, 6)
				mv.visitVarInsn(ILOAD, 7)
				mv.visitVarInsn(FLOAD, 8)
				mv.visitVarInsn(FLOAD, 9)
				mv.visitVarInsn(FLOAD, 10)
				// invoke our prefix
				mv.visitMethodInsn(INVOKESTATIC, "targetsan/mcmods/gateway/Utils", "flintAndSteelPreUse", "(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/player/EntityPlayer;IIIIFFF)Z")
				val notTrue = new Label()
				mv.visitJumpInsn(IFEQ, notTrue)
				mv.visitInsn(ICONST_1)
				mv.visitInsn(IRETURN)
				mv.visitLabel(notTrue)
			}
		}
	}
	
}
