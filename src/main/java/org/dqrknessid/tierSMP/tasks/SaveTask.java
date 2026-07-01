package org.dqrknessid.tierSMP.tasks;

import org.bukkit.scheduler.BukkitRunnable;
import org.dqrknessid.tierSMP.TierSMP;

public class SaveTask extends BukkitRunnable {
    private final TierSMP plugin;

    public SaveTask(TierSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        plugin.getDataManager().saveAll();
    }
}
