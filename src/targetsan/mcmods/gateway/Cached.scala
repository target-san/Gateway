package targetsan.mcmods.gateway

/** Provides effectively lazy resettable value
 */
final class Cached[T >: Null](private val init: () => T) {
	private var hasValue: Boolean = false
	private var cache: T = null
	
	def value =
	{
		if (!hasValue)
		{
			cache = init()
			hasValue = true
		}
		cache
	}
	
	def reset
	{
		hasValue = false
		cache = null
	}
}
