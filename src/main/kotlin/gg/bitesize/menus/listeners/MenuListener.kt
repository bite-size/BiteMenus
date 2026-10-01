package gg.bitesize.menus.listeners

import gg.bitesize.menus.BiteMenus.Companion.INSTANCE
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

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        val inventory = event.inventory
        val holder = inventory.holder
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

        if (button.permission != null && !player.hasPermission(button.permission!!)) {
            return
        }

        val cooldownKey = "btn_${menu.name}_$slot"
        if (button.cooldown > 0 && CooldownManager.isOnCooldown(player, cooldownKey)) {
            val time = CooldownManager.getRemainingFormatted(player, cooldownKey)
            val msg = ConfigManager.getMessage("errors.cooldown", "<red>Please wait <yellow><time> <red>before doing that.").replace("<time>", time)
            FormatManager.error(player, msg)
            return
        }

        if (button.cost > 0) {
            val economy = VaultManager.economy
            if (economy != null) {
                if (!economy.has(player, button.cost)) {
                    val msg = ConfigManager.getMessage("errors.insufficient-funds", "<red>You don't have enough money.")
                    FormatManager.error(player, msg)
                    return
                }
                val response = economy.withdrawPlayer(player, button.cost)
                if (!response.transactionSuccess()) {
                    val msg = ConfigManager.getMessage("errors.transaction-failed", "<red>Transaction failed.")
                    FormatManager.error(player, msg)
                    return
                }
                val formattedCost = economy.format(button.cost)
                val successMsg = ConfigManager.getMessage("messages.transaction-success", "<green>Purchased for <cost>.").replace("<cost>", formattedCost)
                FormatManager.send(player, successMsg)
            } else {
                val msg = ConfigManager.getMessage("errors.vault-unavailable", "<red>Vault economy is not available! Contact an administrator.")
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
                    if (parsedAction.startsWith("[CLOSE]", ignoreCase = true)) {
                        player.closeInventory()
                    } else if (parsedAction.startsWith("[MESSAGE]", ignoreCase = true)) {
                        val msg = parsedAction.substring(9).trim()
                        FormatManager.send(player, msg, usePrefix = false)
                    } else if (parsedAction.startsWith("[MENU]", ignoreCase = true)) {
                        val menuName = parsedAction.substring(6).trim()
                        val targetMenu = MenuManager.getMenu(menuName)
                        if (targetMenu != null) {
                            targetMenu.open(player)
                        } else {
                            val msg = ConfigManager.getMessage("errors.menu-not-found", "<red>Menu not found.")
                            FormatManager.error(player, msg)
                        }
                    } else if (parsedAction.startsWith("[PLAYER]", ignoreCase = true)) {
                        val cmd = parsedAction.substring(8).trim()
                        player.performCommand(cmd)
                    } else if (parsedAction.startsWith("[CONSOLE]", ignoreCase = true)) {
                        val cmd = parsedAction.substring(9).trim()
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd)
                    } else {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsedAction)
                    }
                })
            }

            if (button.closeOnClick) {
                Bukkit.getScheduler().runTask(INSTANCE, Runnable {
                    player.closeInventory()
                })
            }
        })
    }

    @EventHandler
    fun onDrag(event: InventoryDragEvent) {
        val holder = event.inventory.holder
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
            val msg = ConfigManager.getMessage("messages.selection-cancelled", "<red>Cancelled selection.")
            FormatManager.send(player, msg)
            return
        }

        if (EditorManager.isEditing(player)) {
            val menu = EditorManager.getEditingMenu(player)
            val holder = event.inventory.holder
            if (holder is MenuHolder && holder.menu.name == menu?.name) {
                // Sync inventory to Menu
                val topInv = event.view.topInventory
                for (i in 0 until menu.size) {
                    val item = topInv.getItem(i)
                    if (item == null || item.type == Material.AIR) {
                        menu.buttons.remove(i)
                    } else {
                        val button = menu.buttons.getOrPut(i) { Button(icon = item) }
                        button.icon = item
                    }
                }
                MenuManager.saveMenu(menu)
                EditorManager.stopEditing(player)
                val msg = ConfigManager.getMessage("messages.edit-mode-saved", "<green>Saved and exited Edit Mode for <menu>.").replace("<menu>", menu.name)
                FormatManager.send(player, msg)
            }
        }
    }
}
