package org.notionsmp.smartshulkers.utils

import org.bukkit.SoundCategory
import org.bukkit.entity.Player
import org.notionsmp.smartshulkers.SmartShulkers

object SoundManager {
    fun playSound(player: Player, soundPath: String) {
        val soundSettings = SmartShulkers.instance.configManager.getSoundSettings()
        SmartShulkers.instance.configManager.getString(soundPath)?.let { soundName ->
            player.playSound(
                player.location,
                soundName,
                runCatching { SoundCategory.valueOf(soundSettings.source)}.getOrDefault(SoundCategory.VOICE),
                soundSettings.volume,
                soundSettings.pitch
            )
        }
    }
}