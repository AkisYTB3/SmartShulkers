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
import org.notionsmp.smartshulkers.SoundManager
import org.notionsmp.smartshulkers.utils.ShulkerManager
import org.bukkit.Material

class SellListener(private val plugin: SmartShulkers) : Listener {
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onItemPickup(event: EntityPickupItemEvent) {
        if (event.isCancelled) return
        if (plugin.economy == null) return
        if (event.entity !is Player) return
        val player = event.entity as Player
        val item = event.item.itemStack

        if (plugin.configManager.shouldIgnoreItem(item)) return

        player.inventory.contents.forEachIndexed { index, inventoryItem ->
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
        if (price <= 0.0) {
            SoundManager.playSound(player, "sounds.error")
            sendShulkerMessage(player, "sellshulker", item, price, true)
            event.isCancelled = true
            return
        }

        val meta = shulker.itemMeta as? BlockStateMeta ?: return
        val shulkerBox = meta.blockState as? ShulkerBox ?: return
        val sellWhen = plugin.configManager.getString("settings.sellshulker.sell_when") ?: "FULL"

        when (sellWhen.uppercase()) {
            "INSTA" -> {
                val itemValue = price * item.amount
                plugin.economy?.depositPlayer(player, itemValue)
                sendShulkerMessage(player, "sellshulker", item, itemValue)
                SoundManager.playSound(player, "sounds.sell")
                event.isCancelled = true
                event.item.remove()
                return
            }

            else -> {
                val originalAmount = item.amount
                val clone = item.clone()
                val remaining = shulkerBox.inventory.addItem(clone)

                meta.blockState = shulkerBox
                shulker.itemMeta = meta
                player.inventory.setItem(inventoryIndex, shulker)

                val remainingAmount = remaining.values.sumOf { it.amount }
                val acceptedAmount = originalAmount - remainingAmount

                if (acceptedAmount > 0) {
                    if (remainingAmount == 0) {
                        event.isCancelled = true
                        event.item.remove()
                    } else {
                        event.item.itemStack.amount = remainingAmount
                        event.isCancelled = true
                    }

                    SoundManager.playSound(player, "sounds.pickup")

                    if (shouldSellNow(shulkerBox, sellWhen)) {
                        sellContents(player, shulkerBox)
                        meta.blockState = shulkerBox
                        shulker.itemMeta = meta
                        player.inventory.setItem(inventoryIndex, shulker)
                    }
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
        val itemMap = mutableMapOf<Material, Pair<Int, Double>>()

        contents.forEachIndexed { index, item ->
            if (item != null) {
                val price = plugin.configManager.getPrice(item.type.name)
                if (price > 0.0) {
                    val current = itemMap[item.type] ?: Pair(0, 0.0)
                    itemMap[item.type] = Pair(current.first + item.amount, current.second + (price * item.amount))
                    shulkerBox.inventory.setItem(index, null)
                }
            }
        }

        var totalEarned = 0.0
        itemMap.forEach { (material, amountAndPrice) ->
            val (amount, price) = amountAndPrice
            totalEarned += price
            sendCombinedShulkerMessage(player, "sellshulker", material, amount, price)
        }

        if (totalEarned > 0.0) {
            SoundManager.playSound(player, "sounds.sell")
        }
    }

    private fun sendCombinedShulkerMessage(player: Player, shulkerType: String, material: Material, amount: Int, price: Double) {
        val settingsPath = "settings.$shulkerType.message"
        if (!plugin.config.getBoolean("$settingsPath.enabled", true)) return

        val message = plugin.config.getString("$settingsPath.sell")
            ?.replace("<amount>", amount.toString())
            ?.replace("<item>", ShulkerManager.getItemName(material))
            ?.replace("<price>", "%.2f".format(price))
            ?: return

        when (plugin.config.getString("$settingsPath.type")?.uppercase() ?: "ACTIONBAR") {
            "CHAT" -> player.sendMessage(plugin.mm.deserialize(message))
            "ACTIONBAR" -> player.sendActionBar(plugin.mm.deserialize(message))
        }
    }

    private fun sendShulkerMessage(player: Player, shulkerType: String, item: ItemStack, price: Double = 0.0, isError: Boolean = false) {
        val settingsPath = "settings.$shulkerType.message"
        if (!plugin.config.getBoolean("$settingsPath.enabled", true)) return

        val message = when {
            isError -> plugin.config.getString("$settingsPath.error")
            else -> plugin.config.getString("$settingsPath.sell")
        }?.replace("<amount>", item.amount.toString())
            ?.replace("<item>", ShulkerManager.getItemName(item.type))
            ?.replace("<price>", "%.2f".format(price))
            ?: return

        when (plugin.config.getString("$settingsPath.type")?.uppercase() ?: "ACTIONBAR") {
            "CHAT" -> player.sendMessage(plugin.mm.deserialize(message))
            "ACTIONBAR" -> player.sendActionBar(plugin.mm.deserialize(message))
        }
    }
}