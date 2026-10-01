package gg.bitesize.menus

import gg.bitesize.menus.listeners.MenuListener
import gg.bitesize.menus.managers.CommandManager
import gg.bitesize.menus.managers.ConfigManager
import gg.bitesize.menus.managers.CooldownManager
import gg.bitesize.menus.managers.EditorManager
import gg.bitesize.menus.managers.MenuManager
import gg.bitesize.menus.managers.VaultManager
import gg.bitesize.menus.models.MenuHolder
import org.bukkit.plugin.java.JavaPlugin

class BiteMenus : JavaPlugin() {

    companion object {
        lateinit var INSTANCE: BiteMenus
    }

    override fun onLoad() {
        INSTANCE = this
    }

    override fun onEnable() {
        ConfigManager.loadConfigs()
        CooldownManager.loadConfig()
        MenuManager.loadMenus()
        VaultManager.setup()

        server.pluginManager.registerEvents(MenuListener(), this)

        CommandManager.registerAll()
        CommandManager.syncCommands()

        server.scheduler.runTaskTimerAsynchronously(this, Runnable {
            for (player in server.onlinePlayers) {
                val inventory = player.openInventory.topInventory
                val holder = inventory.holder
                if (holder is MenuHolder) {
                    val menu = holder.menu
                    if (menu.autoRefresh && !EditorManager.isEditing(player)) {
                        menu.refresh(player, inventory)
                    }
                }
            }
        }, 20L, 20L)
    }

    override fun onDisable() {
        ConfigManager.saveConfigs()

        CommandManager.shutdown()

        CooldownManager.shutdown()
    }

}