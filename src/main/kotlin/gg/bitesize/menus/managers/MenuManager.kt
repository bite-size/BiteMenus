package gg.bitesize.menus.managers

import gg.bitesize.menus.BiteMenus.Companion.INSTANCE
import gg.bitesize.menus.models.Button
import gg.bitesize.menus.models.Menu
import gg.bitesize.menus.models.MenuHolder
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.ItemStack
import java.io.File

object MenuManager {

    private var menus = mutableMapOf<String, Menu>()
    // Menus whose file failed to parse. Saving them would overwrite the owner's broken-but-unsaved edits.
    private val unreadable = mutableSetOf<String>()
    private lateinit var menusFolder: File

    /** Loads every menu file and returns the names of files that failed to parse. */
    fun loadMenus(): List<String> {
        menusFolder = File(INSTANCE.dataFolder, "menus")
        if (!menusFolder.exists()) {
            menusFolder.mkdirs()
            createExampleMenu()
        }

        val loaded = mutableMapOf<String, Menu>()
        val failed = mutableListOf<String>()
        unreadable.clear()

        menusFolder.listFiles()?.filter { it.extension == "yml" }?.forEach { file ->
            val name = file.nameWithoutExtension
            try {
                // load() throws on bad YAML, where loadConfiguration() would hand back an empty config
                val config = YamlConfiguration().apply { load(file) }
                loaded[name.lowercase()] = parseMenu(name, config)
                INSTANCE.logger.info("Loaded menu: $name")
            } catch (e: Exception) {
                failed.add(file.name)
                unreadable.add(name.lowercase())
                INSTANCE.logger.severe("Failed to load menu: ${file.name} (${e.message}). The file was left untouched.")
                menus[name.lowercase()]?.let {
                    loaded[name.lowercase()] = it
                    INSTANCE.logger.warning("Keeping the previously loaded version of $name until the file is fixed.")
                }
            }
        }

        menus = loaded
        return failed
    }

    private fun parseMenu(name: String, config: YamlConfiguration): Menu {
        val menu = Menu(
            name = name,
            title = config.getString("title", "Menu")!!,
            size = config.getInt("size", 27),
            created = config.getLong("created", System.currentTimeMillis())
        ).apply {
            autoRefresh = config.getBoolean("flags.auto_refresh", true)
            fillerItem = config.getString("flags.filler_item")
            permission = config.getString("flags.permission")
        }

        config.getConfigurationSection("buttons")?.getKeys(false)?.forEach { slotKey ->
            val slot = slotKey.toIntOrNull() ?: return@forEach
            val buttonSection = config.getConfigurationSection("buttons.$slotKey")!!

            var icon = buttonSection.getItemStack("icon")
            if (icon == null && buttonSection.isConfigurationSection("icon")) {
                val iconSec = buttonSection.getConfigurationSection("icon")!!
                val mat = Material.matchMaterial(iconSec.getString("material", "STONE")!!) ?: Material.STONE
                icon = buildSimpleIcon(mat, iconSec.getString("name"), iconSec.getStringList("lore"))
            }

            menu.buttons[slot] = Button(
                icon = icon,
                actions = buttonSection.getStringList("actions"),
                cooldown = buttonSection.getInt("flags.cooldown", 0),
                closeOnClick = buttonSection.getBoolean("flags.close_on_click", false),
                permission = buttonSection.getString("flags.permission"),
                cost = buttonSection.getDouble("flags.cost", 0.0),
                visibleNoPermission = buttonSection.getBoolean("flags.visible_no_permission", true)
            )
        }
        return menu
    }

    /** Builds an icon from the short, hand-editable form, which only holds material, name and lore. */
    private fun buildSimpleIcon(material: Material, name: String?, lore: List<String>): ItemStack {
        val icon = ItemStack(material)
        val meta = icon.itemMeta ?: return icon
        name?.let { meta.setDisplayName(it) }
        if (lore.isNotEmpty()) meta.lore = lore
        icon.itemMeta = meta
        return icon
    }

    /**
     * True only when the short form rebuilds this exact item. Anything it can't hold (custom model data,
     * item model, PDC, enchants, stack size, ...) makes the rebuilt item differ, so it gets saved in full.
     */
    private fun fitsSimpleForm(icon: ItemStack): Boolean {
        val meta = icon.itemMeta
        val name = if (meta != null && meta.hasDisplayName()) meta.displayName else null
        val lore = if (meta != null && meta.hasLore()) meta.lore.orEmpty() else emptyList()
        return icon.amount == 1 && buildSimpleIcon(icon.type, name, lore).isSimilar(icon)
    }

    fun isUnreadable(name: String): Boolean = name.lowercase() in unreadable

    fun saveMenu(menu: Menu) {
        if (isUnreadable(menu.name)) {
            INSTANCE.logger.warning("Refusing to save ${menu.name}: its file failed to load. Fix it and run /bm reload.")
            return
        }
        val file = File(menusFolder, "${menu.name}.yml")
        val config = YamlConfiguration()

        config.set("title", menu.title)
        config.set("size", menu.size)
        config.set("created", menu.created)

        config.set("flags.auto_refresh", menu.autoRefresh)
        if (menu.fillerItem != null) config.set("flags.filler_item", menu.fillerItem)
        if (menu.permission != null) config.set("flags.permission", menu.permission)

        menu.buttons.forEach { slot, button ->
            val path = "buttons.$slot"
            button.icon?.let { icon ->
                if (fitsSimpleForm(icon)) {
                    val meta = icon.itemMeta
                    config.set("$path.icon.material", icon.type.name)
                    if (meta != null) {
                        if (meta.hasDisplayName()) config.set("$path.icon.name", meta.displayName)
                        if (meta.hasLore()) config.set("$path.icon.lore", meta.lore)
                    }
                } else {
                    config.set("$path.icon", icon)
                }
            }
            config.set("$path.actions", button.actions)
            config.set("$path.flags.cooldown", button.cooldown)
            config.set("$path.flags.close_on_click", button.closeOnClick)
            if (button.permission != null) config.set("$path.flags.permission", button.permission)
            config.set("$path.flags.cost", button.cost)
            config.set("$path.flags.visible_no_permission", button.visibleNoPermission)
        }

        config.save(file)
        menus[menu.name.lowercase()] = menu
    }

    fun removeMenu(name: String) {
        val file = File(menusFolder, "$name.yml")
        if (file.exists()) {
            file.delete()
        }
        menus.remove(name.lowercase())
        unreadable.remove(name.lowercase())
    }

    fun getMenu(name: String): Menu? {
        return menus[name.lowercase()]
    }

    fun getMenus(): Collection<Menu> {
        return menus.values
    }

    /** Closes every open BiteMenus inventory so nobody keeps clicking a menu that is about to be replaced. */
    fun closeOpenMenus() {
        Bukkit.getOnlinePlayers().forEach { player ->
            val holder = try { player.openInventory.topInventory.holder } catch (e: Throwable) { null }
            if (holder is MenuHolder) player.closeInventory()
        }
    }

    private fun createExampleMenu() {
        val example = Menu("example", "<rainbow>Example Menu", 27)
        example.fillerItem = ConfigManager.getDefaultFillerItem()
        example.permission = ConfigManager.getDefaultMenuPermission("example")
        example.autoRefresh = ConfigManager.getDefaultAutoRefresh()

        val button = Button(
            icon = ItemStack(Material.DIAMOND),
            actions = mutableListOf("[MESSAGE] <#4ECDC4>You clicked the diamond!")
        )
        val meta = button.icon!!.itemMeta!!
        meta.setDisplayName(FormatManager.parseMiniMessage("<#FF0368><bold>Free Diamond!"))
        button.icon!!.itemMeta = meta

        example.buttons[13] = button
        saveMenu(example)
    }
}
