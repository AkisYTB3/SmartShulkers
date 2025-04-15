package org.notionsmp.autoshulker.commands

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.entity.Player
import org.notionsmp.autoshulker.AutoShulker

@CommandAlias("autoshulker|ash")
@CommandPermission("autoshulker.admin")
class ShulkerCommands(private val plugin: AutoShulker) : BaseCommand() {

    private val mm = MiniMessage.miniMessage()

    @Subcommand("reload")
    @Description("Reloads the plugin configuration")
    fun onReload(sender: Player) {
        plugin.reload()
        sender.sendMessage(mm.deserialize("<green>AutoShulker config reloaded!"))
    }
}