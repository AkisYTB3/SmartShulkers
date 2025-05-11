package org.notionsmp.smartshulkers

import co.aikar.commands.PaperCommandManager
import net.kyori.adventure.text.minimessage.MiniMessage
import net.milkbowl.vault.economy.Economy
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Tag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ShapelessRecipe
import org.bukkit.plugin.RegisteredServiceProvider
import org.bukkit.plugin.java.JavaPlugin
import org.notionsmp.smartshulkers.commands.ShulkerCommands
import org.notionsmp.smartshulkers.config.ConfigManager
import org.notionsmp.smartshulkers.listeners.*
import org.notionsmp.smartshulkers.utils.Metrics
import org.notionsmp.smartshulkers.utils.ShulkerManager

class SmartShulkers : JavaPlugin() {
    companion object {
        lateinit var instance: SmartShulkers
        val SHULKER_BOX_TYPES = Tag.SHULKER_BOXES.values
    }

    lateinit var configManager: ConfigManager
    lateinit var commandManager: PaperCommandManager
    val mm = MiniMessage.miniMessage()
    var economy: Economy? = null
    private var isVaultUnlocked = false
    private val economySetupDelay = 20L

    lateinit var smartShulkerKey: NamespacedKey
    lateinit var garbageShulkerKey: NamespacedKey
    lateinit var sellShulkerKey: NamespacedKey
    lateinit var itemsKey: NamespacedKey

    private val registeredRecipeKeys = mutableSetOf<NamespacedKey>()
    private var uncraftRecipesRegistered = false

    override fun onEnable() {
        instance = this
        configManager = ConfigManager(this)
        configManager.setup()
        initializeKeys()
        registerManagers()
        registerListeners()
        registerUncraftRecipes()
        registerRecipes()
        checkForEconomy()
        initMetrics()
    }

    private fun initMetrics() {
        val metrics: Metrics = Metrics(this, 25707)
    }

    private fun checkForEconomy() {
        Bukkit.getScheduler().runTaskLater(this, Runnable {
            if (!setupEconomy()) {
                logger.warning("==============================================")
                logger.warning(" VAULT NOT FOUND OR NO ECONOMY PLUGIN INSTALLED!")
                logger.warning(" Sell Shulker features will be disabled.")
                logger.warning("==============================================")
                configManager.disableSellShulkers()
            } else {
                logger.info("Successfully hooked into ${if (isVaultUnlocked) "VaultUnlocked" else "Vault"} economy via ${economy?.name}")
                if (configManager.isSellShulkerEnabled) {
                    removeRecipesByType(sellShulkerKey)
                    registerSellRecipes()
                    registerSellListener()
                }
            }
        }, economySetupDelay)
    }

    private fun setupEconomy(): Boolean {
        val rsp: RegisteredServiceProvider<Economy>? = server.servicesManager.getRegistration(Economy::class.java)
        if (rsp != null) {
            economy = rsp.provider
            if (economy != null) {
                isVaultUnlocked = false
                return true
            }
        }

        try {
            val vaultUnlockedClass = Class.forName("net.milkbowl.vault2.economy.Economy")
            val rspV2 = server.servicesManager.getRegistration(vaultUnlockedClass)
            if (rspV2 != null) {
                economy = rspV2.provider as Economy
                isVaultUnlocked = true
                return true
            }
        } catch (e: ClassNotFoundException) {
            logger.info("VaultUnlocked economy class not found, using standard Vault")
        }

        return false
    }

    private fun initializeKeys() {
        smartShulkerKey = NamespacedKey(this, "smartshulker")
        garbageShulkerKey = NamespacedKey(this, "garbageshulker")
        sellShulkerKey = NamespacedKey(this, "sellshulker")
        itemsKey = NamespacedKey(this, "shulkeritems")
    }

    private fun registerManagers() {
        commandManager = PaperCommandManager(this).apply {
            registerCommand(ShulkerCommands(this@SmartShulkers))
        }
    }

    private fun registerListeners() {
        server.pluginManager.registerEvents(BlockBreakListener(this), this)
        server.pluginManager.registerEvents(InventoryClickListener(this), this)
        server.pluginManager.registerEvents(ItemPickupListener(this), this)
        server.pluginManager.registerEvents(CraftingListener(this), this)
    }

    private fun registerSellListener() {
        server.pluginManager.registerEvents(SellListener(this), this)
    }

    fun reload() {
        configManager.reload()
        removeAllRecipes()
        uncraftRecipesRegistered = false
        registerUncraftRecipes()
        registerRecipes()
    }

    private fun removeAllRecipes() {
        registeredRecipeKeys.forEach { key ->
            Bukkit.removeRecipe(key)
        }
        registeredRecipeKeys.clear()
    }

    private fun removeRecipesByType(baseKey: NamespacedKey) {
        SHULKER_BOX_TYPES.forEach { type ->
            val key = NamespacedKey(this, "${baseKey.key}_${type.name.lowercase()}")
            Bukkit.removeRecipe(key)
            registeredRecipeKeys.remove(key)
        }
    }

    private fun registerRecipes() {
        if (configManager.isSmartShulkerEnabled) registerSmartRecipes()
        if (configManager.isGarbageShulkerEnabled) registerGarbageRecipes()
        if (configManager.isSellShulkerEnabled && economy != null) registerSellRecipes()
    }

    private fun registerSmartRecipes() {
        SHULKER_BOX_TYPES.forEach { shulkerType ->
            val key = NamespacedKey(this, "smartshulker_${shulkerType.name.lowercase()}")
            if (!registeredRecipeKeys.contains(key)) {
                val recipe = ShapelessRecipe(
                    key,
                    ShulkerManager.createSmartShulker(ItemStack(shulkerType), emptyList())
                )
                recipe.addIngredient(Material.BOOK)
                recipe.addIngredient(shulkerType)
                Bukkit.addRecipe(recipe)
                registeredRecipeKeys.add(key)
            }
        }
    }

    private fun registerGarbageRecipes() {
        SHULKER_BOX_TYPES.forEach { shulkerType ->
            val key = NamespacedKey(this, "garbageshulker_${shulkerType.name.lowercase()}")
            if (!registeredRecipeKeys.contains(key)) {
                val recipe = ShapelessRecipe(
                    key,
                    ShulkerManager.createGarbageShulker(ItemStack(shulkerType), emptyList())
                )
                recipe.addIngredient(Material.LAVA_BUCKET)
                recipe.addIngredient(shulkerType)
                Bukkit.addRecipe(recipe)
                registeredRecipeKeys.add(key)
            }
        }
    }

    private fun registerSellRecipes() {
        SHULKER_BOX_TYPES.forEach { shulkerType ->
            val key = NamespacedKey(this, "sellshulker_${shulkerType.name.lowercase()}")
            if (!registeredRecipeKeys.contains(key)) {
                val recipe = ShapelessRecipe(
                    key,
                    ShulkerManager.createSellShulker(ItemStack(shulkerType), emptyList())
                )
                recipe.addIngredient(Material.EMERALD)
                recipe.addIngredient(shulkerType)
                Bukkit.addRecipe(recipe)
                registeredRecipeKeys.add(key)
            }
        }
    }

    private fun registerUncraftRecipes() {
        if (uncraftRecipesRegistered) return

        SHULKER_BOX_TYPES.forEach { shulkerType ->
            val smartKey = NamespacedKey(this, "uncraft_smartshulker_${shulkerType.name.lowercase()}")
            val garbageKey = NamespacedKey(this, "uncraft_garbageshulker_${shulkerType.name.lowercase()}")
            val sellKey = NamespacedKey(this, "uncraft_sellshulker_${shulkerType.name.lowercase()}")

            if (!registeredRecipeKeys.contains(smartKey)) {
                Bukkit.addRecipe(ShapelessRecipe(smartKey, ItemStack(shulkerType))
                    .apply { addIngredient(shulkerType) }
                )
                registeredRecipeKeys.add(smartKey)
            }

            if (!registeredRecipeKeys.contains(garbageKey)) {
                Bukkit.addRecipe(ShapelessRecipe(garbageKey, ItemStack(shulkerType))
                    .apply { addIngredient(shulkerType) }
                )
                registeredRecipeKeys.add(garbageKey)
            }

            if (!registeredRecipeKeys.contains(sellKey)) {
                Bukkit.addRecipe(ShapelessRecipe(sellKey, ItemStack(shulkerType))
                    .apply { addIngredient(shulkerType) }
                )
                registeredRecipeKeys.add(sellKey)
            }
        }
        uncraftRecipesRegistered = true
    }
}