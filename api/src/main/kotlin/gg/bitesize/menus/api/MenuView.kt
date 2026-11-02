package gg.bitesize.menus.api

import org.bukkit.Material
import org.bukkit.entity.Player

/**
 * A live handle to a menu, looked up by name on every call, so it stays valid across `/bm reload`.
 *
 * Setters change the loaded menu immediately but only reach its file once [save] is called.
 * Every method throws [IllegalStateException] once the menu has been deleted, and every setter
 * throws it while the menu's file has failed to load, so a save can't overwrite the owner's edits.
 */
public interface MenuView {

    /** The menu's name, as typed when it was created. */
    public val name: String

    /** The raw title, which may contain MiniMessage tags, legacy `&` codes and placeholders. */
    public var title: String

    /** Slot count, a multiple of 9 between 9 and 54. */
    public var size: Int

    /** Permission needed to open the menu, or null for none. */
    public var permission: String?

    /** Whether open menus redraw every second, which keeps placeholders and providers current. */
    public var isAutoRefresh: Boolean

    /** Item placed in every empty slot, or null for none. */
    public var fillerItem: Material?

    /** Epoch milliseconds when the menu was created. */
    public val created: Long

    /** False once the menu has been deleted or dropped by a reload. */
    public val isValid: Boolean

    /** Buttons by slot. The map is a snapshot and does not update. */
    public val buttons: Map<Int, MenuButton>

    public fun getButton(slot: Int): MenuButton?

    /**
     * Places a button, replacing any already in that slot, or clears the slot when [button] is null.
     *
     * @throws IllegalArgumentException if the slot is outside the menu
     */
    public fun setButton(slot: Int, button: MenuButton?)

    /** Writes the menu to its file. */
    public fun save()

    /** Same as [BiteMenusApi.openMenu] for this menu. */
    public fun open(player: Player)
}
