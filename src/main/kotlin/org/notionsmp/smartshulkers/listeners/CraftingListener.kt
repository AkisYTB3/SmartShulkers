package org.notionsmp.smartshulkers.listeners

import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BlockStateMeta
import org.notionsmp.smartshulkers.SmartShulkers
import org.notionsmp.smartshulkers.utils.ShulkerManager

class CraftingListener(private val plugin: SmartShulkers) : Listener {
    @EventHandler
    fun onPrepareCraft(event: PrepareItemCraftEvent) {
        val inventory = event.inventory

        if (inventory.matrix.size == 1 && inventory.matrix[0]?.let {
                ShulkerManager.isSmartShulker(it) || ShulkerManager.isGarbageShulker(it) || ShulkerManager.isSellShulker(it)
            } == true) {
            val shulker = inventory.matrix[0]!!
            val normalShulker = ItemStack(shulker.type)
            (shulker.itemMeta as? BlockStateMeta)?.blockState?.let {
                (normalShulker.itemMeta as? BlockStateMeta)?.blockState = it
            }
            event.inventory.result = normalShulker
            return
        }

        val recipe = event.recipe ?: return
        when {
            ShulkerManager.isShulkerBox(recipe.result?.type) -> {
                if (ShulkerManager.isSmartShulker(inventory.result) &&
                    !event.view.player.hasPermission(plugin.configManager.getString("permissions.craft_smartshulker")!!)) {
                    event.inventory.result = null
                }
                if (ShulkerManager.isGarbageShulker(inventory.result) &&
                    !event.view.player.hasPermission(plugin.configManager.getString("permissions.craft_garbageshulker")!!)) {
                    event.inventory.result = null
                }
                if (ShulkerManager.isSellShulker(inventory.result) &&
                    !event.view.player.hasPermission(plugin.configManager.getString("permissions.craft_sellshulker")!!)) {
                    event.inventory.result = null
                }
            }
        }
    }
}