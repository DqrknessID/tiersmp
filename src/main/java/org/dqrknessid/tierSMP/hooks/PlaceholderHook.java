package org.dqrknessid.tierSMP.hooks;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.dqrknessid.tierSMP.TierSMP;
import org.dqrknessid.tierSMP.data.PlayerData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class PlaceholderHook extends PlaceholderExpansion {
    private final TierSMP plugin;

    public PlaceholderHook(TierSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "tiersmp";
    }

    @Override
    public String getAuthor() {
        return "dqrknessid";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null) return "";

        PlayerData data = plugin.getDataManager().getOrCreate(player.getUniqueId());

        switch (params.toLowerCase()) {
            case "tier":
                return data.getTier().name();
            case "score":
                return String.valueOf(data.getScore());
            case "streak":
                return String.valueOf(data.getKillStreak());
            case "rank":
                return String.valueOf(getRank(player.getUniqueId()));
            default:
                return null;
        }
    }

    private int getRank(UUID uuid) {
        List<PlayerData> list = new ArrayList<>(plugin.getDataManager().getAllData());
        list.sort(Comparator.comparingInt(PlayerData::getScore).reversed());
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getUuid().equals(uuid)) {
                return i + 1;
            }
        }
        return list.size() + 1;
    }
}
