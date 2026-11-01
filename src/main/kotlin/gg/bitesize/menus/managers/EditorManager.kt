package gg.bitesize.menus.managers

import gg.bitesize.menus.models.Menu
import gg.bitesize.menus.models.MenuHolder
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID

object EditorManager {
    private val editingPlayers = mutableMapOf<UUID, Menu>()

    fun startEditing(player: Player, menu: Menu) {
        editingPlayers[player.uniqueId] = menu
    }

    fun stopEditing(player: Player) {
        editingPlayers.remove(player.uniqueId)
    }

    fun getEditingMenu(player: Player): Menu? {
        return editingPlayers[player.uniqueId]
    }

    fun isEditing(player: Player): Boolean {
        return editingPlayers.containsKey(player.uniqueId)
    }

    /**
     * Players who currently have their edit inventory open. Entries left behind by an edit
     * inventory that never opened or was closed elsewhere are pruned, since they hold stale menus.
     */
    fun getActiveEditors(): List<Player> {
        val active = mutableListOf<Player>()
        editingPlayers.entries.removeIf { (uuid, menu) ->
            val player = Bukkit.getPlayer(uuid) ?: return@removeIf true
            val holder = try { player.openInventory.topInventory.holder } catch (e: Throwable) { null }
            val open = holder is MenuHolder && holder.menu.name == menu.name
            if (open) active.add(player)
            !open
        }
        return active
    }
}
