package org.notionsmp.smartshulkers.listeners

import org.bukkit.block.ShulkerBox
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BlockStateMeta
import org.notionsmp.smartshulkers.MessageManager
import org.notionsmp.smartshulkers.SmartShulkers
import org.notionsmp.smartshulkers.SoundManager
import org.notionsmp.smartshulkers.utils.ShulkerManager

class SellListener(private val plugin: SmartShulkers) : Listener {
    @EventHandler
    fun onItemPickup(event: EntityPickupItemEvent) {
        if (plugin.economy == null) return
        if (event.entity !is Player) return
        val player = event.entity as Player
        val item = event.item.itemStack

        player.inventory.contents?.forEachIndexed { index, inventoryItem ->
            if (inventoryItem != null &&
                ShulkerManager.isSellShulker(inventoryItem) &&
                ShulkerManager.getShulkerItems(inventoryItem).contains(item.type)) {

                if (!plugin.configManager.isSellShulkerEnabled) return

                handleSellPickup(event, player, inventoryItem, index, item)
                return
            }
        }
    }

    private fun handleSellPickup(
        event: EntityPickupItemEvent,
        player: Player,
        shulker: ItemStack,
        inventoryIndex: Int,
        item: ItemStack
    ) {
        val price = plugin.configManager.getPrice(item.type.name)
        if (price <= 0.0) return

        val meta = shulker.itemMeta as? BlockStateMeta ?: return
        val shulkerBox = meta.blockState as? ShulkerBox ?: return
        val sellWhen = plugin.configManager.getString("settings.sellshulker.sell_when") ?: "FULL"

        when (sellWhen.uppercase()) {
            "INSTA" -> {
                val itemValue = price * item.amount
                plugin.economy?.depositPlayer(player, itemValue)
                MessageManager.sendMessage(player, "messages.sell",
                    "amount" to item.amount.toString(),
                    "item" to ShulkerManager.getItemName(item.type),
                    "price" to String.format("%.2f", itemValue))
                SoundManager.playSound(player, "sounds.sell")
                event.isCancelled = true
                event.item.remove()
                return
            }

            else -> {
                val remaining = shulkerBox.inventory.addItem(item)
                if (remaining.isEmpty()) {
                    meta.blockState = shulkerBox
                    shulker.itemMeta = meta
                    player.inventory.setItem(inventoryIndex, shulker)

                    if (shouldSellNow(shulkerBox, sellWhen)) {
                        sellContents(player, shulkerBox)
                        meta.blockState = shulkerBox
                        shulker.itemMeta = meta
                        player.inventory.setItem(inventoryIndex, shulker)
                    }

                    SoundManager.playSound(player, "sounds.pickup")
                    event.isCancelled = true
                    event.item.remove()
                }
            }
        }
    }

    private fun shouldSellNow(shulkerBox: ShulkerBox, sellWhen: String): Boolean {
        val contents = shulkerBox.inventory.contents
        return when (sellWhen.uppercase()) {
            "STACK" -> contents.any { it?.amount == it?.maxStackSize }
            "LAST_SLOT" -> contents.count { it != null } >= shulkerBox.inventory.size - 1
            "FULL" -> contents.count { it != null } >= shulkerBox.inventory.size
            else -> false
        }
    }

    private fun sellContents(player: Player, shulkerBox: ShulkerBox) {
        val contents = shulkerBox.inventory.contents
        var totalEarned = 0.0

        contents.forEachIndexed { index, item ->
            if (item != null) {
                val price = plugin.configManager.getPrice(item.type.name)
                if (price > 0.0) {
                    val itemValue = price * item.amount
                    plugin.economy?.depositPlayer(player, itemValue)
                    totalEarned += itemValue
                    MessageManager.sendMessage(player, "messages.sell",
                        "amount" to item.amount.toString(),
                        "item" to ShulkerManager.getItemName(item.type),
                        "price" to String.format("%.2f", itemValue))
                    shulkerBox.inventory.setItem(index, null)
                }
            }
        }

        if (totalEarned > 0.0) {
            SoundManager.playSound(player, "sounds.sell")
        }
    }
}
