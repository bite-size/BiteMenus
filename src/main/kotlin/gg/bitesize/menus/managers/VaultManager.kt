package gg.bitesize.menus.managers

import net.milkbowl.vault.economy.Economy
import org.bukkit.Bukkit

object VaultManager {
    var economy: Economy? = null
        private set

    fun setup(): Boolean {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false
        }
        val rsp = Bukkit.getServicesManager().getRegistration(Economy::class.java)
        if (rsp == null) {
            return false
        }
        economy = rsp.provider
        return economy != null
    }
}
