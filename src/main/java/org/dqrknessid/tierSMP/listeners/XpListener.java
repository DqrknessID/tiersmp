package org.dqrknessid.tierSMP.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.dqrknessid.tierSMP.TierSMP;
import org.dqrknessid.tierSMP.tier.Tier;

public class XpListener implements Listener {
    private final TierSMP plugin;

    public XpListener(TierSMP plugin) {
        this.plugin = plugin;
    }

    private boolean isDisabled(Player p) {
        return plugin.getConfig().getStringList("disabled-worlds").contains(p.getWorld().getName());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerExp(PlayerExpChangeEvent event) {
        Player player = event.getPlayer();
        if (plugin.isTournamentMode() || isDisabled(player)) return;

        Tier tier = plugin.getDataManager().getOrCreate(player.getUniqueId()).getTier();
        double multiplier = plugin.getConfig().getDouble("xp-multipliers." + tier.name().toLowerCase(), getDefaultMultiplier(tier));

        if (multiplier > 1.0) {
            int original = event.getAmount();
            int newAmount = (int) Math.round(original * multiplier);
            event.setAmount(newAmount);
            plugin.debug("✨ XP Boost: §e" + player.getName() + " §7(" + tier + ") got §b" + newAmount + " XP §7(Original: " + original + " XP, " + multiplier + "x)");
        }
    }

    private double getDefaultMultiplier(Tier tier) {
        switch (tier) {
            case S: return 2.0;
            case A: return 1.5;
            case B: return 1.25;
            default: return 1.0;
        }
    }
}
