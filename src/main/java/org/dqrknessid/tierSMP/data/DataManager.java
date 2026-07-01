package org.dqrknessid.tierSMP.data;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.dqrknessid.tierSMP.TierSMP;
import org.dqrknessid.tierSMP.tier.Tier;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DataManager {
    private final TierSMP plugin;
    private final File file;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();

    public DataManager(TierSMP plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "players.yml");
    }

    public PlayerData getOrCreate(UUID uuid) {
        return cache.computeIfAbsent(uuid, PlayerData::new);
    }

    public Collection<PlayerData> getAllData() {
        return cache.values();
    }

    public void loadAll() {
        if (!file.exists()) return;
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String key : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                PlayerData data = getOrCreate(uuid);
                data.setScore(config.getInt(key + ".score", 0));
                data.setTier(Tier.valueOf(config.getString(key + ".tier", "UNRANKED")));
                data.setKillStreak(config.getInt(key + ".streak", 0));
                
                List<?> list = config.getList(key + ".einv");
                if (list != null) {
                    List<ItemStack> items = new ArrayList<>();
                    for (Object obj : list) {
                        if (obj instanceof ItemStack) {
                            items.add((ItemStack) obj);
                        }
                    }
                    data.setEinvContents(items);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load player data for key: " + key);
            }
        }
    }

    public void saveAll() {
        FileConfiguration config = new YamlConfiguration();
        for (PlayerData data : cache.values()) {
            String key = data.getUuid().toString();
            config.set(key + ".score", data.getScore());
            config.set(key + ".tier", data.getTier().name());
            config.set(key + ".streak", data.getKillStreak());
            config.set(key + ".einv", data.getEinvContents());
        }
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save players.yml!");
            e.printStackTrace();
        }
    }

    public void clearAll() {
        cache.clear();
        if (file.exists()) {
            file.delete();
        }
    }
}
