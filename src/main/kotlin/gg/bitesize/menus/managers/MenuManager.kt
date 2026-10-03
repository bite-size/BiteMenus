package gg.bitesize.menus.managers

import gg.bitesize.menus.BiteMenus
import gg.bitesize.menus.BiteMenus.Companion.INSTANCE
import gg.bitesize.menus.models.Button
import gg.bitesize.menus.models.Menu
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.ItemStack
import java.io.File

object MenuManager {

    private val menus = mutableMapOf<String, Menu>()
    private lateinit var menusFolder: File

    fun loadMenus() {
        menus.clear()
        menusFolder = File(INSTANCE.dataFolder, "menus")
        if (!menusFolder.exists()) {
            menusFolder.mkdirs()
            createExampleMenu()
        }

        menusFolder.listFiles()?.filter { it.extension == "yml" }?.forEach { file ->
            try {
                val config = YamlConfiguration.loadConfiguration(file)
                val name = file.nameWithoutExtension
                val title = config.getString("title", "Menu")!!
                val size = config.getInt("size", 27)
                
                val menu = Menu(
                    name = name,
                    title = title,
                    size = size,
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
                        val matName = iconSec.getString("material", "STONE")!!
                        val mat = org.bukkit.Material.matchMaterial(matName) ?: org.bukkit.Material.STONE
                        icon = org.bukkit.inventory.ItemStack(mat)
                        val meta = icon.itemMeta
                        if (meta != null) {
                            iconSec.getString("name")?.let { meta.setDisplayName(it) }
                            val lore = iconSec.getStringList("lore")
                            if (lore.isNotEmpty()) meta.lore = lore
                            icon.itemMeta = meta
                        }
                    }
                    val actions = buttonSection.getStringList("actions")
                    
                    val button = Button(
                        icon = icon,
                        actions = actions,
                        cooldown = buttonSection.getInt("flags.cooldown", 0),
                        closeOnClick = buttonSection.getBoolean("flags.close_on_click", false),
                        permission = buttonSection.getString("flags.permission"),
                        cost = buttonSection.getDouble("flags.cost", 0.0),
                        visibleNoPermission = buttonSection.getBoolean("flags.visible_no_permission", true)
                    )
                    
                    menu.buttons[slot] = button
                }
                
                menus[name.lowercase()] = menu
                INSTANCE.logger.info("Loaded menu: $name")
            } catch (e: Exception) {
                INSTANCE.logger.severe("Failed to load menu: ${file.name}")
                e.printStackTrace()
            }
        }
    }

    fun saveMenu(menu: Menu) {
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
            if (button.icon != null) {
                val meta = button.icon!!.itemMeta
                val hasComplexMeta = meta != null && (meta.hasEnchants() || meta.itemFlags.isNotEmpty() || meta.isUnbreakable || meta.hasAttributeModifiers() || meta.javaClass.simpleName != "CraftMetaItem")
                
                if (hasComplexMeta) {
                    config.set("$path.icon", button.icon)
                } else {
                    config.set("$path.icon.material", button.icon!!.type.name)
                    if (meta != null) {
                        if (meta.hasDisplayName()) config.set("$path.icon.name", meta.displayName)
                        if (meta.hasLore()) config.set("$path.icon.lore", meta.lore)
                    }
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
    }

    fun getMenu(name: String): Menu? {
        return menus[name.lowercase()]
    }

    fun getMenus(): Collection<Menu> {
        return menus.values
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
