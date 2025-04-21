package org.notionsmp.smartshulkers.listeners

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.ShulkerBox
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BlockStateMeta
import org.bukkit.persistence.PersistentDataType
import org.notionsmp.smartshulkers.SmartShulkers
import org.notionsmp.smartshulkers.utils.ShulkerManager

class BlockBreakListener(private val plugin: SmartShulkers) : Listener {
    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        if (event.block.state !is ShulkerBox) return
        val shulkerBox = event.block.state as ShulkerBox
        val contents = shulkerBox.inventory.contents
        val meta = Bukkit.getItemFactory().getItemMeta(event.block.type) as? BlockStateMeta ?: return
        meta.blockState = shulkerBox

        when {
            meta.persistentDataContainer.has(plugin.smartShulkerKey) && plugin.configManager.isSmartShulkerEnabled ->
                handleShulkerBreak(event, meta, contents, ShulkerManager::createSmartShulker)
            meta.persistentDataContainer.has(plugin.garbageShulkerKey) && plugin.configManager.isGarbageShulkerEnabled ->
                handleShulkerBreak(event, meta, contents, ShulkerManager::createGarbageShulker)
            meta.persistentDataContainer.has(plugin.sellShulkerKey) && plugin.configManager.isSellShulkerEnabled ->
                handleShulkerBreak(event, meta, contents, ShulkerManager::createSellShulker)
        }
    }

    private fun handleShulkerBreak(
        event: BlockBreakEvent,
        meta: BlockStateMeta,
        contents: Array<ItemStack?>,
        creator: (ItemStack, List<Material>) -> ItemStack
    ) {
        event.isDropItems = false
        val items = meta.persistentDataContainer.get(plugin.itemsKey, PersistentDataType.STRING)
            ?.split(",")
            ?.mapNotNull { runCatching { Material.valueOf(it) }.getOrNull() }
            ?: emptyList()

        val newShulker = creator(ItemStack(event.block.type), items)
        val newMeta = newShulker.itemMeta as BlockStateMeta
        val newShulkerBox = newMeta.blockState as ShulkerBox

        contents.forEachIndexed { index, item ->
            if (item != null) {
                newShulkerBox.inventory.setItem(index, item)
            }
        }

        newMeta.blockState = newShulkerBox
        newShulker.itemMeta = newMeta
        event.block.world.dropItemNaturally(event.block.location, newShulker)
    }
}