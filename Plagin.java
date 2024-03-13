import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MobHealthPlugin extends JavaPlugin implements Listener {

    private Map<String, Double> extraEffectChances;
    private Map<String, Double> healthMultipliers;
    private Map<String, List<String>> mobEffects;
    private Random random;
    private String mobInfoMessage;

    @Override
    public void onEnable() {
        // Регистрация слушателя событий
        getServer().getPluginManager().registerEvents(this, this);
        // Загрузка конфигурации
        loadConfig();
        // Регистрация команд
        getCommand("mobhealth").setExecutor(this);
        // Инициализация логирования
        getLogger().info("MobHealthPlugin has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("MobHealthPlugin has been disabled!");
    }

    // Загрузка конфигурации
    private void loadConfig() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            saveResource("config.yml", false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        extraEffectChances = new HashMap<>();
        healthMultipliers = new HashMap<>();
        mobEffects = new HashMap<>();
        ConfigurationSection extraEffectsSection = config.getConfigurationSection("extraEffectChances");
        if (extraEffectsSection != null) {
            for (String mobType : extraEffectsSection.getKeys(false)) {
                extraEffectChances.put(mobType.toUpperCase(), config.getDouble("extraEffectChances." + mobType));
            }
        }
        ConfigurationSection healthMultipliersSection = config.getConfigurationSection("healthMultipliers");
        if (healthMultipliersSection != null) {
            for (String mobType : healthMultipliersSection.getKeys(false)) {
                healthMultipliers.put(mobType.toUpperCase(), config.getDouble("healthMultipliers." + mobType));
            }
        }
        ConfigurationSection mobEffectsSection = config.getConfigurationSection("mobEffects");
        if (mobEffectsSection != null) {
            for (String mobType : mobEffectsSection.getKeys(false)) {
                mobEffects.put(mobType.toUpperCase(), config.getStringList("mobEffects." + mobType));
            }
        }
        random = new Random();
        mobInfoMessage = config.getString("mobInfoMessage", "Mob: %mob%, Health: %health%, Effects: %effects%");
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof LivingEntity) {
            LivingEntity livingEntity = (LivingEntity) entity;
            String entityType = livingEntity.getType().name().toUpperCase();
            if (healthMultipliers.containsKey(entityType)) {
                // Увеличение здоровья моба в соответствии с настройками
                double currentMaxHealth = livingEntity.getMaxHealth();
                livingEntity.setMaxHealth(currentMaxHealth * healthMultipliers.get(entityType));
                // Применение дополнительных эффектов к мобу
                applyExtraEffects(livingEntity, entityType);
            }
        }
    }

    // Применение дополнительных эффектов к мобу
    private void applyExtraEffects(LivingEntity entity, String entityType) {
        if (extraEffectChances.containsKey(entityType)) {
            double chance = extraEffectChances.get(entityType);
            if (random.nextDouble() < chance) {
                List<String> effects = mobEffects.get(entityType);
                if (effects != null && !effects.isEmpty()) {
                    for (String effect : effects) {
                        String[] effectData = effect.split(":");
                        if (effectData.length == 2) {
                            PotionEffectType potionEffectType = PotionEffectType.getByName(effectData[0]);
                            if (potionEffectType != null) {
                                int duration = 20 * 60 * 5; // 5 minutes
                                int amplifier = Integer.parseInt(effectData[1]);
                                entity.addPotionEffect(new PotionEffect(potionEffectType, duration, amplifier));
                            }
                        }
                    }
                }
            }
        }
    }

    // Обработка команды /mobhealth
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("mobhealth")) {
            if (args.length == 0) {
                sender.sendMessage("Usage: /mobhealth <mob>");
                return true;
            } else {
                // Показать информацию о мобе
                showMobInfo(sender, args[0]);
                return true;
            }
        }
        return false;
    }

    // Отображение информации о мобе
    private void showMobInfo(CommandSender sender, String mobType) {
        String formattedMobType = mobType.toUpperCase();
        if (healthMultipliers.containsKey(formattedMobType)) {
            double healthMultiplier = healthMultipliers.get(formattedMobType);
            List<String> effects = mobEffects.getOrDefault(formattedMobType, null);
            String effectsString = (effects != null && !effects.isEmpty()) ? String.join(", ", effects) : "None";
            String message = mobInfoMessage.replace("%mob%", mobType)
                    .replace("%health%", String.valueOf(healthMultiplier))
                    .replace("%effects%", effectsString);
            sender.sendMessage(message);
        } else {
            sender.sendMessage("Unknown mob type: " + mobType);
        }
    }
}
