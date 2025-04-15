package org.notionsmp.smartshulkers

import co.aikar.commands.PaperCommandManager
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.*
import org.bukkit.block.ShulkerBox
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.inventory.meta.BlockStateMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import org.notionsmp.smartshulkers.commands.ShulkerCommands

class SmartShulkers : JavaPlugin(), Listener {
    companion object {
        lateinit var instance: SmartShulkers
            private set
    }

    private val PICKUP_SOUND = "sounds.pickup"
    private val GARBAGE_SOUND = "sounds.garbage"
    private val PICKUP_MESSAGE = "messages.pickup"
    private val PERM_CRAFT_AUTO = "permissions.craft_autoshulker"
    private val PERM_CRAFT_GARBAGE = "permissions.craft_garbageshulker"
    private val PERM_MODIFY_AUTO = "permissions.modify_autoshulker"
    private val PERM_MODIFY_GARBAGE = "permissions.modify_garbageshulker"
    private val SETTINGS_AUTO = "settings.autoshulker"
    private val SETTINGS_GARBAGE = "settings.garbageshulker"

    private val defaultConfig = mapOf(
        PICKUP_SOUND to "entity.item.pickup",
        GARBAGE_SOUND to "block.lava.extinguish",
        PICKUP_MESSAGE to "<green>Picked up <amount> <item>!",
        PERM_CRAFT_AUTO to "autoshulker.craft.autoshulker",
        PERM_CRAFT_GARBAGE to "autoshulker.craft.garbageshulker",
        PERM_MODIFY_AUTO to "autoshulker.modify.autoshulker",
        PERM_MODIFY_GARBAGE to "autoshulker.modify.garbageshulker",
        "$SETTINGS_AUTO.enabled" to true,
        "$SETTINGS_AUTO.name" to "<gradient:gold:yellow>AutoShulker</gradient>",
        "$SETTINGS_GARBAGE.enabled" to true,
        "$SETTINGS_GARBAGE.name" to "<gradient:red:dark_red>Garbage Shulker</gradient>"
    )

    lateinit var autoShulkerKey: NamespacedKey
    lateinit var garbageShulkerKey: NamespacedKey
    lateinit var itemsKey: NamespacedKey
    lateinit var commandManager: PaperCommandManager
    val mm = MiniMessage.miniMessage()

    override fun onEnable() {
        instance = this
        setupConfig()
        initializeKeys()
        registerManagers()
        registerRecipes()
    }

    private fun setupConfig() {
        defaultConfig.forEach { (key, value) ->
            if (!config.contains(key)) config.set(key, value)
        }
        saveConfig()
    }

    private fun initializeKeys() {
        autoShulkerKey = NamespacedKey(this, "autoshulker")
        garbageShulkerKey = NamespacedKey(this, "garbageshulker")
        itemsKey = NamespacedKey(this, "shulkeritems")
    }

    private fun registerManagers() {
        commandManager = PaperCommandManager(this).apply {
            registerCommand(ShulkerCommands(this@SmartShulkers))
        }
        server.pluginManager.registerEvents(this, this)
    }

    fun reload() {
        reloadConfig()
        Bukkit.removeRecipe(autoShulkerKey)
        Bukkit.removeRecipe(garbageShulkerKey)
        if (isAutoShulkerEnabled()) registerRecipes()
        Bukkit.getConsoleSender().sendMessage(mm.deserialize("<green>SmartShulkers config reloaded!"))
    }

    private fun isAutoShulkerEnabled() = config.getBoolean("$SETTINGS_AUTO.enabled", true)
    private fun isGarbageShulkerEnabled() = config.getBoolean("$SETTINGS_GARBAGE.enabled", true)

    private fun registerRecipes() {
        if (isAutoShulkerEnabled()) {
            ShapedRecipe(autoShulkerKey, createAutoShulker(emptyList())).apply {
                shape("B", "S")
                setIngredient('B', Material.BOOK)
                setIngredient('S', Material.SHULKER_BOX)
                Bukkit.addRecipe(this)
            }
        }
        if (isGarbageShulkerEnabled()) {
            ShapedRecipe(garbageShulkerKey, createGarbageShulker(emptyList())).apply {
                shape("L", "S")
                setIngredient('L', Material.LAVA_BUCKET)
                setIngredient('S', Material.SHULKER_BOX)
                Bukkit.addRecipe(this)
            }
        }
    }

    @EventHandler
    fun onPrepareCraft(event: PrepareItemCraftEvent) {
        val recipe = event.recipe ?: return
        when {
            recipe.result?.type == Material.SHULKER_BOX -> {
                if (isAutoShulker(event.inventory.result) &&
                    !event.view.player.hasPermission(config.getString(PERM_CRAFT_AUTO)!!)) {
                    event.inventory.result = null
                }
                if (isGarbageShulker(event.inventory.result) &&
                    !event.view.player.hasPermission(config.getString(PERM_CRAFT_GARBAGE)!!)) {
                    event.inventory.result = null
                }
            }
        }
    }

    private fun playPickupSound(player: Player) {
        val soundName = config.getString(PICKUP_SOUND) ?: return
        player.playSound(player.location, soundName, 1.0f, 1.0f)
    }

    private fun playGarbageSound(player: Player) {
        val soundName = config.getString(GARBAGE_SOUND) ?: return
        player.playSound(player.location, soundName, 1.0f, 1.0f)
    }

    private fun getItemName(material: Material): String {
        return material.translationKey()
            .substringAfterLast('.')
            .split('_')
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
    }

    private fun sendPickupMessage(player: Player, item: ItemStack) {
        val rawMessage = config.getString(PICKUP_MESSAGE) ?: return
        val message = rawMessage
            .replace("<amount>", item.amount.toString())
            .replace("<item>", getItemName(item.type))
        player.sendActionBar(mm.deserialize(message))
    }

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        if (!isAutoShulkerEnabled() && !isGarbageShulkerEnabled()) return
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
        if (!isAutoShulkerEnabled() && !isGarbageShulkerEnabled()) return
        val cursor = event.cursor ?: return
        if (cursor.type == Material.AIR) return
        val clicked = event.currentItem ?: return

        if (isAutoShulker(clicked) && !event.whoClicked.hasPermission(config.getString(PERM_MODIFY_AUTO)!!)) {
            event.isCancelled = true
            return
        }
        if (isGarbageShulker(clicked) && !event.whoClicked.hasPermission(config.getString(PERM_MODIFY_GARBAGE)!!)) {
            event.isCancelled = true
            return
        }

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
        if (!isAutoShulkerEnabled() && !isGarbageShulkerEnabled()) return
        if (event.entity !is Player) return
        val player = event.entity as Player
        val item = event.item.itemStack
        player.inventory.contents.forEachIndexed { index, inventoryItem ->
            if (inventoryItem == null) return@forEachIndexed
            when {
                isAutoShulker(inventoryItem) && getShulkerItems(inventoryItem).contains(item.type) -> {
                    if (!isAutoShulkerEnabled()) return
                    handleAutoPickup(event, player, index, inventoryItem, item)
                    return
                }
                isGarbageShulker(inventoryItem) && getShulkerItems(inventoryItem).contains(item.type) -> {
                    if (!isGarbageShulkerEnabled()) return
                    handleGarbagePickup(event, player, item)
                    return
                }
            }
        }
    }

    private fun handleAutoPickup(
        event: EntityPickupItemEvent,
        player: Player,
        index: Int,
        shulker: ItemStack,
        item: ItemStack
    ) {
        (shulker.itemMeta as? BlockStateMeta)?.let { meta ->
            (meta.blockState as? ShulkerBox)?.let { shulkerBox ->
                val remaining = shulkerBox.inventory.addItem(item)
                if (remaining.isEmpty()) {
                    meta.blockState = shulkerBox
                    shulker.itemMeta = meta
                    player.inventory.setItem(index, shulker)
                    playPickupSound(player)
                    sendPickupMessage(player, item)
                    event.isCancelled = true
                    event.item.remove()
                }
            }
        }
    }

    private fun handleGarbagePickup(
        event: EntityPickupItemEvent,
        player: Player,
        item: ItemStack
    ) {
        playGarbageSound(player)
        event.isCancelled = true
        event.item.remove()
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
                displayName(mm.deserialize(
                    if (typeKey == autoShulkerKey) config.getString("$SETTINGS_AUTO.name")!!
                    else config.getString("$SETTINGS_GARBAGE.name")!!
                ))
                lore(buildList {
                    add(mm.deserialize("<gray>Accepts:"))
                    if (items.isEmpty()) add(mm.deserialize("<dark_gray>- None"))
                    else items.forEach { add(mm.deserialize("<dark_gray>- ${getItemName(it)}")) }
                })
                blockState = blockState
            }
        }
    }

    private fun createAutoShulker(items: List<Material>) = createShulker(autoShulkerKey, items)
    private fun createGarbageShulker(items: List<Material>) = createShulker(garbageShulkerKey, items)
}