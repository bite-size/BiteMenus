package gg.bitesize.menus.models

import org.bukkit.inventory.ItemStack

data class Button(
    var icon: ItemStack?,
    var actions: MutableList<String> = mutableListOf(),
    var cooldown: Int = 0,
    var closeOnClick: Boolean = false,
    var permission: String? = null,
    var cost: Double = 0.0,
    var visibleNoPermission: Boolean = true,
    var provider: String? = null
)
