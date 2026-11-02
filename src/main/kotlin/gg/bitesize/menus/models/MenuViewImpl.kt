package gg.bitesize.menus.models

import gg.bitesize.menus.api.MenuButton
import gg.bitesize.menus.api.MenuView
import gg.bitesize.menus.managers.ApiManager
import gg.bitesize.menus.managers.MenuManager
import org.bukkit.Material
import org.bukkit.entity.Player

/** Resolves the menu by name on every call, since /bm reload replaces every Menu instance. */
class MenuViewImpl(override val name: String) : MenuView {

    private fun menu(): Menu = MenuManager.getMenu(name)
        ?: throw IllegalStateException("Menu '$name' no longer exists")

    private fun writableMenu(): Menu {
        ApiManager.requireMainThread()
        val menu = menu()
        check(!MenuManager.isUnreadable(menu.name)) { "The file for menu '$name' failed to load. It must be fixed and reloaded before changes are allowed." }
        return menu
    }

    override var title: String
        get() = menu().title
        set(value) { writableMenu().title = value }

    override var size: Int
        get() = menu().size
        set(value) {
            require(value in ApiManager.VALID_SIZES) { "Size must be a multiple of 9 between 9 and 54" }
            writableMenu().size = value
        }

    override var permission: String?
        get() = menu().permission
        set(value) { writableMenu().permission = value }

    override var isAutoRefresh: Boolean
        get() = menu().autoRefresh
        set(value) { writableMenu().autoRefresh = value }

    override var fillerItem: Material?
        get() = menu().fillerItem?.let { Material.matchMaterial(it) }
        set(value) { writableMenu().fillerItem = value?.name }

    override val created: Long
        get() = menu().created

    override val isValid: Boolean
        get() = MenuManager.getMenu(name) != null

    override val buttons: Map<Int, MenuButton>
        get() = menu().buttons.toSortedMap().mapValues { it.value.toApi() }

    override fun getButton(slot: Int): MenuButton? = menu().buttons[slot]?.toApi()

    override fun setButton(slot: Int, button: MenuButton?) {
        val menu = writableMenu()
        require(slot in 0 until menu.size) { "Slot $slot is outside menu '$name' (size ${menu.size})" }
        if (button == null) menu.buttons.remove(slot) else menu.buttons[slot] = button.toModel()
    }

    override fun save() {
        MenuManager.saveMenu(writableMenu())
    }

    override fun open(player: Player) {
        ApiManager.openMenu(player, name)
    }

    override fun equals(other: Any?): Boolean = other is MenuViewImpl && other.name.equals(name, ignoreCase = true)

    override fun hashCode(): Int = name.lowercase().hashCode()

    override fun toString(): String = "MenuView($name)"
}

fun Button.toApi(): MenuButton = MenuButton.builder()
    .icon(icon)
    .actions(actions)
    .cooldown(cooldown.coerceAtLeast(0))
    .closeOnClick(closeOnClick)
    .permission(permission)
    .cost(cost.coerceAtLeast(0.0))
    .visibleWithoutPermission(visibleNoPermission)
    .provider(provider)
    .build()

fun MenuButton.toModel(): Button = Button(
    icon = icon,
    actions = actions.toMutableList(),
    cooldown = cooldown,
    closeOnClick = isCloseOnClick,
    permission = permission,
    cost = cost,
    visibleNoPermission = isVisibleWithoutPermission,
    provider = provider
)
