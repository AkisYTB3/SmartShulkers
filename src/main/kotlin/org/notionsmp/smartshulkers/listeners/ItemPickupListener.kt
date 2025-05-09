package org.notionsmp.smartshulkers.listeners

import org.bukkit.block.ShulkerBox
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BlockStateMeta
import org.notionsmp.smartshulkers.SmartShulkers
import org.notionsmp.smartshulkers.utils.SoundManager
import org.notionsmp.smartshulkers.utils.ShulkerManager

class ItemPickupListener(private val plugin: SmartShulkers) : Listener {
    @EventHandler(priority = EventPriority.NORMAL)
    fun onItemPickup(event: EntityPickupItemEvent) {
        if (event.isCancelled) return
        if (event.entity !is Player) return
        val player = event.entity as Player
        val item = event.item.itemStack

        if (plugin.configManager.shouldIgnoreItem(item)) return

        var remainingItems = item.amount
        val itemType = item.type

        if (plugin.configManager.isSmartShulkerEnabled) {
            player.inventory.contents.forEach { inventoryItem ->
                if (remainingItems <= 0) return@forEach

                if (ShulkerManager.isSmartShulker(inventoryItem) &&
                    ShulkerManager.getShulkerItems(inventoryItem).contains(itemType)) {

                    val added = tryAddToShulker(event, player, inventoryItem, item.clone().apply { amount = remainingItems }, "smartshulker")
                    if (added > 0) {
                        remainingItems -= added
                    }
                }
            }
        }

        if (remainingItems > 0 && plugin.configManager.isGarbageShulkerEnabled) {
            player.inventory.contents.forEach { inventoryItem ->
                if (remainingItems <= 0) return@forEach

                if (ShulkerManager.isGarbageShulker(inventoryItem) &&
                    ShulkerManager.getShulkerItems(inventoryItem).contains(itemType)) {

                    handleGarbagePickup(event, player, item.clone().apply { amount = remainingItems })
                    remainingItems = 0
                }
            }
        }

        if (remainingItems > 0) {
            event.item.itemStack.amount = remainingItems
        } else {
            event.isCancelled = true
            event.item.remove()
        }
    }

    private fun tryAddToShulker(
        event: EntityPickupItemEvent,
        player: Player,
        shulker: ItemStack?,
        item: ItemStack,
        shulkerType: String
    ): Int {
        if (shulker == null) return 0

        (shulker.itemMeta as? BlockStateMeta)?.let { meta ->
            (meta.blockState as? ShulkerBox)?.let { shulkerBox ->
                val toAdd = item.clone()
                val remaining = shulkerBox.inventory.addItem(toAdd)
                val added = item.amount - (remaining.values.firstOrNull()?.amount ?: 0)

                if (added > 0) {
                    meta.blockState = shulkerBox
                    shulker.itemMeta = meta
                    player.inventory.setItem(player.inventory.first(shulker), shulker)
                    SoundManager.playSound(player, "sounds.pickup")
                    sendShulkerMessage(player, shulkerType, item.clone().apply { amount = added })
                    return added
                }
            }
        }
        return 0
    }

    private fun handleGarbagePickup(event: EntityPickupItemEvent, player: Player, item: ItemStack) {
        SoundManager.playSound(player, "sounds.garbage")
        sendShulkerMessage(player, "garbageshulker", item)
        event.isCancelled = true
        event.item.remove()
    }

    private fun sendShulkerMessage(player: Player, shulkerType: String, item: ItemStack) {
        val settingsPath = "settings.$shulkerType.message"
        if (!plugin.config.getBoolean("$settingsPath.enabled", true)) return

        val message = plugin.config.getString("$settingsPath.contents")
            ?.replace("<amount>", item.amount.toString())
            ?.replace("<item>", ShulkerManager.getItemName(item.type))
            ?: return

        when (plugin.config.getString("$settingsPath.type")?.uppercase() ?: "ACTIONBAR") {
            "CHAT" -> player.sendMessage(plugin.mm.deserialize(message))
            "ACTIONBAR" -> player.sendActionBar(plugin.mm.deserialize(message))
        }
    }
}