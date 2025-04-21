package org.notionsmp.smartshulkers.config

import org.bukkit.configuration.file.YamlConfiguration
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

    fun getString(path: String): String? = plugin.config.getString(path)
    fun getDouble(path: String, default: Double = 0.0) = plugin.config.getDouble(path, default)
    fun getPrice(material: String) = pricesConfig.getDouble(material, 0.0)
}