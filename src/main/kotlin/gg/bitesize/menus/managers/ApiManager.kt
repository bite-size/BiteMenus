package gg.bitesize.menus.managers

import gg.bitesize.menus.BiteMenus.Companion.INSTANCE
import gg.bitesize.menus.api.ActionContext
import gg.bitesize.menus.api.ActionHandler
import gg.bitesize.menus.api.BiteMenusApi
import gg.bitesize.menus.api.ItemContext
import gg.bitesize.menus.api.ItemProvider
import gg.bitesize.menus.api.MenuButton
import gg.bitesize.menus.api.MenuView
import gg.bitesize.menus.models.Button
import gg.bitesize.menus.models.Menu
import gg.bitesize.menus.models.MenuViewImpl
import gg.bitesize.menus.models.toApi
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.server.PluginDisableEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level

object ApiManager : BiteMenusApi, Listener {

    val VALID_SIZES = listOf(9, 18, 27, 36, 45, 54)
    val BUILT_IN_ACTIONS = setOf("CLOSE", "MESSAGE", "MENU", "PLAYER", "CONSOLE")
    private val menuNameRegex = Regex("^[a-zA-Z0-9_\\-]+$")
    private val actionTagRegex = Regex("^[A-Z0-9_\\-]+$")
    val providerIdRegex = Regex("^[a-z0-9_.:\\-]+$")

    private class Registration<T>(val plugin: Plugin, val value: T)

    // Concurrent because menus check for providers while rendering off the main thread
    private val actions = ConcurrentHashMap<String, Registration<ActionHandler>>()
    private val providers = ConcurrentHashMap<String, Registration<ItemProvider>>()
    // Providers that already threw, so a broken one logs once instead of on every refresh
    private val failingProviders = ConcurrentHashMap.newKeySet<String>()

    private data class ActionContextImpl(
        override val player: Player,
        override val menu: MenuView,
        override val slot: Int,
        override val clickType: ClickType,
        override val tag: String,
        override val arguments: String
    ) : ActionContext

    private data class ItemContextImpl(
        override val player: Player,
        override val menu: MenuView,
        override val slot: Int,
        override val button: MenuButton,
        override val isRefresh: Boolean
    ) : ItemContext

    fun requireMainThread() {
        check(Bukkit.isPrimaryThread()) { "The BiteMenus API must be called on the main server thread" }
    }

    override fun getMenu(name: String): MenuView? = MenuManager.getMenu(name)?.let { MenuViewImpl(it.name) }

    override fun getMenus(): Collection<MenuView> = MenuManager.getMenus().map { MenuViewImpl(it.name) }

    override fun openMenu(player: Player, name: String): Boolean {
        requireMainThread()
        val menu = MenuManager.getMenu(name) ?: return false
        menu.open(player)
        return true
    }

    override fun createMenu(name: String, title: String, size: Int): MenuView {
        requireMainThread()
        require(name.matches(menuNameRegex)) { "Menu names can only contain letters, digits, dashes and underscores" }
        require(size in VALID_SIZES) { "Size must be a multiple of 9 between 9 and 54" }
        require(!MenuManager.isUnreadable(name)) { "A file for menu '$name' exists but failed to load" }
        require(MenuManager.getMenu(name) == null) { "Menu '$name' already exists" }

        val menu = Menu(name, title, size)
        menu.permission = ConfigManager.getDefaultMenuPermission(name)
        menu.autoRefresh = ConfigManager.getDefaultAutoRefresh()
        menu.fillerItem = ConfigManager.getDefaultFillerItem()
        MenuManager.saveMenu(menu)
        return MenuViewImpl(name)
    }

    override fun deleteMenu(name: String): Boolean {
        requireMainThread()
        val menu = MenuManager.getMenu(name) ?: return false
        MenuManager.removeMenu(menu.name)
        return true
    }

    override fun registerAction(plugin: Plugin, tag: String, handler: ActionHandler) {
        val key = tag.uppercase()
        require(key.matches(actionTagRegex)) { "Action tags can only contain letters, digits, dashes and underscores" }
        require(key !in BUILT_IN_ACTIONS) { "[$key] is a built-in action and cannot be replaced" }
        val existing = actions[key]
        check(existing == null || existing.plugin == plugin) { "Action [$key] is already registered by ${existing?.plugin?.name}" }
        actions[key] = Registration(plugin, handler)
    }

    override fun unregisterAction(tag: String): Boolean = actions.remove(tag.uppercase()) != null

    override fun registerItemProvider(plugin: Plugin, id: String, provider: ItemProvider) {
        val key = id.lowercase()
        require(key.matches(providerIdRegex)) { "Provider ids can only contain letters, digits, '_', '-', '.' and ':'" }
        val existing = providers[key]
        check(existing == null || existing.plugin == plugin) { "Item provider '$key' is already registered by ${existing?.plugin?.name}" }
        providers[key] = Registration(plugin, provider)
        failingProviders.remove(key)
    }

    override fun unregisterItemProvider(id: String): Boolean = providers.remove(id.lowercase()) != null

    fun getProviderIds(): Set<String> = providers.keys.toSortedSet()

    /** Runs a registered custom action. Returns false when no plugin registered [tag]. */
    fun runAction(tag: String, player: Player, menu: Menu, slot: Int, clickType: ClickType, arguments: String): Boolean {
        val registration = actions[tag] ?: return false
        try {
            registration.value.handle(ActionContextImpl(player, MenuViewImpl(menu.name), slot, clickType, tag, arguments))
        } catch (e: Throwable) {
            INSTANCE.logger.log(Level.SEVERE, "Action [$tag] from ${registration.plugin.name} failed in menu ${menu.name}", e)
        }
        return true
    }

    /** Asks the button's provider for this viewer's item. Null means show the stored icon. */
    fun provideItem(button: Button, player: Player, menu: Menu, slot: Int, isRefresh: Boolean): ItemStack? {
        val id = button.provider ?: return null
        val registration = providers[id] ?: return null
        return try {
            registration.value.provide(ItemContextImpl(player, MenuViewImpl(menu.name), slot, button.toApi(), isRefresh))
        } catch (e: Throwable) {
            if (failingProviders.add(id)) {
                INSTANCE.logger.log(Level.SEVERE, "Item provider '$id' from ${registration.plugin.name} failed in menu ${menu.name}. Showing the stored icon; further errors from it are hidden until it re-registers.", e)
            }
            null
        }
    }

    @EventHandler
    fun onPluginDisable(event: PluginDisableEvent) {
        actions.values.removeIf { it.plugin == event.plugin }
        providers.values.removeIf { it.plugin == event.plugin }
    }

    fun shutdown() {
        actions.clear()
        providers.clear()
        failingProviders.clear()
    }
}
