package org.notionsmp.smartshulkers.utils

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BlockStateMeta
import org.bukkit.persistence.PersistentDataType
import org.notionsmp.smartshulkers.SmartShulkers

object ShulkerManager {
    fun isShulkerBox(material: Material?): Boolean {
        return material in SmartShulkers.SHULKER_BOX_TYPES
    }

    fun isSmartShulker(item: ItemStack?): Boolean {
        return item != null && isShulkerBox(item.type) &&
                item.itemMeta?.persistentDataContainer?.has(SmartShulkers.instance.smartShulkerKey) == true
    }

    fun isGarbageShulker(item: ItemStack?): Boolean {
        return item != null && isShulkerBox(item.type) &&
                item.itemMeta?.persistentDataContainer?.has(SmartShulkers.instance.garbageShulkerKey) == true
    }

    fun isSellShulker(item: ItemStack?): Boolean {
        return item != null && isShulkerBox(item.type) &&
                item.itemMeta?.persistentDataContainer?.has(SmartShulkers.instance.sellShulkerKey) == true
    }

    fun getShulkerItems(shulker: ItemStack?): List<Material> {
        return shulker?.itemMeta?.persistentDataContainer?.get(
            SmartShulkers.instance.itemsKey,
            PersistentDataType.STRING
        )?.split(",")
            ?.mapNotNull { runCatching { Material.valueOf(it) }.getOrNull() }
            ?: emptyList()
    }

    fun createShulker(baseItem: ItemStack, typeKey: NamespacedKey, items: List<Material>): ItemStack {
        val config = SmartShulkers.instance.configManager
        val settingsPath = when (typeKey) {
            SmartShulkers.instance.smartShulkerKey -> "settings.smartshulker"
            SmartShulkers.instance.garbageShulkerKey -> "settings.garbageshulker"
            else -> "settings.sellshulker"
        }

        return ItemStack(baseItem.type).apply {
            itemMeta = (itemMeta as? BlockStateMeta)?.apply {
                persistentDataContainer.set(typeKey, PersistentDataType.BYTE, 1)
                persistentDataContainer.set(
                    SmartShulkers.instance.itemsKey,
                    PersistentDataType.STRING,
                    items.joinToString(",") { it.name }
                )

                displayName(SmartShulkers.instance.mm.deserialize(config.getString("$settingsPath.name")!!))

                lore(buildList {
                    add(SmartShulkers.instance.mm.deserialize(config.getString("$settingsPath.lore.accepts")!!))
                    if (items.isEmpty()) {
                        add(SmartShulkers.instance.mm.deserialize(config.getString("$settingsPath.lore.empty")!!))
                    } else {
                        items.forEach {
                            add(
                                SmartShulkers.instance.mm.deserialize(
                                config.getString("$settingsPath.lore.itemnames")!!
                                    .replace("<item>", getItemName(it))
                            ))
                        }
                    }
                })

                blockState = (baseItem.itemMeta as? BlockStateMeta)?.blockState ?: blockState
            }
        }
    }

    fun createSmartShulker(baseItem: ItemStack, items: List<Material>) =
        createShulker(baseItem, SmartShulkers.instance.smartShulkerKey, items)

    fun createGarbageShulker(baseItem: ItemStack, items: List<Material>) =
        createShulker(baseItem, SmartShulkers.instance.garbageShulkerKey, items)

    fun createSellShulker(baseItem: ItemStack, items: List<Material>) =
        createShulker(baseItem, SmartShulkers.instance.sellShulkerKey, items)

    fun getItemName(material: Material): String {
        return material.translationKey()
            .substringAfterLast('.')
            .split('_')
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
    }
}