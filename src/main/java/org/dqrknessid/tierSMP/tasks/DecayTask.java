package org.dqrknessid.tierSMP.tasks;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.dqrknessid.tierSMP.TierSMP;
import org.dqrknessid.tierSMP.data.PlayerData;

public class DecayTask extends BukkitRunnable {
    private final TierSMP plugin;

    public DecayTask(TierSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        double percent = plugin.getConfig().getDouble("decay-percent", 2.0) / 100.0;
        boolean changed = false;

        for (PlayerData data : plugin.getDataManager().getAllData()) {
            int score = data.getScore();
            if (score > 0) {
                int decayAmount = (int) Math.floor(score * percent);
                if (decayAmount > 0) {
                    data.setScore(score - decayAmount);
                    changed = true;
                }
            }
        }

        if (changed) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.getTierManager().recalculateAll(true);
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> plugin.getDataManager().saveAll());
            });
        }
    }
}
