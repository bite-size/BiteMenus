package gg.bitesize.menus.api.event

import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * Fired on the main thread after `/bm reload` has reloaded the menu files. Registered actions and
 * providers survive reloads, so this is only needed to refresh data you derived from the menus.
 */
public class MenusReloadedEvent(
    /** File names of menus that failed to load and kept their previous version. */
    public val failedMenus: List<String>
) : Event() {

    override fun getHandlers(): HandlerList = HANDLERS

    public companion object {
        private val HANDLERS = HandlerList()

        @JvmStatic
        public fun getHandlerList(): HandlerList = HANDLERS
    }
}
