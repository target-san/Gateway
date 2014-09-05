package targetsan.mcmods.gateway

import net.minecraft.util.IIcon

object IconTransform
{
	object Mode extends Enumeration
	{
		type Mode = Value
		val Identity, Rotate90, Rotate180, Rotate270, FlipWidth, FlipHeight = Value
	}
	
	import Mode._
	
	def apply(icon: IIcon, mode: Mode): IIcon =
	{
		mode match
		{
			case Identity => icon
			case FlipWidth => new IconTransform(icon, FlipU, IdentV)
			case FlipHeight => new IconTransform(icon, IdentU, FlipV)
			case Rotate90 => new IconTransform(icon, IdentV, FlipU)
			case Rotate180 => new IconTransform(icon, FlipU, FlipV)
			case Rotate270 => new IconTransform(icon, FlipV, IdentU)
		}
	}
}

trait TexCoord
{
	def min(icon: IIcon): Float
	def max(icon: IIcon): Float
	def pixSize(icon: IIcon): Int
}

object IdentU extends TexCoord
{
	def min(icon: IIcon): Float = icon.getMinU()
	def max(icon: IIcon): Float = icon.getMaxU()
	def pixSize(icon: IIcon): Int = icon.getIconWidth()
}

object IdentV extends TexCoord
{
	def min(icon: IIcon): Float = icon.getMinV()
	def max(icon: IIcon): Float = icon.getMaxV()
	def pixSize(icon: IIcon): Int = icon.getIconHeight()
}

object FlipU extends TexCoord
{
	def min(icon: IIcon): Float = icon.getMaxU()
	def max(icon: IIcon): Float = icon.getMinU()
	def pixSize(icon: IIcon): Int = icon.getIconWidth()
}

object FlipV extends TexCoord
{
	def min(icon: IIcon): Float = icon.getMaxV()
	def max(icon: IIcon): Float = icon.getMinV()
	def pixSize(icon: IIcon): Int = icon.getIconHeight()
}

class IconTransform(private val icon: IIcon, private val u: TexCoord, private val v: TexCoord) extends IIcon
{
	override def getMinU: Float = u.min(icon)
	override def getMaxU: Float = u.max(icon)
	override def getMinV: Float = v.min(icon)
	override def getMaxV: Float = v.max(icon)
	// These two will change only for SwapUV
	override def getIconWidth: Int = u.pixSize(icon)
	override def getIconHeight: Int = v.pixSize(icon)
	// These three won't change
	override def getInterpolatedU(u: Double): Float =
		getMinU + (getMaxU - getMinU) * (u.toFloat / 16.0F)
	override def getInterpolatedV(v: Double): Float =
		getMinV + (getMaxV - getMinV) * (v.toFloat / 16.0F)
	override def getIconName: String =
		icon.getIconName()
}
