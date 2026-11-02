package gg.bitesize.menus.api

import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType

/** Runs a custom `[TAG]` button action. Called on the main thread. */
public fun interface ActionHandler {
    public fun handle(context: ActionContext)
}

public interface ActionContext {
    public val player: Player
    public val menu: MenuView
    public val slot: Int
    public val clickType: ClickType

    /** Upper-cased tag, without brackets. */
    public val tag: String

    /** Text after the tag, trimmed, with placeholders and `@p` already replaced. */
    public val arguments: String
}
