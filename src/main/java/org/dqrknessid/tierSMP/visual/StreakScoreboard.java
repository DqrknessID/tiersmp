package org.dqrknessid.tierSMP.visual;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.dqrknessid.tierSMP.TierSMP;
import org.dqrknessid.tierSMP.data.PlayerData;
import org.dqrknessid.tierSMP.tier.Tier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class StreakScoreboard {
    private final TierSMP plugin;
    private final List<String> activeEntries = new ArrayList<>();

    public StreakScoreboard(TierSMP plugin) {
        this.plugin = plugin;
    }

    public void update() {
        Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();

        List<PlayerData> sPlayers = new ArrayList<>();
        for (PlayerData d : plugin.getDataManager().getAllData()) {
            if (d.getTier() == Tier.S) {
                sPlayers.add(d);
            }
        }

        Objective obj = sb.getObjective("tsmp_streak");

        if (sPlayers.isEmpty()) {
            if (obj != null) {
                obj.unregister();
                activeEntries.clear();
            }
            return;
        }

        sPlayers.sort(Comparator.comparingInt(PlayerData::getKillStreak).reversed()
                .thenComparing(Comparator.comparingInt(PlayerData::getScore).reversed()));

        if (obj == null) {
            obj = sb.registerNewObjective("tsmp_streak", Criteria.DUMMY, LegacyComponentSerializer.legacyAmpersand().deserialize("&6&l★ S-Tier Streaks ★"));
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        } else {
            if (obj.getDisplaySlot() != DisplaySlot.SIDEBAR) {
                obj.setDisplaySlot(DisplaySlot.SIDEBAR);
            }
        }

        // Clear previous active entries
        for (String entry : activeEntries) {
            sb.resetScores(entry);
        }
        activeEntries.clear();

        int limit = Math.min(5, sPlayers.size());
        for (int i = 0; i < limit; i++) {
            PlayerData d = sPlayers.get(i);
            OfflinePlayer op = Bukkit.getOfflinePlayer(d.getUuid());
            String name = op.getName() != null ? op.getName() : "Unknown";
            String entryKey = "§e#" + (i + 1) + " §f" + name + "§7: §6" + d.getKillStreak() + " §7kills";
            obj.getScore(entryKey).setScore(limit - i);
            activeEntries.add(entryKey);
        }

        // Ensure all online players have the main scoreboard attached
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getScoreboard() != sb) {
                p.setScoreboard(sb);
            }
        }
    }
}
