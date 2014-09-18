package targetsan.mcmods.gateway

// Used to check if some dependencies of cached value were changed
trait Versioned {
	protected def version: Int
}

/** Provides effectively lazy resettable value
 */
final class Cached[T >: Null](private val init: () => T, deps: Versioned*) extends Versioned {
	private var cache: Option[T] = None
	private var versionNum = 0
	private var dependencies = deps map { x => (x, x.version) }

	protected def version = versionNum
	
	def get = {
		// check if any of dependnecies has changed and so update current value accordingly
		if (dependencies exists { x => x._1.version > x._2} ) {
			reset()
			dependencies = deps map { x => (x, x.version) }
		}
		// If value isn't cached, then re-init it
		if (cache.isEmpty) {
			cache = Some(init())
		}
		// Value should be fine here
		cache.get
	}
	
	def reset() {
		cache = None
		versionNum += 1
	}
}
