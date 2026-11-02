package gg.bitesize.menus.api

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

/**
 * Entry point for plugins that integrate with BiteMenus.
 *
 * Get it with [BiteMenusApi.get] (or from Bukkit's `ServicesManager`) once BiteMenus has enabled,
 * which is guaranteed when your plugin lists `BiteMenus` under `depend` in its plugin.yml.
 *
 * Everything here must be called on the main server thread unless stated otherwise.
 */
public interface BiteMenusApi {

    /** Looks up a menu by name, ignoring case. */
    public fun getMenu(name: String): MenuView?

    /** Every loaded menu. The collection is a snapshot and does not update. */
    public fun getMenus(): Collection<MenuView>

    /**
     * Opens a menu for a player, applying the menu's permission and firing
     * [gg.bitesize.menus.api.event.MenuOpenEvent]. Returns false when no menu has that name.
     */
    public fun openMenu(player: Player, name: String): Boolean

    /**
     * Creates and saves a new, empty menu using the server's configured defaults
     * for permission, filler item and auto-refresh.
     *
     * @param size slot count, a multiple of 9 between 9 and 54
     * @throws IllegalArgumentException if the name or size is invalid, or a menu with that name exists
     */
    public fun createMenu(name: String, title: String, size: Int): MenuView

    /** Deletes a menu and its file. Returns false when no menu has that name. */
    public fun deleteMenu(name: String): Boolean

    /**
     * Registers a custom button action. A button action written as `[TAG] arguments` calls [handler]
     * with the arguments, after PlaceholderAPI placeholders and `@p` have been replaced.
     *
     * Tags are case-insensitive and may contain letters, digits, `_` and `-`. The built-in tags
     * (`CLOSE`, `MESSAGE`, `MENU`, `PLAYER`, `CONSOLE`) cannot be overridden. The handler is removed
     * automatically when [plugin] disables.
     *
     * @throws IllegalArgumentException if the tag is invalid or built in
     * @throws IllegalStateException if another plugin already registered the tag
     */
    public fun registerAction(plugin: Plugin, tag: String, handler: ActionHandler)

    /** Removes a custom action. Returns false when the tag was not registered. */
    public fun unregisterAction(tag: String): Boolean

    /**
     * Registers an item provider. A button whose `provider` flag is set to [id] shows the item
     * [provider] returns for each viewer, falling back to the button's stored icon when it returns
     * null or no provider with that id is registered.
     *
     * Ids are case-insensitive and may contain letters, digits, `_`, `-`, `.` and `:`. Namespacing
     * them by plugin, such as `myplugin:balance`, avoids collisions. The provider is removed
     * automatically when [plugin] disables.
     *
     * @throws IllegalArgumentException if the id is invalid
     * @throws IllegalStateException if another plugin already registered the id
     */
    public fun registerItemProvider(plugin: Plugin, id: String, provider: ItemProvider)

    /** Removes an item provider. Returns false when the id was not registered. */
    public fun unregisterItemProvider(id: String): Boolean

    public companion object {
        /**
         * Returns the registered API.
         *
         * @throws IllegalStateException if BiteMenus is not enabled
         */
        @JvmStatic
        public fun get(): BiteMenusApi =
            Bukkit.getServicesManager().load(BiteMenusApi::class.java)
                ?: throw IllegalStateException("BiteMenus is not enabled. Add it to depend in your plugin.yml.")
    }
}
