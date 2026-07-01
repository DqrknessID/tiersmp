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
        if (isDisabled(player)) return;

        Tier tier = plugin.getDataManager().getOrCreate(player.getUniqueId()).getTier();
        double multiplier = 1.0;

        switch (tier) {
            case S: multiplier = 1.4; break;
            case A: multiplier = 1.25; break;
            case B: multiplier = 1.1; break;
            default: break;
        }

        if (multiplier > 1.0) {
            int newAmount = (int) Math.round(event.getAmount() * multiplier);
            event.setAmount(newAmount);
        }
    }
}
