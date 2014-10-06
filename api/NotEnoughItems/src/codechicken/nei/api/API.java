/** Stripped down from original codechicken.nei.api.API
 *
 */
package codechicken.nei.api;

import net.minecraft.item.ItemStack;

/**
 * This is the main class that handles item property configuration.
 * WARNING: DO NOT access this class until the world has been loaded
 * These methods should be called from INEIConfig implementors
 */
public class API
{
    /**
     * Hide an item from the item panel
     * Damage values of OreDictionary.WILDCARD_VALUE and null NBT tags function as wildcards for their respective variables
     */
    public static void hideItem(ItemStack item) { }
}
