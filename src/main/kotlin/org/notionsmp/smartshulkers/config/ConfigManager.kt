package org.notionsmp.smartshulkers.config

import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.ItemStack
import org.notionsmp.smartshulkers.SmartShulkers
import java.io.File

class ConfigManager(private val plugin: SmartShulkers) {
    private lateinit var pricesConfig: YamlConfiguration

    val isSmartShulkerEnabled get() = plugin.config.getBoolean("settings.smartshulker.enabled", true)
    val isGarbageShulkerEnabled get() = plugin.config.getBoolean("settings.garbageshulker.enabled", true)
    val isSellShulkerEnabled get() = plugin.config.getBoolean("settings.sellshulker.enabled", true) && plugin.economy != null

    fun setup() {
        plugin.saveDefaultConfig()
        pricesConfig = setupPricesConfig()
    }

    fun reload() {
        plugin.reloadConfig()
        pricesConfig = setupPricesConfig()
    }

    fun disableSellShulkers() {
        plugin.config.set("settings.sellshulker.enabled", false)
        plugin.saveConfig()
    }

    private fun setupPricesConfig(): YamlConfiguration {
        val file = File(plugin.dataFolder, "prices.yml")
        if (!file.exists()) {
            plugin.saveResource("prices.yml", false)
        }
        return YamlConfiguration.loadConfiguration(file)
    }

    fun canSellItem(material: Material): Boolean {
        if (pricesConfig.contains(material.name)) return true

        return pricesConfig.getKeys(false).any { key ->
            key.startsWith("${material.name}x") &&
                    key.substringAfter("x").toIntOrNull() != null
        }
    }

    fun getString(path: String): String? = plugin.config.getString(path)
    fun getDouble(path: String, default: Double = 0.0) = plugin.config.getDouble(path, default)

    fun getPrice(material: String): Double {
        val exactPrice = pricesConfig.getDouble(material, -1.0)
        if (exactPrice >= 0) return exactPrice

        val multiplierEntries = pricesConfig.getKeys(false).filter {
            it.startsWith("${material}x") && it.substringAfter("x").toIntOrNull() != null
        }

        for (entry in multiplierEntries) {
            val multiplier = entry.substringAfter("x").toInt()
            if (multiplier > 0) {
                val totalPrice = pricesConfig.getDouble(entry)
                return totalPrice / multiplier
            }
        }

        return 0.0
    }

    fun shouldIgnoreItem(item: ItemStack): Boolean {
        val meta = item.itemMeta ?: return false
        val config = plugin.config.getConfigurationSection("settings.general.ignore-items") ?: return false

        if (config.getBoolean("with-name") && meta.hasDisplayName()) return true
        if (config.getBoolean("with-lore") && meta.hasLore()) return true
        if (config.getBoolean("with-cmd") && meta.hasCustomModelData()) return true

        val namedItems = config.getStringList("named")
        if (meta.hasDisplayName()) {
            val displayComponent = meta.displayName() ?: return false
            val plainText = plugin.mm.serialize(displayComponent)
            return namedItems.any { pattern ->
                try {
                    plainText.matches(Regex(pattern))
                } catch (e: Exception) {
                    plainText.contains(pattern, ignoreCase = true)
                }
            }
        }

        return false
    }
}