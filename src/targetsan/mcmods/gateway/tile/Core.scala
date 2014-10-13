package targetsan.mcmods.gateway.tile

import java.nio.ByteBuffer
import java.util.UUID

import net.minecraft.nbt.NBTTagCompound
import targetsan.mcmods.gateway.Utils._

class Core extends Gateway{

	private var partnerPos: BlockPos = null
	private var ownerId: UUID = null
	private var ownerName = ""
	private var flags = 0

	//******************************************************************************************************************
	// Flag field parts
	//******************************************************************************************************************

	//******************************************************************************************************************
	// Persistence
	//******************************************************************************************************************
	override def readFromNBT(tag: NBTTagCompound): Unit = {
		super.readFromNBT(tag)

		// owner's UUID is composed as 2 longs from 16-byte byte array
		val id = ByteBuffer.wrap(tag.getByteArray("ownerId"))
		val lower = id.getLong
		val upper = id.getLong
		ownerId = new UUID(upper, lower)

		// partner node pos is encoded as 4 ints - x, y, z, dimension id
		val coords = tag.getIntArray("partnerPos")
		partnerPos = new BlockPos(coords(0), coords(1), coords(2), coords(3))

		// owner name and flags are trivial
		ownerName = tag.getString("ownerName")
		flags = tag.getInteger("flags")
	}

	override def writeToNBT(tag: NBTTagCompound): Unit = {
		super.writeToNBT(tag)

		// Store owner's UUID
		tag.setByteArray("ownerId",
			ByteBuffer.allocate(16)
				.putLong(ownerId.getLeastSignificantBits)
				.putLong(ownerId.getMostSignificantBits)
				.array()
		)
		// Store partner position
		tag.setIntArray("partnerPos",
			Array[Int](partnerPos.x, partnerPos.y, partnerPos.z, partnerPos.dim)
		)
		// Other fields are trivial
		tag.setString("ownerName", ownerName)
		tag.setInteger("flags", flags)
	}
}
