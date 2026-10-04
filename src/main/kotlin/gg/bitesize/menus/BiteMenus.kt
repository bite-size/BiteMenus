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

        val pluginId = 34487 // bStats Plugin ID
        org.bstats.bukkit.Metrics(this, pluginId)

        server.pluginManager.registerEvents(MenuListener(), this)

        CommandManager.registerAll()
        CommandManager.syncCommands()

    }

    override fun onDisable() {
        CommandManager.shutdown()

        CooldownManager.shutdown()
    }

}