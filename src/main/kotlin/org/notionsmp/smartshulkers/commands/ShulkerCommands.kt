package org.notionsmp.smartshulkers.commands

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.entity.Player
import org.notionsmp.smartshulkers.SmartShulkers

@CommandAlias("smartshulkers|sshulk")
@CommandPermission("smartshulkers.admin")
class ShulkerCommands(private val plugin: SmartShulkers) : BaseCommand() {

    private val mm = MiniMessage.miniMessage()

    @Subcommand("reload")
    @Description("Reloads the plugin configuration")
    fun onReload(sender: Player) {
        plugin.reload()
        sender.sendMessage(mm.deserialize("<green>SmartShulkers config reloaded!"))
    }
}