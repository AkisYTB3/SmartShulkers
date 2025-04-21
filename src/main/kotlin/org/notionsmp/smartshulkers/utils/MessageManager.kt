package org.notionsmp.smartshulkers

import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.entity.Player

object MessageManager {
    private val mm = MiniMessage.miniMessage()

    fun sendMessage(player: Player, messagePath: String, vararg replacements: Pair<String, String>) {
        val config = SmartShulkers.instance.configManager
        val messageType = config.getString("messages.type") ?: "ACTIONBAR"
        val rawMessage = config.getString(messagePath) ?: return

        var message = rawMessage
        replacements.forEach { (key, value) ->
            message = message.replace("<$key>", value)
        }

        when (messageType.uppercase()) {
            "CHAT" -> player.sendMessage(mm.deserialize(message))
            else -> player.sendActionBar(mm.deserialize(message))
        }
    }
}