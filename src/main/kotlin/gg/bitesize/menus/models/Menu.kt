package gg.bitesize.menus.models

import gg.bitesize.menus.BiteMenus.Companion.INSTANCE
import gg.bitesize.menus.api.event.MenuOpenEvent
import gg.bitesize.menus.managers.ApiManager
import gg.bitesize.menus.managers.EditorManager
import gg.bitesize.menus.managers.FormatManager
import gg.bitesize.menus.managers.PapiManager
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack

class MenuHolder(val menu: Menu) : InventoryHolder {
    private lateinit var inv: Inventory
    fun setInventory(inventory: Inventory) { this.inv = inventory }
    override fun getInventory(): Inventory = inv
}

data class Menu(
    val name: String,
    var title: String,
    var size: Int,
    val buttons: MutableMap<Int, Button> = mutableMapOf(),
    var autoRefresh: Boolean = true,
    var fillerItem: String? = null,
    var permission: String? = null,
    val created: Long = System.currentTimeMillis()
) {
    @Transient private var cachedFiller: ItemStack? = null
    @Transient private var cachedFillerMatName: String? = null

    private fun getFillerItem(): ItemStack? {
        if (fillerItem == null) return null
        if (cachedFiller != null && cachedFillerMatName == fillerItem) return cachedFiller

        val mat = Material.matchMaterial(fillerItem!!) ?: return null
        val filler = ItemStack(mat)
        val meta = filler.itemMeta ?: return filler
        val fillerName = gg.bitesize.menus.managers.ConfigManager.getDefaultFillerName()
        meta.setDisplayName(FormatManager.parseMiniMessage(fillerName))
        
        if (gg.bitesize.menus.managers.ConfigManager.getDefaultFillerHideAttributes()) {
            meta.addItemFlags(*org.bukkit.inventory.ItemFlag.values())
        }
        
        filler.itemMeta = meta
        cachedFiller = filler
        cachedFillerMatName = fillerItem
        return filler
    }

    fun open(player: Player) {
        if (permission != null && !player.hasPermission(permission!!)) {
            FormatManager.error(player, "<#F7567C>You do not have permission to open this menu.")
            return
        }

        val isEditing = EditorManager.isEditing(player)
        if (!isEditing) {
            val event = MenuOpenEvent(player, MenuViewImpl(name))
            Bukkit.getPluginManager().callEvent(event)
            if (event.isCancelled) return
        }
        // Copied on the main thread so commands and API calls can't change the map mid-render
        val snapshot = buttons.toMap()

        Bukkit.getScheduler().runTaskAsynchronously(INSTANCE, Runnable {
            val parsedTitle = PapiManager.parse(player, title)
            val componentTitle = FormatManager.parseMiniMessage(parsedTitle)
            val builtItems = buildItems(player, snapshot, isEditing)

            Bukkit.getScheduler().runTask(INSTANCE, Runnable {
                if (!isEditing) applyProviders(player, snapshot, builtItems, isRefresh = false)

                val holder = MenuHolder(this)
                val inventory = Bukkit.createInventory(holder, size, componentTitle)
                holder.setInventory(inventory)

                if (!isEditing) {
                    getFillerItem()?.let { filler ->
                        for (i in 0 until size) {
                            inventory.setItem(i, filler)
                        }
                    }
                }

                builtItems.forEach { (slot, item) ->
                    inventory.setItem(slot, item)
                }

                player.openInventory(inventory)

                if (autoRefresh && !isEditing) {
                    object : org.bukkit.scheduler.BukkitRunnable() {
                        override fun run() {
                            if (!player.isOnline) {
                                cancel()
                                return
                            }
                            val currentInv = player.openInventory.topInventory
                            val currentHolder = try { currentInv.holder } catch (e: Throwable) { null }
                            if (currentHolder is MenuHolder && currentHolder.menu.name == name && !EditorManager.isEditing(player)) {
                                refresh(player, currentInv)
                            } else {
                                cancel()
                            }
                        }
                    }.runTaskTimer(INSTANCE, 20L, 20L)
                }
            })
        })
    }

    fun refresh(player: Player, inventory: Inventory) {
        val isEditing = EditorManager.isEditing(player)
        val snapshot = buttons.toMap()

        Bukkit.getScheduler().runTaskAsynchronously(INSTANCE, Runnable {
            val builtItems = buildItems(player, snapshot, isEditing)

            Bukkit.getScheduler().runTask(INSTANCE, Runnable {
                if (!isEditing) applyProviders(player, snapshot, builtItems, isRefresh = true)
                builtItems.forEach { (slot, item) ->
                    inventory.setItem(slot, item)
                }
            })
        })
    }

    private fun canSee(player: Player, button: Button): Boolean =
        button.permission == null || player.hasPermission(button.permission!!) || button.visibleNoPermission

    /** Renders each visible button's stored icon. Runs off the main thread. */
    private fun buildItems(player: Player, snapshot: Map<Int, Button>, isEditing: Boolean): MutableMap<Int, ItemStack> {
        val builtItems = mutableMapOf<Int, ItemStack>()
        snapshot.forEach { (slot, button) ->
            if (slot !in 0 until size || !canSee(player, button)) return@forEach
            button.icon?.let { builtItems[slot] = renderItem(player, it, isEditing) }
        }
        return builtItems
    }

    /** Swaps in items from API item providers. Runs on the main thread, as the API promises providers. */
    private fun applyProviders(player: Player, snapshot: Map<Int, Button>, builtItems: MutableMap<Int, ItemStack>, isRefresh: Boolean) {
        snapshot.forEach { (slot, button) ->
            if (button.provider == null || slot !in 0 until size || !canSee(player, button)) return@forEach
            ApiManager.provideItem(button, player, this, slot, isRefresh)?.let {
                builtItems[slot] = renderItem(player, it, isEditing = false)
            }
        }
    }

    private fun renderItem(player: Player, source: ItemStack, isEditing: Boolean): ItemStack {
        val item = source.clone()
        if (!item.hasItemMeta()) return item
        val meta = item.itemMeta ?: return item
        if (meta.hasDisplayName()) {
            val originalName = meta.displayName
            val parsed = if (isEditing) originalName else PapiManager.parse(player, originalName)
            meta.setDisplayName(FormatManager.parseMiniMessage(parsed))
        }

        if (meta.hasLore()) {
            val newLore = mutableListOf<String>()
            meta.lore?.forEach { line ->
                val parsed = if (isEditing) line else PapiManager.parse(player, line)
                newLore.add(FormatManager.parseMiniMessage(parsed))
            }
            meta.lore = newLore
        }
        item.itemMeta = meta
        return item
    }
}
