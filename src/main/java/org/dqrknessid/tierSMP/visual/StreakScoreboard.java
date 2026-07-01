package org.dqrknessid.tierSMP.visual;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
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
        // Skip scoreboard update if no S tier players exist
        boolean hasSTier = false;
        for (PlayerData d : plugin.getDataManager().getAllData()) {
            if (d.getTier() == Tier.S) {
                hasSTier = true;
                break;
            }
        }

        Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
        Objective obj = sb.getObjective("tsmp_streak");

        if (!hasSTier) {
            if (obj != null) {
                obj.unregister();
                activeEntries.clear();
            }
            return;
        }

        List<PlayerData> sPlayers = new ArrayList<>();
        for (PlayerData d : plugin.getDataManager().getAllData()) {
            if (d.getTier() == Tier.S && d.getKillStreak() > 0) {
                sPlayers.add(d);
            }
        }

        if (sPlayers.isEmpty()) {
            if (obj != null) {
                obj.unregister();
                activeEntries.clear();
            }
            return;
        }

        sPlayers.sort(Comparator.comparingInt(PlayerData::getKillStreak).reversed());

        if (obj == null) {
            obj = sb.registerNewObjective("tsmp_streak", Criteria.DUMMY, Component.text("S-Tier Streaks", NamedTextColor.GOLD));
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        // Clear previous active entries
        for (String entry : activeEntries) {
            sb.resetScores(entry);
        }
        activeEntries.clear();

        int limit = Math.min(3, sPlayers.size());
        for (int i = 0; i < limit; i++) {
            PlayerData d = sPlayers.get(i);
            OfflinePlayer op = Bukkit.getOfflinePlayer(d.getUuid());
            String name = op.getName() != null ? op.getName() : "Unknown";
            String entryText = "#" + (i + 1) + " " + name + ": " + d.getKillStreak() + " kills";
            obj.getScore(entryText).setScore(3 - i);
            activeEntries.add(entryText);
        }
    }
}
