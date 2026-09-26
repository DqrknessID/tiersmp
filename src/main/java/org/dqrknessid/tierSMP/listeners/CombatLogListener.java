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
        int loss = plugin.getConfig().getInt("score-losses.death-by-same", 15);
        data.setScore(Math.max(0, data.getScore() - loss));
        data.setKillStreak(0);

        // Recalculate tier
        plugin.getTierManager().recalculateTier(data, true, true);

        // Drop player main inventory + armor + offhand at logout location
        org.bukkit.Location loc = player.getLocation();
        if (loc.getWorld() != null) {
            for (org.bukkit.inventory.ItemStack item : player.getInventory().getContents()) {
                if (item != null && !item.getType().isAir()) {
                    loc.getWorld().dropItemNaturally(loc, item.clone());
                }
            }
        }
        player.getInventory().clear();

        // Drop extra inventory contents at logout location
        plugin.getExtraInventoryManager().dropExtraInventory(uuid, loc);

        // Kill the player so they are dead on reconnect
        player.setHealth(0.0);

        // Broadcast to server
        plugin.getVisualManager().broadcastMessage("combat-log-broadcast", "&c[TierSMP] {player} combat logged!", player, data.getTier().name());

        plugin.debug("🚨 Combat Log: §c" + player.getName() + " §7combat logged! (Inventory dropped, einv dropped, health=0, score -" + loss + ")");

        // Save data immediately
        plugin.getDataManager().saveAll();
    }
}
