package gg.bitesize.menus.api.event

import gg.bitesize.menus.api.MenuButton
import gg.bitesize.menus.api.MenuView
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.HandlerList
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.player.PlayerEvent

/**
 * Fired on the main thread when a player who has the button's permission clicks it, before the
 * cooldown is checked or any cost is charged. Cancelling it skips the cooldown, cost and actions.
 */
public class ButtonClickEvent(
    player: Player,
    public val menu: MenuView,
    public val slot: Int,
    public val button: MenuButton,
    public val clickType: ClickType
) : PlayerEvent(player), Cancellable {

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
