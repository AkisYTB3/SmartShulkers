package org.notionsmp.autoshulker

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.ShulkerBox
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.inventory.meta.BlockStateMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin

class AutoShulker : JavaPlugin(), Listener {
    private lateinit var autoShulkerKey: NamespacedKey
    private lateinit var garbageShulkerKey: NamespacedKey
    private lateinit var itemsKey: NamespacedKey
    private val mm = MiniMessage.miniMessage()

    override fun onEnable() {
        autoShulkerKey = NamespacedKey(this, "autoshulker")
        garbageShulkerKey = NamespacedKey(this, "garbageshulker")
        itemsKey = NamespacedKey(this, "shulkeritems")
        server.pluginManager.registerEvents(this, this)
        registerRecipes()
    }

    private fun registerRecipes() {
        ShapedRecipe(autoShulkerKey, createAutoShulker(emptyList())).apply {
            shape("B", "S")
            setIngredient('B', Material.BOOK)
            setIngredient('S', Material.SHULKER_BOX)
            Bukkit.addRecipe(this)
        }

        ShapedRecipe(garbageShulkerKey, createGarbageShulker(emptyList())).apply {
            shape("L", "S")
            setIngredient('L', Material.LAVA_BUCKET)
            setIngredient('S', Material.SHULKER_BOX)
            Bukkit.addRecipe(this)
        }
    }

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        if (event.block.state !is ShulkerBox) return

        val shulkerBox = event.block.state as ShulkerBox
        val contents = shulkerBox.inventory.contents

        val meta = Bukkit.getItemFactory().getItemMeta(Material.SHULKER_BOX) as? BlockStateMeta ?: return
        meta.blockState = shulkerBox

        val isAuto = meta.persistentDataContainer.has(autoShulkerKey)
        val isGarbage = meta.persistentDataContainer.has(garbageShulkerKey)

        if (isAuto || isGarbage) {
            event.isDropItems = false

            val items = meta.persistentDataContainer.get(itemsKey, PersistentDataType.STRING)
                ?.split(",")
                ?.mapNotNull { runCatching { Material.valueOf(it) }.getOrNull() }
                ?: emptyList()

            val newShulker = if (isAuto) createAutoShulker(items) else createGarbageShulker(items)

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

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val cursor = event.cursor ?: return
        if (cursor.type == Material.AIR) return
        val clicked = event.currentItem ?: return

        if (!isAutoShulker(clicked) && !isGarbageShulker(clicked)) return

        val currentItems = getShulkerItems(clicked)
        val newItems = currentItems.toMutableList().apply {
            if (contains(cursor.type)) remove(cursor.type) else add(cursor.type)
        }

        event.currentItem = createShulker(
            if (isAutoShulker(clicked)) autoShulkerKey else garbageShulkerKey,
            newItems
        )
        event.isCancelled = true
    }

    @EventHandler
    fun onItemPickup(event: EntityPickupItemEvent) {
        if (event.entity !is Player) return
        val player = event.entity as Player
        val item = event.item.itemStack

        player.inventory.contents.forEachIndexed { index, inventoryItem ->
            if (inventoryItem == null) return@forEachIndexed

            when {
                isAutoShulker(inventoryItem) && getShulkerItems(inventoryItem).contains(item.type) -> {
                    val meta = inventoryItem.itemMeta as? BlockStateMeta ?: return@forEachIndexed
                    val shulkerBox = meta.blockState as? ShulkerBox ?: return@forEachIndexed

                    val remaining = shulkerBox.inventory.addItem(item)
                    if (remaining.isEmpty()) {
                        meta.blockState = shulkerBox
                        inventoryItem.itemMeta = meta
                        player.inventory.setItem(index, inventoryItem)

                        event.isCancelled = true
                        event.item.remove()
                        return
                    }
                }
                isGarbageShulker(inventoryItem) && getShulkerItems(inventoryItem).contains(item.type) -> {
                    event.isCancelled = true
                    event.item.remove()
                    return
                }
            }
        }
    }

    private fun isAutoShulker(item: ItemStack?): Boolean {
        if (item?.type != Material.SHULKER_BOX) return false
        return item.itemMeta?.persistentDataContainer?.has(autoShulkerKey) == true
    }

    private fun isGarbageShulker(item: ItemStack?): Boolean {
        if (item?.type != Material.SHULKER_BOX) return false
        return item.itemMeta?.persistentDataContainer?.has(garbageShulkerKey) == true
    }

    private fun getShulkerItems(shulker: ItemStack?): List<Material> {
        return shulker?.itemMeta?.persistentDataContainer?.get(itemsKey, PersistentDataType.STRING)
            ?.split(",")
            ?.mapNotNull { runCatching { Material.valueOf(it) }.getOrNull() }
            ?: emptyList()
    }

    private fun getShulkerItems(meta: BlockStateMeta): List<Material> {
        return meta.persistentDataContainer.get(itemsKey, PersistentDataType.STRING)
            ?.split(",")
            ?.mapNotNull { runCatching { Material.valueOf(it) }.getOrNull() }
            ?: emptyList()
    }

    private fun createShulker(typeKey: NamespacedKey, items: List<Material>): ItemStack {
        return ItemStack(Material.SHULKER_BOX).apply {
            itemMeta = (itemMeta as? BlockStateMeta)?.apply {
                persistentDataContainer.set(typeKey, PersistentDataType.BYTE, 1)
                persistentDataContainer.set(itemsKey, PersistentDataType.STRING, items.joinToString(",") { it.name })

                displayName(if (typeKey == autoShulkerKey)
                    mm.deserialize("<gradient:gold:yellow>AutoShulker</gradient>")
                else
                    mm.deserialize("<gradient:red:dark_red>Garbage Shulker</gradient>"))

                lore(buildList {
                    add(mm.deserialize("<gray>Accepts:"))
                    if (items.isEmpty()) add(mm.deserialize("<dark_gray>- None"))
                    else items.forEach { add(mm.deserialize("<dark_gray>- ${it.name}")) }
                })

                // Initialize the shulker inventory
                blockState = blockState // This creates a fresh ShulkerBox state
            }
        }
    }

    private fun createAutoShulker(items: List<Material>) = createShulker(autoShulkerKey, items)
    private fun createGarbageShulker(items: List<Material>) = createShulker(garbageShulkerKey, items)
}