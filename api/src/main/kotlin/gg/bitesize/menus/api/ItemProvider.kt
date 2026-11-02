package gg.bitesize.menus.api

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * Draws a button for one viewer. Called on the main thread when the menu opens and, for menus with
 * auto-refresh, once a second while it stays open, so keep it cheap and serve cached data.
 *
 * The returned item's name and lore go through the same placeholder and color parsing as stored
 * icons. Return null to show the button's stored icon. Providers are skipped in Edit Mode.
 */
public fun interface ItemProvider {
    public fun provide(context: ItemContext): ItemStack?
}

public interface ItemContext {
    public val player: Player
    public val menu: MenuView
    public val slot: Int
    public val button: MenuButton

    /** False on the first draw after opening, true on auto-refresh redraws. */
    public val isRefresh: Boolean
}
