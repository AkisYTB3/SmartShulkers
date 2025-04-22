package org.notionsmp.smartshulkers.listeners

import org.bukkit.Material
import org.bukkit.block.ShulkerBox
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BlockStateMeta
import org.notionsmp.smartshulkers.SmartShulkers
import org.notionsmp.smartshulkers.SoundManager
import org.notionsmp.smartshulkers.utils.ShulkerManager

class ItemPickupListener(private val plugin: SmartShulkers) : Listener {
    @EventHandler
    fun onItemPickup(event: EntityPickupItemEvent) {
        if (event.entity !is Player) return
        val player = event.entity as Player
        val item = event.item.itemStack

        player.inventory.contents?.forEach { inventoryItem ->
            when {
                ShulkerManager.isSmartShulker(inventoryItem) && ShulkerManager.getShulkerItems(inventoryItem).contains(item.type) -> {
                    if (!plugin.configManager.isSmartShulkerEnabled) return
                    if (inventoryItem != null) {
                        handleAutoPickup(event, player, inventoryItem, item)
                    }
                    return
                }
                ShulkerManager.isGarbageShulker(inventoryItem) && ShulkerManager.getShulkerItems(inventoryItem).contains(item.type) -> {
                    if (!plugin.configManager.isGarbageShulkerEnabled) return
                    handleGarbagePickup(event, player, item)
                    return
                }
            }
        }
    }

    private fun handleAutoPickup(event: EntityPickupItemEvent, player: Player, shulker: ItemStack, item: ItemStack) {
        (shulker.itemMeta as? BlockStateMeta)?.let { meta ->
            (meta.blockState as? ShulkerBox)?.let { shulkerBox ->
                val remaining = shulkerBox.inventory.addItem(item)
                if (remaining.isEmpty()) {
                    meta.blockState = shulkerBox
                    shulker.itemMeta = meta
                    player.inventory.setItem(player.inventory.first(shulker), shulker)
                    SoundManager.playSound(player, "sounds.pickup")
                    sendShulkerMessage(player, "smartshulker", item)
                    event.isCancelled = true
                    event.item.remove()
                }
            }
        }
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