package gg.bitesize.menus.managers

import gg.bitesize.menus.BiteMenus.Companion.INSTANCE
import gg.bitesize.menus.commands.BaseCommand
import gg.bitesize.menus.commands.BiteMenusCommand
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandMap

object CommandManager {

    private val commandMap: CommandMap = try {
        val method = Bukkit.getServer().javaClass.getDeclaredMethod("getCommandMap")
        method.invoke(Bukkit.getServer()) as CommandMap
    } catch (e: Exception) {
        val field = Bukkit.getServer().javaClass.getDeclaredField("commandMap")
        field.isAccessible = true
        field.get(Bukkit.getServer()) as CommandMap
    }
    
    private val knownCommands: MutableMap<String, Command> = try {
        val method = commandMap.javaClass.getDeclaredMethod("getKnownCommands")
        @Suppress("UNCHECKED_CAST")
        method.invoke(commandMap) as MutableMap<String, Command>
    } catch (e: Exception) {
        val field = commandMap.javaClass.getDeclaredField("knownCommands")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        field.get(commandMap) as MutableMap<String, Command>
    }
    
    private val registeredCommands = mutableSetOf<BaseCommand>()

    fun registerAll() {
        register(BiteMenusCommand())
    }

    fun register(command: BaseCommand) {
        commandMap.register(command.name.lowercase(), command)
        registeredCommands.add(command)
    }



    fun syncCommands() {
        Bukkit.getServer().onlinePlayers.forEach { player ->
            player.updateCommands()
        }
    }

    fun shutdown() {
        registeredCommands.forEach { command ->
            command.unregister(commandMap)

            knownCommands.remove(command.name)
            knownCommands.remove("${INSTANCE.name.lowercase()}:${command.name}")
            command.aliases.forEach { alias ->
                knownCommands.remove(alias)
                knownCommands.remove("${INSTANCE.name.lowercase()}:$alias")
            }
        }

        registeredCommands.clear()

        INSTANCE.logger.info("Unregistered commands.")
    }

}