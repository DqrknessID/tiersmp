package org.dqrknessid.tierSMP.tasks;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.dqrknessid.tierSMP.TierSMP;
import org.dqrknessid.tierSMP.tier.Tier;

public class SpeedReapplyTask extends BukkitRunnable {
    private final TierSMP plugin;

    public SpeedReapplyTask(TierSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.getConfig().getStringList("disabled-worlds").contains(player.getWorld().getName())) {
                continue;
            }
            Tier tier = plugin.getDataManager().getOrCreate(player.getUniqueId()).getTier();
            if (tier == Tier.S) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, 0, false, false, false));
            }
        }
    }
}
