package targetsan.mcmods.gateway.connectors

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.{IInventory, ISidedInventory}
import net.minecraft.item.ItemStack
import targetsan.mcmods.gateway.ConnectorHost

/** Provides sided access for sided and normal inventories
 */
trait SidedInventoryConnector extends ConnectorHost with ISidedInventory {
	def getAccessibleSlotsFromSide(side : Int): Array[Int] = ???

	def canInsertItem(slot : Int, item : ItemStack, side : Int): Boolean = ???

	def canExtractItem(slot : Int, item : ItemStack, side : Int): Boolean = ???

	def getSizeInventory: Int = ???

	def getStackInSlot(slot : Int): ItemStack = ???

	def decrStackSize(slot : Int, count : Int): ItemStack = ???

	def getStackInSlotOnClosing(slot : Int): ItemStack = ???

	def setInventorySlotContents(slot : Int, item : ItemStack): Unit = ???

	def getInventoryName: String = ""

	def hasCustomInventoryName: Boolean = false

	def getInventoryStackLimit: Int = ???

	def markDirty(): Unit =
		sides foreach { typedTile[IInventory](_) foreach { _.markDirty() } }

	def isUseableByPlayer(player : EntityPlayer): Boolean = ???

	def openInventory(): Unit = ???

	def closeInventory(): Unit = ???

	def isItemValidForSlot(slot : Int, item : ItemStack): Boolean = ???
}
