package gg.bitesize.menus.managers

import gg.bitesize.menus.BiteMenus.Companion.INSTANCE
import gg.bitesize.menus.commands.BaseCommand
import org.bukkit.Bukkit

object CommandManager {

    private val commandMap: org.bukkit.command.CommandMap = try {
        val method = Bukkit.getServer().javaClass.getDeclaredMethod("getCommandMap")
        method.invoke(Bukkit.getServer()) as org.bukkit.command.CommandMap
    } catch (e: Exception) {
        val field = Bukkit.getServer().javaClass.getDeclaredField("commandMap")
        field.isAccessible = true
        field.get(Bukkit.getServer()) as org.bukkit.command.CommandMap
    }
    
    private val knownCommands: MutableMap<String, org.bukkit.command.Command> = try {
        val method = commandMap.javaClass.getDeclaredMethod("getKnownCommands")
        @Suppress("UNCHECKED_CAST")
        method.invoke(commandMap) as MutableMap<String, org.bukkit.command.Command>
    } catch (e: Exception) {
        val field = commandMap.javaClass.getDeclaredField("knownCommands")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        field.get(commandMap) as MutableMap<String, org.bukkit.command.Command>
    }
    
    private val registeredCommands = mutableSetOf<BaseCommand>()

    fun registerAll() {
        register(gg.bitesize.menus.commands.BiteMenusCommand())
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