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
import org.bukkit.inventory.ShapelessRecipe
import org.bukkit.inventory.meta.BlockStateMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import org.notionsmp.smartshulkers.commands.ShulkerCommands

class SmartShulkers : JavaPlugin(), Listener {
    companion object {
        lateinit var instance: SmartShulkers
            private set

        private val SHULKER_BOX_TYPES = setOf(
            Material.SHULKER_BOX,
            Material.WHITE_SHULKER_BOX,
            Material.ORANGE_SHULKER_BOX,
            Material.MAGENTA_SHULKER_BOX,
            Material.LIGHT_BLUE_SHULKER_BOX,
            Material.YELLOW_SHULKER_BOX,
            Material.LIME_SHULKER_BOX,
            Material.PINK_SHULKER_BOX,
            Material.GRAY_SHULKER_BOX,
            Material.LIGHT_GRAY_SHULKER_BOX,
            Material.CYAN_SHULKER_BOX,
            Material.PURPLE_SHULKER_BOX,
            Material.BLUE_SHULKER_BOX,
            Material.BROWN_SHULKER_BOX,
            Material.GREEN_SHULKER_BOX,
            Material.RED_SHULKER_BOX,
            Material.BLACK_SHULKER_BOX
        )
    }

    private val PICKUP_SOUND = "sounds.pickup"
    private val GARBAGE_SOUND = "sounds.garbage"
    private val MESSAGE_TYPE = "messages.type"
    private val PICKUP_MESSAGE = "messages.pickup"
    private val GARBAGE_MESSAGE = "messages.garbage"
    private val PERM_CRAFT_AUTO = "permissions.craft_smartshulker"
    private val PERM_CRAFT_GARBAGE = "permissions.craft_garbageshulker"
    private val PERM_MODIFY_AUTO = "permissions.modify_smartshulker"
    private val PERM_MODIFY_GARBAGE = "permissions.modify_garbageshulker"
    private val SETTINGS_AUTO = "settings.smartshulker"
    private val SETTINGS_GARBAGE = "settings.garbageshulker"

    private val defaultConfig = mapOf(
        PICKUP_SOUND to "entity.item.pickup",
        GARBAGE_SOUND to "block.lava.extinguish",
        MESSAGE_TYPE to "ACTIONBAR",
        PICKUP_MESSAGE to "<green>Picked up <amount> <item>!",
        GARBAGE_MESSAGE to "<red>Deleted <amount> <item>!",
        PERM_CRAFT_AUTO to "smartshulkers.craft.smartshulker",
        PERM_CRAFT_GARBAGE to "smartshulkers.craft.garbageshulker",
        PERM_MODIFY_AUTO to "smartshulkers.modify.smartshulker",
        PERM_MODIFY_GARBAGE to "smartshulkers.modify.garbageshulker",
        "$SETTINGS_AUTO.enabled" to true,
        "$SETTINGS_AUTO.name" to "<gradient:gold:yellow>Smart Shulker</gradient>",
        "$SETTINGS_AUTO.lore.accepts" to "<gray>Accepts:",
        "$SETTINGS_AUTO.lore.itemnames" to "<dark_gray>- <item>",
        "$SETTINGS_AUTO.lore.empty" to "<dark_gray>- None",
        "$SETTINGS_GARBAGE.enabled" to true,
        "$SETTINGS_GARBAGE.name" to "<gradient:red:dark_red>Garbage Shulker</gradient>",
        "$SETTINGS_GARBAGE.lore.accepts" to "<gray>Destroys:",
        "$SETTINGS_GARBAGE.lore.itemnames" to "<dark_gray>- <item>",
        "$SETTINGS_GARBAGE.lore.empty" to "<dark_gray>- None"
    )

    lateinit var smartShulkerKey: NamespacedKey
    lateinit var garbageShulkerKey: NamespacedKey
    lateinit var itemsKey: NamespacedKey
    lateinit var commandManager: PaperCommandManager
    val mm = MiniMessage.miniMessage()

    override fun onEnable() {
        instance = this
        saveDefaultConfig()
        initializeKeys()
        registerManagers()
        registerRecipes()
    }

    private fun initializeKeys() {
        smartShulkerKey = NamespacedKey(this, "smartshulker")
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
        Bukkit.removeRecipe(smartShulkerKey)
        Bukkit.removeRecipe(garbageShulkerKey)
        if (isSmartShulkerEnabled()) registerRecipes()
        Bukkit.getConsoleSender().sendMessage(mm.deserialize("<green>SmartShulkers config reloaded!"))
    }

    private fun isSmartShulkerEnabled() = config.getBoolean("$SETTINGS_AUTO.enabled", true)
    private fun isGarbageShulkerEnabled() = config.getBoolean("$SETTINGS_GARBAGE.enabled", true)

    private fun isShulkerBox(material: Material?): Boolean {
        return material in SHULKER_BOX_TYPES
    }

    private fun registerRecipes() {
        if (isSmartShulkerEnabled()) {
            SHULKER_BOX_TYPES.forEach { shulkerType ->
                val recipeKey = NamespacedKey(this, "smartshulker_${shulkerType.name.lowercase()}")
                val recipe = ShapelessRecipe(recipeKey, createSmartShulker(ItemStack(shulkerType), emptyList()))
                recipe.addIngredient(Material.BOOK)
                recipe.addIngredient(shulkerType)
                Bukkit.addRecipe(recipe)
            }
        }
        if (isGarbageShulkerEnabled()) {
            SHULKER_BOX_TYPES.forEach { shulkerType ->
                val recipeKey = NamespacedKey(this, "garbageshulker_${shulkerType.name.lowercase()}")
                val recipe = ShapelessRecipe(recipeKey, createGarbageShulker(ItemStack(shulkerType), emptyList()))
                recipe.addIngredient(Material.LAVA_BUCKET)
                recipe.addIngredient(shulkerType)
                Bukkit.addRecipe(recipe)
            }
        }

        SHULKER_BOX_TYPES.forEach { shulkerType ->
            val recipeKey = NamespacedKey(this, "uncraft_smartshulker_${shulkerType.name.lowercase()}")
            val recipe = ShapelessRecipe(recipeKey, ItemStack(shulkerType))
            recipe.addIngredient(Material.BOOK)
            recipe.addIngredient(Material.BOOK)
            Bukkit.addRecipe(recipe)
        }
    }

    @EventHandler
    fun onPrepareCraft(event: PrepareItemCraftEvent) {
        val recipe = event.recipe ?: return
        val inventory = event.inventory

        if (inventory.matrix.any { isSmartShulker(it) || isGarbageShulker(it) }) {
            val shulkerType = inventory.matrix.firstOrNull { isShulkerBox(it?.type) }?.type ?: return
            event.inventory.result = ItemStack(shulkerType)
            return
        }

        when {
            isShulkerBox(recipe.result?.type) -> {
                if (isSmartShulker(inventory.result) &&
                    !event.view.player.hasPermission(config.getString(PERM_CRAFT_AUTO)!!)) {
                    event.inventory.result = null
                }
                if (isGarbageShulker(inventory.result) &&
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

    private fun sendMessage(player: Player, item: ItemStack, isPickup: Boolean) {
        val messageType = config.getString(MESSAGE_TYPE) ?: "ACTIONBAR"
        val rawMessage = if (isPickup) {
            config.getString(PICKUP_MESSAGE) ?: return
        } else {
            config.getString(GARBAGE_MESSAGE) ?: return
        }

        val message = rawMessage
            .replace("<amount>", item.amount.toString())
            .replace("<item>", getItemName(item.type))

        when (messageType.uppercase()) {
            "CHAT" -> player.sendMessage(mm.deserialize(message))
            else -> player.sendActionBar(mm.deserialize(message))
        }
    }

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        if (!isSmartShulkerEnabled() && !isGarbageShulkerEnabled()) return
        if (event.block.state !is ShulkerBox) return
        val shulkerBox = event.block.state as ShulkerBox
        val contents = shulkerBox.inventory.contents
        val meta = Bukkit.getItemFactory().getItemMeta(event.block.type) as? BlockStateMeta ?: return
        meta.blockState = shulkerBox
        val isAuto = meta.persistentDataContainer.has(smartShulkerKey)
        val isGarbage = meta.persistentDataContainer.has(garbageShulkerKey)
        if (isAuto || isGarbage) {
            event.isDropItems = false
            val items = meta.persistentDataContainer.get(itemsKey, PersistentDataType.STRING)
                ?.split(",")
                ?.mapNotNull { runCatching { Material.valueOf(it) }.getOrNull() }
                ?: emptyList()
            val newShulker = if (isAuto) createSmartShulker(ItemStack(event.block.type), items)
            else createGarbageShulker(ItemStack(event.block.type), items)
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
        if (!isSmartShulkerEnabled() && !isGarbageShulkerEnabled()) return
        val cursor = event.cursor ?: return
        if (cursor.type == Material.AIR) return
        val clicked = event.currentItem ?: return

        if (isSmartShulker(clicked) && !event.whoClicked.hasPermission(config.getString(PERM_MODIFY_AUTO)!!)) {
            event.isCancelled = true
            return
        }
        if (isGarbageShulker(clicked) && !event.whoClicked.hasPermission(config.getString(PERM_MODIFY_GARBAGE)!!)) {
            event.isCancelled = true
            return
        }

        if (!isSmartShulker(clicked) && !isGarbageShulker(clicked)) return
        val currentItems = getShulkerItems(clicked)
        val newItems = currentItems.toMutableList().apply {
            if (contains(cursor.type)) remove(cursor.type) else add(cursor.type)
        }
        event.currentItem = createShulker(clicked,
            if (isSmartShulker(clicked)) smartShulkerKey else garbageShulkerKey,
            newItems)
        event.isCancelled = true
    }

    @EventHandler
    fun onItemPickup(event: EntityPickupItemEvent) {
        if (!isSmartShulkerEnabled() && !isGarbageShulkerEnabled()) return
        if (event.entity !is Player) return
        val player = event.entity as Player
        val item = event.item.itemStack
        player.inventory.contents.forEachIndexed { index, inventoryItem ->
            if (inventoryItem == null) return@forEachIndexed
            when {
                isSmartShulker(inventoryItem) && getShulkerItems(inventoryItem).contains(item.type) -> {
                    if (!isSmartShulkerEnabled()) return
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
                    sendMessage(player, item, true)
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
        sendMessage(player, item, false)
        event.isCancelled = true
        event.item.remove()
    }

    private fun isSmartShulker(item: ItemStack?): Boolean {
        if (item == null || !isShulkerBox(item.type)) return false
        return item.itemMeta?.persistentDataContainer?.has(smartShulkerKey) == true
    }

    private fun isGarbageShulker(item: ItemStack?): Boolean {
        if (item == null || !isShulkerBox(item.type)) return false
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

    private fun createShulker(baseItem: ItemStack, typeKey: NamespacedKey, items: List<Material>): ItemStack {
        return ItemStack(baseItem.type).apply {
            itemMeta = (itemMeta as? BlockStateMeta)?.apply {
                persistentDataContainer.set(typeKey, PersistentDataType.BYTE, 1)
                persistentDataContainer.set(itemsKey, PersistentDataType.STRING, items.joinToString(",") { it.name })
                displayName(mm.deserialize(
                    if (typeKey == smartShulkerKey) config.getString("$SETTINGS_AUTO.name")!!
                    else config.getString("$SETTINGS_GARBAGE.name")!!
                ))
                lore(buildList {
                    add(mm.deserialize(
                        if (typeKey == smartShulkerKey) config.getString("$SETTINGS_AUTO.lore.accepts")!!
                        else config.getString("$SETTINGS_GARBAGE.lore.accepts")!!
                    ))
                    if (items.isEmpty()) {
                        add(mm.deserialize(
                            if (typeKey == smartShulkerKey) config.getString("$SETTINGS_AUTO.lore.empty")!!
                            else config.getString("$SETTINGS_GARBAGE.lore.empty")!!
                        ))
                    } else {
                        items.forEach {
                            add(mm.deserialize(
                                (if (typeKey == smartShulkerKey) config.getString("$SETTINGS_AUTO.lore.itemnames")!!
                                else config.getString("$SETTINGS_GARBAGE.lore.itemnames")!!)
                                    .replace("<item>", getItemName(it))
                            ))
                        }
                    }
                })
                blockState = (baseItem.itemMeta as? BlockStateMeta)?.blockState ?: blockState
            }
        }
    }

    private fun createSmartShulker(baseItem: ItemStack, items: List<Material>) =
        createShulker(baseItem, smartShulkerKey, items)

    private fun createGarbageShulker(baseItem: ItemStack, items: List<Material>) =
        createShulker(baseItem, garbageShulkerKey, items)
}