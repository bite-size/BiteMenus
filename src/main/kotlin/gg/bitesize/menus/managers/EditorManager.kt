package gg.bitesize.menus.managers

import gg.bitesize.menus.models.Menu
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
}
