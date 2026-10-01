package gg.bitesize.menus.managers

import org.bukkit.command.CommandSender
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

object FormatManager {

    private val miniMessage = MiniMessage.miniMessage()
    private val hexRegex = Regex("&#([A-Fa-f0-9]{6})", RegexOption.IGNORE_CASE)
    private val legacySpigotHexRegex = Regex("&x&([A-Fa-f0-9])&([A-Fa-f0-9])&([A-Fa-f0-9])&([A-Fa-f0-9])&([A-Fa-f0-9])&([A-Fa-f0-9])", RegexOption.IGNORE_CASE)
    private val legacySerializer = LegacyComponentSerializer.builder()
        .character('§')
        .hexColors()
        .useUnusualXRepeatedCharacterHexFormat()
        .build()

    fun parseMiniMessage(text: String, vararg placeholders: TagResolver): String {
        val processedText = text
            .replace("§", "&")
            .replace(legacySpigotHexRegex, "<#$1$2$3$4$5$6>")
            .replace(hexRegex, "<#$1>")
            .replace("&0", "<black>")
            .replace("&1", "<dark_blue>")
            .replace("&2", "<dark_green>")
            .replace("&3", "<dark_aqua>")
            .replace("&4", "<dark_red>")
            .replace("&5", "<dark_purple>")
            .replace("&6", "<gold>")
            .replace("&7", "<gray>")
            .replace("&8", "<dark_gray>")
            .replace("&9", "<blue>")
            .replace("&a", "<green>", ignoreCase = true)
            .replace("&b", "<aqua>", ignoreCase = true)
            .replace("&c", "<red>", ignoreCase = true)
            .replace("&d", "<light_purple>", ignoreCase = true)
            .replace("&e", "<yellow>", ignoreCase = true)
            .replace("&f", "<white>", ignoreCase = true)
            .replace("&l", "<bold>", ignoreCase = true)
            .replace("&m", "<strikethrough>", ignoreCase = true)
            .replace("&n", "<underlined>", ignoreCase = true)
            .replace("&o", "<italic>", ignoreCase = true)
            .replace("&r", "<reset>", ignoreCase = true)
            
        val component = miniMessage.deserialize(processedText, *placeholders)
        return legacySerializer.serialize(component)
    }

    fun getPrefix(): String {
        return parseMiniMessage(ConfigManager.getPrefix())
    }

    fun send(sender: CommandSender, text: String, usePrefix: Boolean = true,
             vararg placeholders: TagResolver) {

        var message = parseMiniMessage(text, *placeholders)

        if (usePrefix) {
            message = getPrefix() + message
        }

        sender.sendMessage(message)

    }

    fun error(sender: CommandSender, text: String, vararg placeholders: TagResolver) {
        send(sender, ConfigManager.getErrorPrefix() + text, true, *placeholders)
    }

}