package gg.bitesize.menus.commands

import org.bukkit.command.Command
import org.bukkit.command.CommandSender

abstract class BaseCommand(
    name: String,
    description: String = "",
    usage: String = "/$name",
    aliases: List<String> = emptyList(),
): Command(name, description, usage, aliases) {

    abstract override fun execute(sender: CommandSender, label: String, args: Array<String>): Boolean

    override fun tabComplete(sender: CommandSender, alias: String, args: Array<String>): List<String> {
        return emptyList()
    }

}