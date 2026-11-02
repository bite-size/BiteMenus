package gg.bitesize.menus.listeners

import gg.bitesize.menus.BiteMenus.Companion.INSTANCE
import gg.bitesize.menus.api.event.ButtonClickEvent
import gg.bitesize.menus.managers.ApiManager
import gg.bitesize.menus.managers.ActionQueueManager
import gg.bitesize.menus.managers.ConfigManager
import gg.bitesize.menus.managers.CooldownManager
import gg.bitesize.menus.managers.MenuManager
import gg.bitesize.menus.managers.VaultManager
import gg.bitesize.menus.managers.EditorManager
import gg.bitesize.menus.managers.FormatManager
import gg.bitesize.menus.managers.PapiManager
import gg.bitesize.menus.models.MenuHolder
import gg.bitesize.menus.models.Button
import gg.bitesize.menus.models.MenuViewImpl
import gg.bitesize.menus.models.toApi
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.inventory.InventoryType
import java.util.concurrent.TimeUnit

class MenuListener : Listener {

    private val actionTagRegex = Regex("""^\[([A-Za-z0-9_\-]+)]\s*(.*)$""", RegexOption.DOT_MATCHES_ALL)

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        val inventory = event.inventory
        val holder = try { inventory.holder } catch (e: Throwable) { null }
        if (holder !is MenuHolder) return

        val player = event.whoClicked as? Player ?: return
        
        if (ActionQueueManager.hasPendingAction(player)) {
            event.isCancelled = true
            val clickedInv = event.clickedInventory ?: return
            if (clickedInv.type == InventoryType.PLAYER) return
            
            ActionQueueManager.executeAction(player, event.slot)
            player.closeInventory()
            return
        }

        val menu = holder.menu
        
        // Handle Edit Mode
        if (EditorManager.isEditing(player)) {
            val editingMenu = EditorManager.getEditingMenu(player)
            if (editingMenu?.name == menu.name) {
                // DO NOT cancel the event, let them drag and drop items
                return
            }
        }

        // Normal Mode
        event.isCancelled = true

        val clickedInv = event.clickedInventory ?: return
        if (clickedInv.type == InventoryType.PLAYER) return

        val slot = event.slot
        val button = menu.buttons[slot] ?: return

        if (button.closeOnClick) {
            player.closeInventory()
        }

        if (button.permission != null && !player.hasPermission(button.permission!!)) {
            return
        }

        val clickType = event.click
        val clickEvent = ButtonClickEvent(player, MenuViewImpl(menu.name), slot, button.toApi(), clickType)
        Bukkit.getPluginManager().callEvent(clickEvent)
        if (clickEvent.isCancelled) return

        val cooldownKey = "btn_${menu.name}_$slot"
        if (button.cooldown > 0 && CooldownManager.isOnCooldown(player, cooldownKey)) {
            val time = CooldownManager.getRemainingFormatted(player, cooldownKey)
            val msg = ConfigManager.getMessage("errors.cooldown", "<#F7567C>Please wait <#4ECDC4><time> <#F7567C>before doing that.").replace("<time>", time)
            FormatManager.error(player, msg)
            return
        }

        if (button.cost > 0) {
            val economy = VaultManager.economy
            if (economy != null) {
                if (!economy.has(player, button.cost)) {
                    val msg = ConfigManager.getMessage("errors.insufficient-funds", "<#F7567C>You don't have enough money.")
                    FormatManager.error(player, msg)
                    return
                }
                val response = economy.withdrawPlayer(player, button.cost)
                if (!response.transactionSuccess()) {
                    val msg = ConfigManager.getMessage("errors.transaction-failed", "<#F7567C>Transaction failed.")
                    FormatManager.error(player, msg)
                    return
                }
                val formattedCost = economy.format(button.cost)
                val successMsg = ConfigManager.getMessage("messages.transaction-success", "<#4ECDC4>Purchased for <cost>.").replace("<cost>", formattedCost)
                FormatManager.send(player, successMsg)
            } else {
                val msg = ConfigManager.getMessage("errors.vault-unavailable", "<#F7567C>Vault economy is not available! Contact an administrator.")
                FormatManager.error(player, msg)
                return
            }
        }

        if (button.cooldown > 0) {
            CooldownManager.setCooldown(player, cooldownKey, button.cooldown.toLong(), TimeUnit.SECONDS)
        }

        Bukkit.getScheduler().runTaskAsynchronously(INSTANCE, Runnable {
            for (action in button.actions) {
                val parsedAction = PapiManager.parse(player, action)
                    .replace("@p", player.name, ignoreCase = true)
                
                Bukkit.getScheduler().runTask(INSTANCE, Runnable {
                    val match = actionTagRegex.find(parsedAction)
                    val tag = match?.groupValues?.get(1)?.uppercase()
                    val argument = match?.groupValues?.get(2)?.trim() ?: ""
                    when (tag) {
                        "CLOSE" -> player.closeInventory()
                        "MESSAGE" -> FormatManager.send(player, argument, usePrefix = false)
                        "MENU" -> {
                            val targetMenu = MenuManager.getMenu(argument)
                            if (targetMenu != null) {
                                targetMenu.open(player)
                            } else {
                                val msg = ConfigManager.getMessage("errors.menu-not-found", "<#F7567C>Menu not found.")
                                FormatManager.error(player, msg)
                            }
                        }
                        "PLAYER" -> player.performCommand(argument)
                        "CONSOLE" -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), argument)
                        else -> {
                            // Custom tags come from API plugins; anything else stays a raw console command
                            if (tag == null || !ApiManager.runAction(tag, player, menu, slot, clickType, argument)) {
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsedAction)
                            }
                        }
                    }
                })
            }
        })
    }

    @EventHandler
    fun onDrag(event: InventoryDragEvent) {
        val holder = try { event.inventory.holder } catch (e: Throwable) { null }
        if (holder is MenuHolder) {
            val player = event.whoClicked as? Player ?: return
            if (ActionQueueManager.hasPendingAction(player)) {
                event.isCancelled = true
                return
            }
            if (EditorManager.isEditing(player)) {
                val editingMenu = EditorManager.getEditingMenu(player)
                if (editingMenu?.name == holder.menu.name) {
                    // Let them drag
                    return
                }
            }
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return
        
        if (ActionQueueManager.hasPendingAction(player)) {
            ActionQueueManager.cancelAction(player)
            val msg = ConfigManager.getMessage("messages.selection-cancelled", "<#F7567C>Cancelled selection.")
            FormatManager.send(player, msg)
            return
        }

        if (EditorManager.isEditing(player)) {
            val menu = EditorManager.getEditingMenu(player)
            val holder = try { event.inventory.holder } catch (e: Throwable) { null }
            if (holder is MenuHolder && holder.menu.name == menu?.name) {
                // Sync inventory to Menu
                val topInv = event.view.topInventory
                for (i in 0 until menu.size) {
                    val item = topInv.getItem(i)
                    if (item == null || item.type == Material.AIR) {
                        menu.buttons.remove(i)
                    } else {
                        // The editor shows rendered icons, so an untouched slot holds a color-parsed copy.
                        // Keep the stored icon for those, or each edit session would bake the parsed colors in.
                        val stored = menu.buttons[i]?.icon
                        if (stored != null && item.amount == stored.amount &&
                            menu.renderItem(player, stored, isEditing = true).isSimilar(item)) continue
                        val button = menu.buttons.getOrPut(i) { Button(icon = item) }
                        button.icon = item
                    }
                }
                MenuManager.saveMenu(menu)
                EditorManager.stopEditing(player)
                val msg = ConfigManager.getMessage("messages.edit-mode-saved", "<#4ECDC4>Saved and exited Edit Mode for <menu>.").replace("<menu>", menu.name)
                FormatManager.send(player, msg)
            }
        }
    }
}
