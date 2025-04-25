package org.notionsmp.smartshulkers.utils

import org.bukkit.SoundCategory
import org.bukkit.entity.Player
import org.notionsmp.smartshulkers.SmartShulkers

object SoundManager {
    fun playSound(player: Player, soundPath: String) {
        val soundSettings = SmartShulkers.instance.configManager.getSoundSettings()
        SmartShulkers.instance.configManager.getString(soundPath)?.let { soundName ->
            try {
                val soundCategory = SoundCategory.valueOf(soundSettings.source)
                player.playSound(
                    player.location,
                    soundName,
                    soundCategory,
                    soundSettings.volume,
                    soundSettings.pitch
                )
            } catch (e: IllegalArgumentException) {
                player.playSound(
                    player.location,
                    soundName,
                    SoundCategory.VOICE,
                    soundSettings.volume,
                    soundSettings.pitch
                )
            }
        }
    }
}