package org.dqrknessid.tierSMP.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.dqrknessid.tierSMP.TierSMP;
import org.dqrknessid.tierSMP.data.PlayerData;
import org.dqrknessid.tierSMP.tier.Tier;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class CombatListener implements Listener {
    private final TierSMP plugin;
    private final Map<UUID, Long> combatTags = new HashMap<>();
    private final Map<String, Long> killCooldowns = new HashMap<>();

    public CombatListener(TierSMP plugin) {
        this.plugin = plugin;
    }

    public Map<UUID, Long> getCombatTags() {
        return combatTags;
    }

    private boolean isDisabled(Player p) {
        return plugin.getConfig().getStringList("disabled-worlds").contains(p.getWorld().getName());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) return;
        Player victim = (Player) event.getEntity();
        Player attacker = (Player) event.getDamager();

        if (isDisabled(victim) || isDisabled(attacker)) return;

        long expiry = System.currentTimeMillis() + (plugin.getConfig().getInt("combat-tag-seconds", 15) * 1000L);
        combatTags.put(victim.getUniqueId(), expiry);
        combatTags.put(attacker.getUniqueId(), expiry);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        
        // Drop extra inventory and reset streak on death
        plugin.getExtraInventoryManager().dropExtraInventory(victim.getUniqueId(), victim.getLocation());
        PlayerData victimData = plugin.getDataManager().getOrCreate(victim.getUniqueId());
        victimData.setKillStreak(0);

        Player killer = victim.getKiller();
        if (killer == null || isDisabled(victim) || isDisabled(killer)) {
            plugin.getTierManager().recalculateTier(victimData, true, true);
            return;
        }

        // Clean old cooldown entries to prevent memory leak
        cleanCooldowns();

        // Increment killer streak (cooldown doesn't block streak)
        PlayerData killerData = plugin.getDataManager().getOrCreate(killer.getUniqueId());
        killerData.setKillStreak(killerData.getKillStreak() + 1);

        // Check bidirectional kill cooldown
        String pairKey1 = killer.getUniqueId() + "_" + victim.getUniqueId();
        long now = System.currentTimeMillis();
        long cooldownMs = plugin.getConfig().getInt("kill-cooldown-minutes", 5) * 60000L;

        if (killCooldowns.containsKey(pairKey1) && (now - killCooldowns.get(pairKey1)) < cooldownMs) {
            plugin.getVisualManager().sendMessage(killer, "kill-cooldown-message", "&c[TierSMP] Kill not counted — cooldown active.");
            plugin.getTierManager().recalculateTier(victimData, true, true); // this is a death
            plugin.getTierManager().recalculateTier(killerData, true, false); // not killer's death
            return;
        }

        // Apply cooldown bidirectionally
        String pairKey2 = victim.getUniqueId() + "_" + killer.getUniqueId();
        killCooldowns.put(pairKey1, now);
        killCooldowns.put(pairKey2, now);

        // Calculate score delta
        applyScoreChanges(killer, victim, killerData, victimData);
    }

    private void applyScoreChanges(Player killer, Player victim, PlayerData killerData, PlayerData victimData) {
        Tier killerTier = killerData.getTier();
        Tier victimTier = victimData.getTier();

        int gains;
        int losses;

        if (killerTier == Tier.UNRANKED) {
            gains = plugin.getConfig().getInt("score-gains.kill-higher", 5);
            losses = plugin.getConfig().getInt("score-losses.death-by-higher", 3);
        } else if (victimTier == Tier.UNRANKED) {
            gains = plugin.getConfig().getInt("score-gains.kill-higher", 5);
            losses = plugin.getConfig().getInt("score-losses.death-by-higher", 3);
        } else {
            int comp = Integer.compare(killerTier.ordinal(), victimTier.ordinal()); // ordinal: S=0, A=1, B=2, C=3, UNRANKED=4
            if (comp < 0) { // killer ordinal is lower, meaning killer tier is higher
                gains = plugin.getConfig().getInt("score-gains.kill-higher", 5);
                losses = plugin.getConfig().getInt("score-losses.death-by-higher", 3);
            } else if (comp == 0) {
                gains = plugin.getConfig().getInt("score-gains.kill-same", 10);
                losses = plugin.getConfig().getInt("score-losses.death-by-same", 5);
            } else {
                gains = plugin.getConfig().getInt("score-gains.kill-lower", 20);
                losses = plugin.getConfig().getInt("score-losses.death-by-lower", 10);
            }
        }

        killerData.setScore(killerData.getScore() + gains);
        victimData.setScore(victimData.getScore() - losses);

        plugin.getTierManager().recalculateTier(victimData, true, true);
        plugin.getTierManager().recalculateTier(killerData, true, false);
    }

    private void cleanCooldowns() {
        long now = System.currentTimeMillis();
        long cooldownMs = plugin.getConfig().getInt("kill-cooldown-minutes", 5) * 60000L;
        Iterator<Map.Entry<String, Long>> it = killCooldowns.entrySet().iterator();
        while (it.hasNext()) {
            if (now - it.next().getValue() >= cooldownMs) {
                it.remove();
            }
        }
    }
}
