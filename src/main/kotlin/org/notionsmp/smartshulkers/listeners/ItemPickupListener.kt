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

        var itemHandled = false

        if (plugin.configManager.isSmartShulkerEnabled) {
            player.inventory.contents.forEach { inventoryItem ->
                if (itemHandled) return@forEach

                if (ShulkerManager.isSmartShulker(inventoryItem) &&
                    ShulkerManager.getShulkerItems(inventoryItem).contains(item.type)) {

                    if (tryAddToShulker(event, player, inventoryItem, item, "smartshulker")) {
                        itemHandled = true
                    }
                }
            }
        }

        if (!itemHandled && plugin.configManager.isGarbageShulkerEnabled) {
            player.inventory.contents.forEach { inventoryItem ->
                if (itemHandled) return@forEach

                if (ShulkerManager.isGarbageShulker(inventoryItem) &&
                    ShulkerManager.getShulkerItems(inventoryItem).contains(item.type)) {

                    handleGarbagePickup(event, player, item)
                    itemHandled = true
                }
            }
        }
    }

    private fun tryAddToShulker(
        event: EntityPickupItemEvent,
        player: Player,
        shulker: ItemStack?,
        item: ItemStack,
        shulkerType: String
    ): Boolean {
        if (shulker == null) return false

        (shulker.itemMeta as? BlockStateMeta)?.let { meta ->
            (meta.blockState as? ShulkerBox)?.let { shulkerBox ->
                val remaining = shulkerBox.inventory.addItem(item)
                if (remaining.isEmpty()) {
                    meta.blockState = shulkerBox
                    shulker.itemMeta = meta
                    player.inventory.setItem(player.inventory.first(shulker), shulker)
                    SoundManager.playSound(player, "sounds.pickup")
                    sendShulkerMessage(player, shulkerType, item)
                    event.isCancelled = true
                    event.item.remove()
                    return true
                }
            }
        }
        return false
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