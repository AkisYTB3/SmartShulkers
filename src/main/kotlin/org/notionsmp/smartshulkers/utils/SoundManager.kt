package org.notionsmp.smartshulkers

import org.bukkit.entity.Player
import org.notionsmp.smartshulkers.config.ConfigManager

object SoundManager {
    fun playSound(player: Player, soundPath: String) {
        SmartShulkers.instance.configManager.getString(soundPath)?.let { soundName ->
            player.playSound(player.location, soundName, 1.0f, 1.0f)
        }
    }
}