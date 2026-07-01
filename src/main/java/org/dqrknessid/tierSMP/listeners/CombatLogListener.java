package org.dqrknessid.tierSMP.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.dqrknessid.tierSMP.TierSMP;
import org.dqrknessid.tierSMP.data.PlayerData;

import java.util.UUID;

public class CombatLogListener implements Listener {
    private final TierSMP plugin;

    public CombatLogListener(TierSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        Long expiry = plugin.getCombatListener().getCombatTags().get(uuid);
        if (expiry != null && expiry > System.currentTimeMillis()) {
            triggerCombatLogPenalty(player);
        }
        plugin.getCombatListener().getCombatTags().remove(uuid);
    }

    public void triggerCombatLogPenalty(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerData data = plugin.getDataManager().getOrCreate(uuid);

        // Calculate score loss as if they died to same tier
        int loss = plugin.getConfig().getInt("score-losses.death-by-same", 5);
        data.setScore(data.getScore() - loss);
        data.setKillStreak(0);

        // Recalculate tier
        plugin.getTierManager().recalculateTier(data, true, true);

        // Drop extra inventory contents at last location
        plugin.getExtraInventoryManager().dropExtraInventory(uuid, player.getLocation());

        // Broadcast to server
        plugin.getVisualManager().broadcastMessage("combat-log-broadcast", "&c[TierSMP] {player} combat logged!", player, data.getTier().name());

        // Save data immediately
        plugin.getDataManager().saveAll();
    }
}
