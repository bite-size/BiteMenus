package gg.bitesize.menus.commands

import gg.bitesize.menus.managers.ActionQueueManager
import gg.bitesize.menus.managers.ConfigManager
import gg.bitesize.menus.managers.CooldownManager
import gg.bitesize.menus.managers.FormatManager
import gg.bitesize.menus.managers.MenuManager
import gg.bitesize.menus.managers.EditorManager
import gg.bitesize.menus.models.Menu
import gg.bitesize.menus.models.Button
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class BiteMenusCommand : BaseCommand(
    name = "bitemenus",
    description = "Main command for BiteMenus",
    usage = "/bm help",
    aliases = listOf("bm", "bmenus")
) {
    override fun execute(sender: CommandSender, label: String, args: Array<String>): Boolean {
        if (args.isEmpty()) {
            return sendHelp(sender, label)
        }

        val subCmd = args[0].lowercase()

        when (subCmd) {
            "help" -> {
                sendHelp(sender, label)
            }
            "open" -> {
                if (!sender.hasPermission("bitemenus.use.open")) {
                    FormatManager.error(sender, ConfigManager.getMessage("errors.no-permission", "<#F7567C>No permission."))
                    return true
                }
                if (sender !is Player) return true
                if (args.size < 2) {
                    val msg = ConfigManager.getMessage("errors.usage-open", "<#F7567C>Usage: /<label> open <name>").replace("<label>", label)
                    FormatManager.error(sender, msg)
                    return true
                }
                val menu = MenuManager.getMenu(args[1])
                if (menu == null) {
                    FormatManager.error(sender, ConfigManager.getMessage("errors.menu-not-found", "<#F7567C>Menu not found."))
                    return true
                }
                menu.open(sender)
            }
            "list" -> {
                if (!sender.hasPermission("bitemenus.use.list")) return true
                val menus = MenuManager.getMenus().joinToString(", ") { it.name }
                val msg = ConfigManager.getMessage("messages.loaded-menus", "<#4ECDC4>Loaded menus: <#F7FFF7><menus>").replace("<menus>", menus)
                FormatManager.send(sender, msg)
            }
            "info" -> {
                if (!sender.hasPermission("bitemenus.use.info")) return true
                if (args.size < 2) {
                    val msg = ConfigManager.getMessage("errors.usage-info", "<#F7567C>Usage: /<label> info <name>").replace("<label>", label)
                    FormatManager.error(sender, msg)
                    return true
                }
                val name = args[1]
                val menu = MenuManager.getMenu(name) ?: return true.also { FormatManager.error(sender, ConfigManager.getMessage("errors.menu-not-found", "<#F7567C>Menu not found.")) }
                
                FormatManager.send(sender, ConfigManager.getMessage("info.header", "<#4ECDC4>--- Menu Info: <menu> ---").replace("<menu>", menu.name))
                FormatManager.send(sender, ConfigManager.getMessage("info.title", "<gray>Title: <#F7FFF7><title>").replace("<title>", menu.title))
                FormatManager.send(sender, ConfigManager.getMessage("info.size", "<gray>Size: <#F7FFF7><size>").replace("<size>", menu.size.toString()))
                FormatManager.send(sender, ConfigManager.getMessage("info.auto-refresh", "<gray>Auto Refresh: <#F7FFF7><value>").replace("<value>", menu.autoRefresh.toString()))
                FormatManager.send(sender, ConfigManager.getMessage("info.filler-item", "<gray>Filler Item: <#F7FFF7><value>").replace("<value>", menu.fillerItem ?: "None"))
                FormatManager.send(sender, ConfigManager.getMessage("info.permission", "<gray>Permission: <#F7FFF7><value>").replace("<value>", menu.permission ?: "None"))
                
                val totalButtons = menu.buttons.values.count { it.actions.isNotEmpty() }
                val menuLinks = menu.buttons.values.count { btn -> btn.actions.any { it.startsWith("[MENU]", ignoreCase = true) } }
                val displayStr = if (menuLinks > 0) "$totalButtons ($menuLinks Menu Links)" else totalButtons.toString()
                
                FormatManager.send(sender, ConfigManager.getMessage("info.total-buttons", "<gray>Total Buttons: <#F7FFF7><value>").replace("<value>", displayStr))

                val timezoneStr = ConfigManager.getString("timezone", "UTC")
                val zoneId = try { ZoneId.of(timezoneStr) } catch (e: Exception) { java.time.ZoneId.of("UTC") }
                val dateTime = Instant.ofEpochMilli(menu.created).atZone(zoneId)
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")
                val dateStr = dateTime.format(formatter)

                FormatManager.send(sender, ConfigManager.getMessage("info.created-date", "<gray>Created: <#F7FFF7><date>").replace("<date>", dateStr))
            }
            "create" -> {
                if (!sender.hasPermission("bitemenus.admin.create")) return true
                if (sender !is Player) {
                    FormatManager.error(sender, ConfigManager.getMessage("errors.players-only", "<#F7567C>Only players can use this command."))
                    return true
                }
                if (args.size < 3) {
                    val msg = ConfigManager.getMessage("errors.usage-create", "<#F7567C>Usage: /<label> create <name> <size> [title]").replace("<label>", label)
                    FormatManager.error(sender, msg)
                    return true
                }
                val name = args[1]
                if (!name.matches(Regex("^[a-zA-Z0-9_\\-]+$"))) {
                    FormatManager.error(sender, ConfigManager.getMessage("errors.invalid-name", "<#F7567C>Menu name can only contain alphanumeric characters, dashes, and underscores."))
                    return true
                }
                val rawSize = args[2].toIntOrNull() ?: 27
                val validSizes = listOf(1, 2, 3, 4, 5, 6, 9, 18, 27, 36, 45, 54)
                if (rawSize !in validSizes) {
                    FormatManager.error(sender, ConfigManager.getMessage("errors.invalid-size-bounds", "<#F7567C>Size must be a multiple of 9, between 9 and 54."))
                    return true
                }
                val size = if (rawSize <= 6) rawSize * 9 else rawSize
                val title = if (args.size > 3) args.drop(3).joinToString(" ") else name
                
                if (MenuManager.getMenu(name) != null) {
                    FormatManager.error(sender, ConfigManager.getMessage("errors.menu-exists", "<#F7567C>Menu already exists."))
                    return true
                }
                val menu = Menu(name, title, size)
                menu.permission = ConfigManager.getDefaultMenuPermission(name)
                menu.autoRefresh = ConfigManager.getDefaultAutoRefresh()
                menu.fillerItem = ConfigManager.getDefaultFillerItem()
                MenuManager.saveMenu(menu)
                val msg = ConfigManager.getMessage("messages.menu-created", "<#4ECDC4>Menu <menu> created.").replace("<menu>", name)
                FormatManager.send(sender, msg)
                
                EditorManager.startEditing(sender, menu)
                menu.open(sender)
            }
            "edit" -> {
                if (!sender.hasPermission("bitemenus.admin.edit")) return true
                if (sender !is Player) {
                    FormatManager.error(sender, ConfigManager.getMessage("errors.players-only", "<#F7567C>Only players can use this command."))
                    return true
                }
                if (args.size < 2) {
                    val msg = ConfigManager.getMessage("errors.usage-edit", "<#F7567C>Usage: /<label> edit <name>").replace("<label>", label)
                    FormatManager.error(sender, msg)
                    return true
                }
                val name = args[1]
                val menu = MenuManager.getMenu(name)
                if (menu == null) {
                    FormatManager.error(sender, ConfigManager.getMessage("errors.menu-not-found", "<#F7567C>Menu not found."))
                    return true
                }
                
                val msg = ConfigManager.getMessage("messages.opening-edit", "<#4ECDC4>Opening <menu> in Edit Mode.").replace("<menu>", name)
                FormatManager.send(sender, msg)
                EditorManager.startEditing(sender, menu)
                menu.open(sender)
            }
            "resize" -> {
                if (!sender.hasPermission("bitemenus.admin.resize")) return true
                if (args.size < 3) {
                    val msg = ConfigManager.getMessage("errors.usage-resize", "<#F7567C>Usage: /<label> resize <name> <size>").replace("<label>", label)
                    FormatManager.error(sender, msg)
                    return true
                }
                val name = args[1]
                val rawSize = args[2].toIntOrNull() ?: return true.also { FormatManager.error(sender, ConfigManager.getMessage("errors.invalid-size", "<#F7567C>Invalid size.")) }
                val validSizes = listOf(1, 2, 3, 4, 5, 6, 9, 18, 27, 36, 45, 54)
                if (rawSize !in validSizes) {
                    FormatManager.error(sender, ConfigManager.getMessage("errors.invalid-size-bounds", "<#F7567C>Size must be a multiple of 9, between 9 and 54."))
                    return true
                }
                val size = if (rawSize <= 6) rawSize * 9 else rawSize
                val menu = MenuManager.getMenu(name) ?: return true.also { FormatManager.error(sender, ConfigManager.getMessage("errors.menu-not-found", "<#F7567C>Menu not found.")) }
                menu.size = size
                MenuManager.saveMenu(menu)
                val msg = ConfigManager.getMessage("messages.menu-resized", "<#4ECDC4>Menu <menu> resized to <size>.").replace("<menu>", name).replace("<size>", size.toString())
                FormatManager.send(sender, msg)
            }
            "retitle" -> {
                if (!sender.hasPermission("bitemenus.admin.retitle")) return true
                if (args.size < 3) {
                    val msg = ConfigManager.getMessage("errors.usage-retitle", "<#F7567C>Usage: /<label> retitle <name> <title>").replace("<label>", label)
                    FormatManager.error(sender, msg)
                    return true
                }
                val name = args[1]
                val menu = MenuManager.getMenu(name) ?: return true.also { FormatManager.error(sender, ConfigManager.getMessage("errors.menu-not-found", "<#F7567C>Menu not found.")) }
                val newTitle = args.drop(2).joinToString(" ")
                menu.title = newTitle
                MenuManager.saveMenu(menu)
                val msg = ConfigManager.getMessage("messages.menu-retitled", "<#4ECDC4>Menu <menu> retitled to <title>.").replace("<menu>", name).replace("<title>", newTitle)
                FormatManager.send(sender, msg)
            }
            "remove" -> {
                if (!sender.hasPermission("bitemenus.admin.remove")) return true
                if (args.size < 2) {
                    val msg = ConfigManager.getMessage("errors.usage-remove", "<#F7567C>Usage: /<label> remove <name>").replace("<label>", label)
                    FormatManager.error(sender, msg)
                    return true
                }
                val name = args[1]
                if (MenuManager.getMenu(name) == null) {
                    FormatManager.error(sender, ConfigManager.getMessage("errors.menu-not-found", "<#F7567C>Menu not found."))
                    return true
                }
                MenuManager.removeMenu(name)
                val msg = ConfigManager.getMessage("messages.menu-removed", "<#4ECDC4>Menu <menu> removed.").replace("<menu>", name)
                FormatManager.send(sender, msg)
            }
            "reload" -> {
                if (!sender.hasPermission("bitemenus.admin.reload")) return true
                ConfigManager.reloadConfigs()
                CooldownManager.loadConfig()
                MenuManager.loadMenus()
                FormatManager.send(sender, ConfigManager.getMessage("messages.plugin-reloaded", "<#4ECDC4>Plugin reloaded."))
            }
            "button" -> {
                if (!sender.hasPermission("bitemenus.admin.button")) return true
                if (sender !is Player) {
                    FormatManager.error(sender, ConfigManager.getMessage("errors.players-only", "<#F7567C>Only players can use this command."))
                    return true
                }
                if (args.size < 3) {
                    val msg = ConfigManager.getMessage("errors.usage-button", "<#F7567C>Usage: /<label> button <command|menu|remove> <menu> [args...]").replace("<label>", label)
                    FormatManager.error(sender, msg)
                    return true
                }
                val actionType = args[1].lowercase()
                val menuName = args[2]
                val menu = MenuManager.getMenu(menuName) ?: return true.also { FormatManager.error(sender, ConfigManager.getMessage("errors.menu-not-found", "<#F7567C>Menu not found.")) }

                val action: (Int) -> Unit = when (actionType) {
                    "command" -> {
                        if (args.size < 5) {
                            val msg = ConfigManager.getMessage("errors.usage-button-command", "<#F7567C>Usage: /<label> button command <menu> <isPlayer> <cmd>").replace("<label>", label)
                            FormatManager.error(sender, msg)
                            return true
                        }
                        val isPlayerStr = args[3].lowercase()
                        if (isPlayerStr != "true" && isPlayerStr != "false") {
                            FormatManager.error(sender, ConfigManager.getMessage("errors.invalid-boolean", "<#F7567C>Value must be true or false."))
                            return true
                        }
                        val isPlayer = isPlayerStr.toBooleanStrict()
                        val cmd = args.drop(4).joinToString(" ")
                        val prefix = if (isPlayer) "[PLAYER]" else "[CONSOLE]"
                        { slot ->
                            val type = sender.openInventory.topInventory.getItem(slot)?.type
                            if (type == null || type == Material.AIR) {
                                FormatManager.send(sender, ConfigManager.getMessage("messages.blank-slot-warning", "<#4ECDC4>Warning: You assigned a button to a blank slot!"))
                            }
                            val button = menu.buttons.getOrPut(slot) { Button(icon = ItemStack(Material.STONE)) }
                            button.actions.add("$prefix $cmd")
                            MenuManager.saveMenu(menu)
                            val msg = ConfigManager.getMessage("messages.button-command-added", "<#4ECDC4>Added command to slot <slot> in <menu>.").replace("<slot>", slot.toString()).replace("<menu>", menu.name)
                            FormatManager.send(sender, msg)
                        }
                    }
                    "menu" -> {
                        if (args.size < 4) {
                            val msg = ConfigManager.getMessage("errors.usage-button-menu", "<#F7567C>Usage: /<label> button menu <menu> <target>").replace("<label>", label)
                            FormatManager.error(sender, msg)
                            return true
                        }
                        val target = args[3]
                        if (target.equals(menu.name, ignoreCase = true)) {
                            FormatManager.error(sender, ConfigManager.getMessage("errors.target-same-menu", "<#F7567C>Target menu cannot be the same as the current menu."))
                            return true
                        }
                        if (MenuManager.getMenu(target) == null) {
                            FormatManager.error(sender, ConfigManager.getMessage("errors.menu-not-found", "<#F7567C>Menu not found."))
                            return true
                        }
                        { slot ->
                            val type = sender.openInventory.topInventory.getItem(slot)?.type
                            if (type == null || type == Material.AIR) {
                                FormatManager.send(sender, ConfigManager.getMessage("messages.blank-slot-warning", "<#4ECDC4>Warning: You assigned a button to a blank slot!"))
                            }
                            val button = menu.buttons.getOrPut(slot) { Button(icon = ItemStack(Material.STONE)) }
                            button.actions.add("[MENU] $target")
                            MenuManager.saveMenu(menu)
                            val msg = ConfigManager.getMessage("messages.button-menu-added", "<#4ECDC4>Added menu link to slot <slot> in <menu>.").replace("<slot>", slot.toString()).replace("<menu>", menu.name)
                            FormatManager.send(sender, msg)
                        }
                    }
                    "remove" -> {
                        { slot ->
                            val button = menu.buttons[slot]
                            if (button != null) {
                                button.actions.clear()
                                button.cooldown = 0
                                button.closeOnClick = false
                                button.permission = null
                                button.cost = 0.0
                                button.visibleNoPermission = true
                                MenuManager.saveMenu(menu)
                                val msg = ConfigManager.getMessage("messages.button-removed", "<#4ECDC4>Cleared actions and flags for slot <slot> in <menu>.").replace("<slot>", slot.toString()).replace("<menu>", menu.name)
                                FormatManager.send(sender, msg)
                            } else {
                                val msg = ConfigManager.getMessage("errors.button-not-found", "<#F7567C>No button found at slot <slot>.").replace("<slot>", slot.toString())
                                FormatManager.send(sender, msg)
                            }
                        }
                    }
                    else -> return true.also { FormatManager.error(sender, ConfigManager.getMessage("errors.unknown-action", "<#F7567C>Unknown button action.")) }
                }

                ActionQueueManager.queueAction(sender, action)
                FormatManager.send(sender, ConfigManager.getMessage("messages.click-to-apply-action", "<#4ECDC4>Please click the slot you want to apply this action to!"))
                menu.open(sender)
            }
            "flag" -> {
                if (!sender.hasPermission("bitemenus.admin.flags")) return true
                if (args.size < 2) {
                    val msg = ConfigManager.getMessage("errors.usage-flag", "<#F7567C>Usage: /<label> flag list OR /<label> flag <menu> menu/button ...").replace("<label>", label)
                    FormatManager.error(sender, msg)
                    return true
                }
                if (args[1].lowercase() == "list") {
                    FormatManager.send(sender, ConfigManager.getMessage("flag-list.header", "<#4ECDC4>--- Available Flags ---"))
                    FormatManager.send(sender, ConfigManager.getMessage("flag-list.menu-header", "<#4ECDC4>Menu Flags:"))
                    FormatManager.send(sender, ConfigManager.getMessage("flag-list.menu.auto-refresh", "<gray>- auto_refresh <true/false>"))
                    FormatManager.send(sender, ConfigManager.getMessage("flag-list.menu.filler-item", "<gray>- filler_item <material/none>"))
                    FormatManager.send(sender, ConfigManager.getMessage("flag-list.menu.permission", "<gray>- permission <value/none>"))
                    FormatManager.send(sender, ConfigManager.getMessage("flag-list.button-header", "<#4ECDC4>Button Flags:"))
                    FormatManager.send(sender, ConfigManager.getMessage("flag-list.button.cooldown", "<gray>- cooldown <seconds>"))
                    FormatManager.send(sender, ConfigManager.getMessage("flag-list.button.close-on-click", "<gray>- close_on_click <true/false>"))
                    FormatManager.send(sender, ConfigManager.getMessage("flag-list.button.permission", "<gray>- permission <value/none>"))
                    FormatManager.send(sender, ConfigManager.getMessage("flag-list.button.cost", "<gray>- cost <value>"))
                    FormatManager.send(sender, ConfigManager.getMessage("flag-list.button.visible", "<gray>- visible_no_permission <true/false>"))
                    return true
                }
                
                if (args.size < 4) {
                    val msg = ConfigManager.getMessage("errors.usage-flag-general", "<#F7567C>Usage: /<label> flag <menu> menu/button <flag> <value>").replace("<label>", label)
                    FormatManager.error(sender, msg)
                    return true
                }
                val menu = MenuManager.getMenu(args[1]) ?: return true.also { FormatManager.error(sender, ConfigManager.getMessage("errors.menu-not-found", "<#F7567C>Menu not found.")) }
                val target = args[2].lowercase()
                
                if (target == "menu") {
                    val flag = args[3].lowercase()
                    val value = if (args.size > 4) args.drop(4).joinToString(" ") else "none"

                    when (flag) {
                        "auto_refresh" -> {
                            val lowerValue = value.lowercase()
                            if (lowerValue != "true" && lowerValue != "false") {
                                return true.also { FormatManager.error(sender, ConfigManager.getMessage("errors.invalid-boolean", "<#F7567C>Value must be true or false.")) }
                            }
                            menu.autoRefresh = lowerValue == "true"
                        }
                        "filler_item" -> {
                            if (value.lowercase() == "none" || value.isBlank()) {
                                menu.fillerItem = null
                            } else {
                                val mat = Material.matchMaterial(value)
                                if (mat == null) {
                                    return true.also { FormatManager.error(sender, ConfigManager.getMessage("errors.invalid-material", "<#F7567C>Invalid Material name.")) }
                                }
                                menu.fillerItem = mat.name
                            }
                        }
                        "permission" -> {
                            if (value.lowercase() == "none" || value.isBlank()) {
                                menu.permission = null
                            } else if (!value.matches(Regex("^[a-zA-Z0-9_.-]+$"))) {
                                return true.also { FormatManager.error(sender, ConfigManager.getMessage("errors.invalid-permission", "<#F7567C>Invalid permission format.")) }
                            } else {
                                menu.permission = value
                            }
                        }
                        else -> return true.also { FormatManager.error(sender, ConfigManager.getMessage("errors.unknown-menu-flag", "<#F7567C>Unknown menu flag.")) }
                    }
                    MenuManager.saveMenu(menu)
                    val msg = ConfigManager.getMessage("messages.flag-menu-set", "<#4ECDC4>Set menu flag <flag> to <value>.").replace("<flag>", flag).replace("<value>", value)
                    FormatManager.send(sender, msg)
                } else if (target == "button") {
                    if (sender !is Player) {
                        FormatManager.error(sender, ConfigManager.getMessage("errors.players-only", "<#F7567C>Only players can use this command."))
                        return true
                    }
                    val flag = args[3].lowercase()
                    val value = if (args.size > 4) args.drop(4).joinToString(" ") else "none"

                    val lowerValue = value.lowercase()
                    when (flag) {
                        "cooldown" -> {
                            if (lowerValue != "none" && lowerValue.isNotBlank()) {
                                val cd = value.toIntOrNull()
                                if (cd == null || cd < 0) {
                                    FormatManager.error(sender, ConfigManager.getMessage("errors.invalid-number", "<#F7567C>Value must be a valid number."))
                                    return true
                                }
                            }
                        }
                        "close_on_click", "visible_no_permission" -> {
                            if (lowerValue != "true" && lowerValue != "false") {
                                FormatManager.error(sender, ConfigManager.getMessage("errors.invalid-boolean", "<#F7567C>Value must be true or false."))
                                return true
                            }
                        }
                        "permission" -> {
                            if (lowerValue != "none" && lowerValue.isNotBlank()) {
                                if (!value.matches(Regex("^[a-zA-Z0-9_.-]+$"))) {
                                    FormatManager.error(sender, ConfigManager.getMessage("errors.invalid-permission", "<#F7567C>Invalid permission format."))
                                    return true
                                }
                            }
                        }
                        "cost" -> {
                            if (lowerValue != "none" && lowerValue.isNotBlank()) {
                                val cst = value.toDoubleOrNull()
                                if (cst == null || cst < 0) {
                                    FormatManager.error(sender, ConfigManager.getMessage("errors.invalid-number", "<#F7567C>Value must be a valid number."))
                                    return true
                                }
                            }
                        }
                        else -> {
                            FormatManager.error(sender, ConfigManager.getMessage("errors.unknown-button-flag", "<#F7567C>Unknown button flag."))
                            return true
                        }
                    }

                    val action: (Int) -> Unit = { slot ->
                        val button = menu.buttons[slot]
                        if (button == null) {
                            val msg = ConfigManager.getMessage("errors.slot-no-button", "<#F7567C>This slot does not have a button! Please add one using <#4ECDC4>/<label> button command <gray>or <#4ECDC4>/<label> button menu<#F7567C>.").replace("<label>", label)
                            FormatManager.error(sender, msg)
                        } else {
                            val lowerVal = value.lowercase()
                            when (flag) {
                                "cooldown" -> button.cooldown = if (lowerVal == "none" || lowerVal.isBlank()) 0 else value.toInt()
                                "close_on_click" -> button.closeOnClick = lowerVal == "true"
                                "permission" -> button.permission = if (lowerVal == "none" || lowerVal.isBlank()) null else value
                                "cost" -> button.cost = if (lowerVal == "none" || lowerVal.isBlank()) 0.0 else value.toDouble()
                                "visible_no_permission" -> button.visibleNoPermission = lowerVal == "true"
                            }
                            MenuManager.saveMenu(menu)
                            val msg = ConfigManager.getMessage("messages.flag-button-set", "<#4ECDC4>Set button flag <flag> to <value> for slot <slot>.")
                                .replace("<flag>", flag).replace("<value>", value).replace("<slot>", slot.toString())
                            FormatManager.send(sender, msg)
                        }
                    }

                    ActionQueueManager.queueAction(sender, action)
                    FormatManager.send(sender, ConfigManager.getMessage("messages.click-to-apply-flag", "<#4ECDC4>Please click the slot you want to apply this flag to!"))
                    menu.open(sender)
                } else {
                    FormatManager.error(sender, ConfigManager.getMessage("errors.invalid-flag-target", "<#F7567C>Target must be 'menu' or 'button'."))
                }
            }
            else -> {
                FormatManager.error(sender, ConfigManager.getMessage("errors.unknown-command", "<#F7567C>Unknown command."))
            }
        }
        return true
    }

    private fun sendHelp(sender: CommandSender, label: String): Boolean {
        if (sender.hasPermission("bitemenus.use.help")) {
            FormatManager.send(sender, ConfigManager.getMessage("help.header", "<#4ECDC4>--- BiteMenus Help ---"))
            FormatManager.send(sender, ConfigManager.getMessage("help.open", "<gray>- /<label> open <name>").replace("<label>", label), usePrefix = false)
            FormatManager.send(sender, ConfigManager.getMessage("help.list", "<gray>- /<label> list").replace("<label>", label), usePrefix = false)
            FormatManager.send(sender, ConfigManager.getMessage("help.info", "<gray>- /<label> info <name>").replace("<label>", label), usePrefix = false)
            if (sender.hasPermission("bitemenus.admin.create")) {
                FormatManager.send(sender, ConfigManager.getMessage("help.create", "<gray>- /<label> create <name> <size> [title]").replace("<label>", label), usePrefix = false)
                FormatManager.send(sender, ConfigManager.getMessage("help.edit", "<gray>- /<label> edit <name>").replace("<label>", label), usePrefix = false)
                FormatManager.send(sender, ConfigManager.getMessage("help.resize", "<gray>- /<label> resize <name> <size>").replace("<label>", label), usePrefix = false)
                FormatManager.send(sender, ConfigManager.getMessage("help.retitle", "<gray>- /<label> retitle <name> <title>").replace("<label>", label), usePrefix = false)
                FormatManager.send(sender, ConfigManager.getMessage("help.remove", "<gray>- /<label> remove <name>").replace("<label>", label), usePrefix = false)
                FormatManager.send(sender, ConfigManager.getMessage("help.reload", "<gray>- /<label> reload").replace("<label>", label), usePrefix = false)
                FormatManager.send(sender, ConfigManager.getMessage("help.button-command", "<gray>- /<label> button command <menu> <isPlayer> <cmd>").replace("<label>", label), usePrefix = false)
                FormatManager.send(sender, ConfigManager.getMessage("help.button-menu", "<gray>- /<label> button menu <menu> <target>").replace("<label>", label), usePrefix = false)
                FormatManager.send(sender, ConfigManager.getMessage("help.button-remove", "<gray>- /<label> button remove <menu>").replace("<label>", label), usePrefix = false)
                FormatManager.send(sender, ConfigManager.getMessage("help.flag-list", "<gray>- /<label> flag list").replace("<label>", label), usePrefix = false)
                FormatManager.send(sender, ConfigManager.getMessage("help.flag-menu", "<gray>- /<label> flag <menu> menu <flag> <value>").replace("<label>", label), usePrefix = false)
                FormatManager.send(sender, ConfigManager.getMessage("help.flag-button", "<gray>- /<label> flag <menu> button <flag> <value>").replace("<label>", label), usePrefix = false)
            }
        } else {
            FormatManager.error(sender, ConfigManager.getMessage("errors.no-permission", "<#F7567C>No permission."))
        }
        return true
    }

    override fun tabComplete(sender: CommandSender, alias: String, args: Array<String>): List<String> {
        val completions = mutableListOf<String>()
        if (args.size == 1) {
            val subs = mutableListOf("help", "open", "list", "info")
            if (sender.hasPermission("bitemenus.admin.create")) {
                subs.addAll(listOf("create", "edit", "remove", "reload", "button", "flag", "resize", "retitle"))
            }
            completions.addAll(subs.filter { it.startsWith(args[0], true) })
        } else if (args.size == 2) {
            val sub = args[0].lowercase()
            if (sub in listOf("open", "edit", "remove", "info", "resize", "retitle", "flag")) {
                completions.addAll(MenuManager.getMenus().map { it.name }.filter { it.startsWith(args[1], true) })
                if (sub == "flag") {
                    completions.addAll(listOf("list").filter { it.startsWith(args[1], true) })
                }
            } else if (sub == "button") {
                completions.addAll(listOf("command", "menu", "remove").filter { it.startsWith(args[1], true) })
            }
        } else if (args.size == 3) {
            val sub = args[0].lowercase()
            if (sub == "create" || sub == "resize") {
                val sizes = listOf("1", "2", "3", "4", "5", "6", "9", "18", "27", "36", "45", "54")
                completions.addAll(sizes.filter { it.startsWith(args[2], true) })
            } else if (sub == "flag" && args[1].lowercase() != "list") {
                completions.addAll(listOf("menu", "button").filter { it.startsWith(args[2], true) })
            } else if (sub == "button") {
                completions.addAll(MenuManager.getMenus().map { it.name }.filter { it.startsWith(args[2], true) })
            }
        } else if (args.size == 4) {
            val sub = args[0].lowercase()
            if (sub == "flag" && args[2].lowercase() == "menu") {
                val flags = listOf("auto_refresh", "filler_item", "permission")
                completions.addAll(flags.filter { it.startsWith(args[3], true) })
            } else if (sub == "flag" && args[2].lowercase() == "button") {
                val flags = listOf("cooldown", "close_on_click", "permission", "cost", "visible_no_permission")
                completions.addAll(flags.filter { it.startsWith(args[3], true) })
            } else if (sub == "button" && args[1].lowercase() == "command") {
                completions.addAll(listOf("true", "false").filter { it.startsWith(args[3], true) })
            } else if (sub == "button" && args[1].lowercase() == "menu") {
                completions.addAll(MenuManager.getMenus().map { it.name }.filter { it.startsWith(args[3], true) })
            }
        } else if (args.size == 5) {
            val sub = args[0].lowercase()
            if (sub == "flag") {
                val flag = args[3].lowercase()
                if (args[2].lowercase() == "menu" && flag == "filler_item") {
                    completions.addAll(Material.values().map { it.name }.filter { it.startsWith(args[4], true) })
                } else if (flag == "auto_refresh" || flag == "close_on_click" || flag == "visible_no_permission") {
                    completions.addAll(listOf("true", "false").filter { it.startsWith(args[4], true) })
                }
            }
        }
        return completions
    }
}
