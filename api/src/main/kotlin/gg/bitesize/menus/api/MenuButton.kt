package gg.bitesize.menus.api

import org.bukkit.inventory.ItemStack

/**
 * An immutable snapshot of a button. Create one with [builder], or change one with [toBuilder]
 * and pass the result to [MenuView.setButton].
 */
public class MenuButton private constructor(
    private val iconItem: ItemStack?,
    /** Actions run in order on click, such as `[MESSAGE] hi` or `[CONSOLE] give @p diamond`. */
    public val actions: List<String>,
    /** Seconds before the same player can click this button again. */
    public val cooldown: Int,
    public val isCloseOnClick: Boolean,
    /** Permission needed to click, or null for none. */
    public val permission: String?,
    /** Vault economy cost per click. */
    public val cost: Double,
    /** Whether players without [permission] still see the button. */
    public val isVisibleWithoutPermission: Boolean,
    /** Id of the [ItemProvider] that draws this button, or null to always show [icon]. */
    public val provider: String?
) {

    /** The stored icon, cloned so changes to it don't leak back into the button. */
    public val icon: ItemStack? get() = iconItem?.clone()

    public fun toBuilder(): Builder = Builder()
        .icon(iconItem)
        .actions(actions)
        .cooldown(cooldown)
        .closeOnClick(isCloseOnClick)
        .permission(permission)
        .cost(cost)
        .visibleWithoutPermission(isVisibleWithoutPermission)
        .provider(provider)

    override fun toString(): String =
        "MenuButton(icon=${iconItem?.type}, actions=$actions, cooldown=$cooldown, closeOnClick=$isCloseOnClick, " +
            "permission=$permission, cost=$cost, visibleWithoutPermission=$isVisibleWithoutPermission, provider=$provider)"

    public class Builder {
        private var icon: ItemStack? = null
        private val actions = mutableListOf<String>()
        private var cooldown = 0
        private var closeOnClick = false
        private var permission: String? = null
        private var cost = 0.0
        private var visibleWithoutPermission = true
        private var provider: String? = null

        public fun icon(icon: ItemStack?): Builder = apply { this.icon = icon?.clone() }

        public fun actions(actions: List<String>): Builder = apply {
            this.actions.clear()
            this.actions.addAll(actions)
        }

        public fun addAction(action: String): Builder = apply { actions.add(action) }

        public fun cooldown(seconds: Int): Builder = apply {
            require(seconds >= 0) { "Cooldown cannot be negative" }
            cooldown = seconds
        }

        public fun closeOnClick(closeOnClick: Boolean): Builder = apply { this.closeOnClick = closeOnClick }

        public fun permission(permission: String?): Builder = apply { this.permission = permission }

        public fun cost(cost: Double): Builder = apply {
            require(cost >= 0.0) { "Cost cannot be negative" }
            this.cost = cost
        }

        public fun visibleWithoutPermission(visible: Boolean): Builder = apply { visibleWithoutPermission = visible }

        public fun provider(id: String?): Builder = apply { provider = id?.lowercase() }

        public fun build(): MenuButton = MenuButton(
            icon?.clone(), actions.toList(), cooldown, closeOnClick, permission, cost, visibleWithoutPermission, provider
        )
    }

    public companion object {
        @JvmStatic
        public fun builder(): Builder = Builder()
    }
}
