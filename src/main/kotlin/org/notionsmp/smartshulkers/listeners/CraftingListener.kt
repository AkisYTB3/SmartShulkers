package org.notionsmp.smartshulkers.listeners

import org.bukkit.DyeColor
import org.bukkit.Material
import org.bukkit.block.ShulkerBox
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
        val recipe = event.recipe ?: return
        val matrix = event.inventory.matrix
        val shulkerItem = matrix.firstOrNull { it != null && ShulkerManager.isShulkerBox(it.type) } ?: return
        val dyeItem = matrix.firstOrNull { it != null && it.type.toString().endsWith("_DYE") }

        if (dyeItem != null) {
            val dyeColor = DyeColor.valueOf(dyeItem.type.toString().removeSuffix("_DYE"))
            val newShulkerType = Material.valueOf("${dyeColor}_SHULKER_BOX")

            if (ShulkerManager.isSmartShulker(shulkerItem)) {

                val newItem = ShulkerManager.createSmartShulker(ItemStack(newShulkerType), emptyList())
                copyContents(shulkerItem, newItem)
                event.inventory.result = newItem
                return
            } else if (ShulkerManager.isGarbageShulker(shulkerItem)) {

                val newItem = ShulkerManager.createGarbageShulker(ItemStack(newShulkerType), emptyList())
                copyContents(shulkerItem, newItem)
                event.inventory.result = newItem
                return
            } else if (ShulkerManager.isSellShulker(shulkerItem)) {

                val newItem = ShulkerManager.createSellShulker(ItemStack(newShulkerType), emptyList())
                copyContents(shulkerItem, newItem)
                event.inventory.result = newItem
                return
            } else {

                val newItem = ItemStack(newShulkerType)
                copyContents(shulkerItem, newItem)
                event.inventory.result = newItem
                return
            }
        }

        if (ShulkerManager.isSmartShulker(shulkerItem) || ShulkerManager.isGarbageShulker(shulkerItem) || ShulkerManager.isSellShulker(shulkerItem)) {
            val originalMeta = shulkerItem.itemMeta as? BlockStateMeta ?: return
            val originalState = originalMeta.blockState as? ShulkerBox ?: return

            val newItem = ItemStack(shulkerItem.type)
            val newMeta = newItem.itemMeta as? BlockStateMeta ?: return
            val newState = newMeta.blockState as? ShulkerBox ?: return

            newState.inventory.contents = originalState.inventory.contents
            newState.update()

            newMeta.blockState = newState
            newItem.itemMeta = newMeta

            event.inventory.result = newItem
            return
        }

        if (ShulkerManager.isShulkerBox(recipe.result?.type)) {
            val result = event.inventory.result ?: return

            if (ShulkerManager.isSmartShulker(result)) {
                if (!event.view.player.hasPermission(plugin.configManager.getString("permissions.craft_smartshulker")!!)) {
                    event.inventory.result = null
                    return
                }
                copyContents(shulkerItem, result)
            }
            else if (ShulkerManager.isGarbageShulker(result)) {
                if (!event.view.player.hasPermission(plugin.configManager.getString("permissions.craft_garbageshulker")!!)) {
                    event.inventory.result = null
                }
            }
            else if (ShulkerManager.isSellShulker(result)) {
                if (!event.view.player.hasPermission(plugin.configManager.getString("permissions.craft_sellshulker")!!)) {
                    event.inventory.result = null
                }
            }
            else {
                event.inventory.result = null
            }
        }
        else if (matrix.any { it != null && ShulkerManager.isShulkerBox(it.type) }) {
            event.inventory.result = null
        }
    }

    private fun copyContents(source: ItemStack, destination: ItemStack) {
        val sourceMeta = source.itemMeta as? BlockStateMeta ?: return
        val sourceState = sourceMeta.blockState as? ShulkerBox ?: return
        val contents = sourceState.inventory.contents ?: return

        val destMeta = destination.itemMeta as? BlockStateMeta ?: return
        val destState = destMeta.blockState as? ShulkerBox ?: return

        destState.inventory.contents = contents
        destState.update()
        destMeta.blockState = destState
        destination.itemMeta = destMeta
    }
}