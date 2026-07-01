package org.dqrknessid.tierSMP.listeners;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.dqrknessid.tierSMP.TierSMP;

public class PlayerListener implements Listener {
    private final TierSMP plugin;

    public PlayerListener(TierSMP plugin) {
        this.plugin = plugin;
    }

    private boolean isDisabled(String worldName) {
        return plugin.getConfig().getStringList("disabled-worlds").contains(worldName);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getDataManager().getOrCreate(player.getUniqueId());
        plugin.getVisualManager().updateNametag(player);
        plugin.getBenefitManager().applyBenefits(player);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                plugin.getBenefitManager().applyBenefits(player);
                plugin.getVisualManager().updateNametag(player);
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        World from = event.getFrom();
        World to = player.getWorld();

        boolean toDisabled = isDisabled(to.getName());
        boolean fromDisabled = isDisabled(from.getName());

        if (toDisabled) {
            Long expiry = plugin.getCombatListener().getCombatTags().get(player.getUniqueId());
            if (expiry != null && expiry > System.currentTimeMillis()) {
                plugin.getCombatLogListener().triggerCombatLogPenalty(player);
                plugin.getCombatListener().getCombatTags().remove(player.getUniqueId());
            } else {
                plugin.getBenefitManager().removeBenefits(player);
                plugin.getVisualManager().updateNametag(player);
            }
        } else if (fromDisabled) {
            plugin.getBenefitManager().applyBenefits(player);
            plugin.getVisualManager().updateNametag(player);
        }
    }
}
