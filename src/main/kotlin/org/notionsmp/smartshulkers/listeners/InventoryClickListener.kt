package org.notionsmp.smartshulkers.listeners

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.ShulkerBox
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BlockStateMeta
import org.bukkit.persistence.PersistentDataType
import org.notionsmp.smartshulkers.SmartShulkers
import org.notionsmp.smartshulkers.SoundManager
import org.notionsmp.smartshulkers.utils.ShulkerManager

class InventoryClickListener(private val plugin: SmartShulkers) : Listener {

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        if (!plugin.configManager.isSmartShulkerEnabled &&
            !plugin.configManager.isGarbageShulkerEnabled &&
            !plugin.configManager.isSellShulkerEnabled) return

        val cursor = event.cursor ?: return
        if (cursor.type == Material.AIR) return
        val clicked = event.currentItem ?: return

        when {
            ShulkerManager.isSmartShulker(clicked) && !event.whoClicked.hasPermission(
                plugin.configManager.getString("permissions.modify_smartshulker")!!
            ) -> {
                event.isCancelled = true
                return
            }
            ShulkerManager.isGarbageShulker(clicked) && !event.whoClicked.hasPermission(
                plugin.configManager.getString("permissions.modify_garbageshulker")!!
            ) -> {
                event.isCancelled = true
                return
            }
            ShulkerManager.isSellShulker(clicked) && !event.whoClicked.hasPermission(
                plugin.configManager.getString("permissions.modify_sellshulker")!!
            ) -> {
                event.isCancelled = true
                return
            }
        }

        if (!ShulkerManager.isSmartShulker(clicked) &&
            !ShulkerManager.isGarbageShulker(clicked) &&
            !ShulkerManager.isSellShulker(clicked)) return

        when {
            ShulkerManager.isSmartShulker(clicked) -> modifyShulker(event, clicked, cursor, plugin.smartShulkerKey)
            ShulkerManager.isGarbageShulker(clicked) -> modifyShulker(event, clicked, cursor, plugin.garbageShulkerKey)
            ShulkerManager.isSellShulker(clicked) -> handleSellShulkerModification(event, clicked, cursor)
        }
    }

    private fun handleSellShulkerModification(event: InventoryClickEvent, clicked: ItemStack, cursor: ItemStack) {
        if (!plugin.configManager.canSellItem(cursor.type)) {
            SoundManager.playSound(event.whoClicked as Player, "sounds.error")
            sendShulkerMessage(event.whoClicked as Player, "sellshulker", cursor, isError = true)
            event.isCancelled = true
            return
        }
        modifyShulker(event, clicked, cursor, plugin.sellShulkerKey)
    }

    private fun modifyShulker(event: InventoryClickEvent, shulker: ItemStack, item: ItemStack, typeKey: NamespacedKey) {
        val currentItems = ShulkerManager.getShulkerItems(shulker)
        val newItems = currentItems.toMutableList().apply {
            if (contains(item.type)) remove(item.type) else add(item.type)
        }

        val shulkerMeta = shulker.itemMeta as? BlockStateMeta
        val shulkerBox = shulkerMeta?.blockState as? ShulkerBox
        val contents = shulkerBox?.inventory?.contents?.clone() ?: arrayOfNulls(27)

        val newShulker = shulker.clone().apply {
            itemMeta = (itemMeta as? BlockStateMeta)?.apply {
                persistentDataContainer.set(
                    plugin.itemsKey,
                    PersistentDataType.STRING,
                    newItems.joinToString(",") { it.name }
                )

                val settingsPath = when(typeKey) {
                    plugin.smartShulkerKey -> "settings.smartshulker"
                    plugin.garbageShulkerKey -> "settings.garbageshulker"
                    else -> "settings.sellshulker"
                }

                lore(buildList {
                    add(plugin.mm.deserialize(plugin.configManager.getString("$settingsPath.lore.accepts")!!))
                    if (newItems.isEmpty()) {
                        add(plugin.mm.deserialize(plugin.configManager.getString("$settingsPath.lore.empty")!!))
                    } else {
                        newItems.forEach {
                            add(plugin.mm.deserialize(
                                plugin.configManager.getString("$settingsPath.lore.itemnames")!!
                                    .replace("<item>", ShulkerManager.getItemName(it))
                            ))
                        }
                    }
                })

                (blockState as? ShulkerBox)?.let { box ->
                    contents.forEachIndexed { index, itemStack ->
                        box.inventory.setItem(index, itemStack)
                    }
                    blockState = box
                }
            }
        }

        event.currentItem = newShulker
        event.isCancelled = true
    }

    private fun sendShulkerMessage(player: Player, shulkerType: String, item: ItemStack, isError: Boolean = false) {
        val settingsPath = "settings.$shulkerType.message"
        if (!plugin.config.getBoolean("$settingsPath.enabled", true)) return

        val message = if (isError) {
            plugin.config.getString("$settingsPath.error")
        } else {
            null
        } ?: return

        when (plugin.config.getString("$settingsPath.type")?.uppercase() ?: "ACTIONBAR") {
            "CHAT" -> player.sendMessage(plugin.mm.deserialize(message))
            "ACTIONBAR" -> player.sendActionBar(plugin.mm.deserialize(message))
        }
    }
}