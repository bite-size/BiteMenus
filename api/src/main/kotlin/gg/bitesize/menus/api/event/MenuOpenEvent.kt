package gg.bitesize.menus.api.event

import gg.bitesize.menus.api.MenuView
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerEvent

/**
 * Fired on the main thread before a menu opens for a player who has its permission.
 * Not fired for Edit Mode. Cancelling it keeps the menu closed.
 */
public class MenuOpenEvent(player: Player, public val menu: MenuView) : PlayerEvent(player), Cancellable {

    private var cancelled = false

    override fun isCancelled(): Boolean = cancelled

    override fun setCancelled(cancel: Boolean) {
        cancelled = cancel
    }

    override fun getHandlers(): HandlerList = HANDLERS

    public companion object {
        private val HANDLERS = HandlerList()

        @JvmStatic
        public fun getHandlerList(): HandlerList = HANDLERS
    }
}
