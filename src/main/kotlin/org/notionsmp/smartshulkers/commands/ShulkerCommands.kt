package org.notionsmp.smartshulkers.commands

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.block.ShulkerBox
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.BlockStateMeta
import org.notionsmp.smartshulkers.MessageManager
import org.notionsmp.smartshulkers.SmartShulkers
import org.notionsmp.smartshulkers.SoundManager
import org.notionsmp.smartshulkers.utils.ShulkerManager

@CommandAlias("smartshulkers|ss")
class ShulkerCommands(private val plugin: SmartShulkers) : BaseCommand() {
    private val mm = MiniMessage.miniMessage()

    @Subcommand("reload")
    @CommandPermission("smartshulkers.reload")
    fun onReload(player: Player) {
        plugin.reload()
        player.sendMessage(mm.deserialize("<green>SmartShulkers config reloaded!"))
    }

    @Subcommand("sellall")
    @CommandPermission("smartshulkers.sell.command")
    fun onSellAll(player: Player) {
        if (plugin.economy == null || !plugin.configManager.isSellShulkerEnabled) {
            player.sendMessage(mm.deserialize("<red>Sell shulkers are disabled!"))
            return
        }

        var totalEarned = 0.0
        player.inventory.contents?.forEachIndexed { index, item ->
            if ((item != null) && ShulkerManager.isSellShulker(item)) {
                (item.itemMeta as? BlockStateMeta)?.let { meta ->
                    (meta.blockState as? ShulkerBox)?.let { shulkerBox ->
                        shulkerBox.inventory.contents.forEachIndexed { slot, stack ->
                            if (stack != null) {
                                val price = plugin.configManager.getPrice(stack.type.name)
                                if (price > 0.0) {
                                    val itemValue = price * stack.amount
                                    plugin.economy?.depositPlayer(player, itemValue)
                                    totalEarned += itemValue
                                    shulkerBox.inventory.setItem(slot, null)
                                }
                            }
                        }
                        meta.blockState = shulkerBox
                        item.itemMeta = meta
                        player.inventory.setItem(index, item)
                    }
                }
            }
        }

        if (totalEarned > 0.0) {
            SoundManager.playSound(player, "sounds.sell")
            player.sendMessage(mm.deserialize("<green>Sold all items for %.2f".format(totalEarned)))
        } else {
            player.sendMessage(mm.deserialize("<red>No items to sell!"))
        }
    }
}