package gg.bitesize.menus.managers

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import gg.bitesize.menus.BiteMenus.Companion.INSTANCE
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.File
import java.io.IOException
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.math.max

object CooldownManager {

    private val cache: Cache<String, Long> = CacheBuilder.newBuilder()
        .expireAfterWrite(60, TimeUnit.MINUTES)
        .build()

    private lateinit var file: File
    private lateinit var config: YamlConfiguration

    fun loadConfig() {
        file = File(INSTANCE.dataFolder, "cooldowns.yml")
        if(!file.exists()) {
            file.parentFile.mkdirs()
            file.createNewFile()
        }
        config = YamlConfiguration.loadConfiguration(file)
    }

    fun setCooldown(player: Player, cooldownName: String, duration: Long, unit: TimeUnit, persistent: Boolean = false) {
        val expiresAt = System.currentTimeMillis() + unit.toMillis(duration)

        if (persistent) {
            config.set("${player.uniqueId}.$cooldownName", expiresAt)
            savePersistent()
        } else {
            val key = "${player.uniqueId}_$cooldownName"
            cache.put(key, expiresAt)
        }
    }

    fun isOnCooldown(player: Player, cooldownName: String, isPersistent: Boolean = false): Boolean {
        val expiresAt = if (isPersistent) {
            config.getLong("${player.uniqueId}.$cooldownName", 0)
        } else {
            cache.getIfPresent("${player.uniqueId}_$cooldownName") ?: 0
        }

        return expiresAt > System.currentTimeMillis()
    }

    fun getRemainingFormatted(player: Player, cooldownName: String, isPersistent: Boolean = false): String {
        val expiresAt = if (isPersistent) {
            config.getLong("${player.uniqueId}.$cooldownName", 0)
        } else {
            cache.getIfPresent("${player.uniqueId}_$cooldownName") ?: 0
        }

        val millisLeft = max(0, expiresAt - System.currentTimeMillis())
        return formatTime(millisLeft)
    }

    private fun formatTime(millis: Long): String {
        if (millis <= 0) return "0s"

        val days = TimeUnit.MILLISECONDS.toDays(millis)
        val hours = TimeUnit.MILLISECONDS.toHours(millis) % 24
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60

        val parts = mutableListOf<String>()
        if (days > 0) parts.add("${days}d")
        if (hours > 0) parts.add("${hours}h")
        if (minutes > 0) parts.add("${minutes}m")
        if (seconds > 0) parts.add("${seconds}s")

        return parts.joinToString(" ")
    }

    private fun savePersistent() {
        try {
            config.save(file)
        } catch (e: IOException) {
            INSTANCE.logger.severe("Could not save cooldowns.yml!")
            e.printStackTrace()
        }
    }

    fun shutdown() {
        cache.invalidateAll()
        cache.cleanUp()
        savePersistent()
    }

}