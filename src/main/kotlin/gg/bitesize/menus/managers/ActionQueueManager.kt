package gg.bitesize.menus.managers

import org.bukkit.entity.Player
import java.util.UUID

object ActionQueueManager {
    private val pendingActions = mutableMapOf<UUID, (Int) -> Unit>()

    fun queueAction(player: Player, action: (Int) -> Unit) {
        pendingActions[player.uniqueId] = action
    }

    fun hasPendingAction(player: Player): Boolean {
        return pendingActions.containsKey(player.uniqueId)
    }

    fun executeAction(player: Player, slot: Int) {
        val action = pendingActions.remove(player.uniqueId)
        action?.invoke(slot)
    }

    fun cancelAction(player: Player) {
        pendingActions.remove(player.uniqueId)
    }
}
