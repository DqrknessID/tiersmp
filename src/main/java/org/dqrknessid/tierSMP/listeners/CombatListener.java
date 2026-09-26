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

        int tagSec = plugin.getConfig().getInt("combat-tag-seconds", 15);
        long expiry = System.currentTimeMillis() + (tagSec * 1000L);
        combatTags.put(victim.getUniqueId(), expiry);
        combatTags.put(attacker.getUniqueId(), expiry);

        plugin.debug("⚔ Combat Tag: §e" + attacker.getName() + " §7attacked §c" + victim.getName() + " §7(Tagged for " + tagSec + "s)");
    }

    private boolean killCooldownEnabled = true;

    public boolean isKillCooldownEnabled() {
        return killCooldownEnabled;
    }

    public void setKillCooldownEnabled(boolean enabled) {
        this.killCooldownEnabled = enabled;
    }

    public boolean toggleKillCooldown() {
        this.killCooldownEnabled = !this.killCooldownEnabled;
        return this.killCooldownEnabled;
    }

    public void clearAllKillCooldowns() {
        killCooldowns.clear();
    }

    public void clearKillCooldown(UUID uuid) {
        String prefix = uuid.toString();
        killCooldowns.entrySet().removeIf(entry -> entry.getKey().contains(prefix));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        
        // Drop extra inventory and reset streak on death
        plugin.getExtraInventoryManager().dropExtraInventory(victim.getUniqueId(), victim.getLocation(), event.getDrops());
        PlayerData victimData = plugin.getDataManager().getOrCreate(victim.getUniqueId());
        victimData.setKillStreak(0);

        Player killer = victim.getKiller();
        if (killer == null || isDisabled(victim) || isDisabled(killer)) {
            plugin.debug("💀 PvE Death: §c" + victim.getName() + " §7died (no player killer). Streak reset to 0.");
            plugin.getStreakScoreboard().update();
            plugin.getTierManager().recalculateTier(victimData, true, true);
            return;
        }

        plugin.debug("💀 PvP Kill: §a" + killer.getName() + " §7killed §c" + victim.getName());

        // Clean old cooldown entries to prevent memory leak
        cleanCooldowns();

        // Increment killer streak (cooldown doesn't block streak)
        PlayerData killerData = plugin.getDataManager().getOrCreate(killer.getUniqueId());
        killerData.setKillStreak(killerData.getKillStreak() + 1);
        plugin.debug("🔥 Streak Updated: §a" + killer.getName() + " §7now on §6" + killerData.getKillStreak() + " kill streak!");
        plugin.getStreakScoreboard().update();

        // Check bidirectional kill cooldown if enabled
        String pairKey1 = killer.getUniqueId() + "_" + victim.getUniqueId();
        long now = System.currentTimeMillis();
        long cooldownMs = plugin.getConfig().getInt("kill-cooldown-minutes", 5) * 60000L;

        if (killCooldownEnabled && killCooldowns.containsKey(pairKey1) && (now - killCooldowns.get(pairKey1)) < cooldownMs) {
            plugin.debug("⏳ Kill Cooldown Active: Kill between §a" + killer.getName() + " §7and §c" + victim.getName() + " §7awarded 0 score.");
            plugin.getVisualManager().sendMessage(killer, "kill-cooldown-message", "&c[TierSMP] Kill not counted — cooldown active.");
            plugin.getStreakScoreboard().update();
            plugin.getTierManager().recalculateTier(victimData, true, true); // this is a death
            plugin.getTierManager().recalculateTier(killerData, true, false); // not killer's death
            return;
        }

        // Apply cooldown bidirectionally if enabled
        if (killCooldownEnabled) {
            String pairKey2 = victim.getUniqueId() + "_" + killer.getUniqueId();
            killCooldowns.put(pairKey1, now);
            killCooldowns.put(pairKey2, now);
        }

        // Calculate score delta
        applyScoreChanges(killer, victim, killerData, victimData);
    }

    private void applyScoreChanges(Player killer, Player victim, PlayerData killerData, PlayerData victimData) {
        Tier killerTier = killerData.getTier();
        Tier victimTier = victimData.getTier();

        int gains;
        int losses;

        boolean dynamicScaling = plugin.getConfig().getBoolean("dynamic-tier-scaling", true);

        if (dynamicScaling) {
            // ordinal: S=0, A=1, B=2, C=3, UNRANKED=4
            // tierGap > 0 -> Killer has lower tier than victim (underdog kill: e.g. A killed S -> tierGap = 1)
            // tierGap < 0 -> Killer has higher tier than victim (top dog kill: e.g. S killed A -> tierGap = -1)
            int tierGap = killerTier.ordinal() - victimTier.ordinal();

            int baseGain = plugin.getConfig().getInt("score-scaling.base-gain", 30);
            int baseLoss = plugin.getConfig().getInt("score-scaling.base-loss", 15);
            int gainBonusPerTier = plugin.getConfig().getInt("score-scaling.gain-bonus-per-tier", 10);
            int gainReductionPerTier = plugin.getConfig().getInt("score-scaling.gain-reduction-per-tier", 5);
            int lossBonusPerTier = plugin.getConfig().getInt("score-scaling.loss-bonus-per-tier", 5);
            int lossReductionPerTier = plugin.getConfig().getInt("score-scaling.loss-reduction-per-tier", 3);
            int minGain = plugin.getConfig().getInt("score-scaling.min-gain", 5);
            int minLoss = plugin.getConfig().getInt("score-scaling.min-loss", 3);

            if (tierGap > 0) {
                // Killer killed a HIGHER tier player (underdog kill)
                gains = baseGain + (tierGap * gainBonusPerTier);
                losses = baseLoss + (tierGap * lossBonusPerTier);
            } else if (tierGap < 0) {
                // Killer killed a LOWER tier player
                int gap = Math.abs(tierGap);
                gains = Math.max(minGain, baseGain - (gap * gainReductionPerTier));
                losses = Math.max(minLoss, baseLoss - (gap * lossReductionPerTier));
            } else {
                // Same tier kill
                gains = baseGain;
                losses = baseLoss;
            }

            plugin.debug("📊 Dynamic Math: Killer=§a" + killer.getName() + " §7(" + killerTier + "), Victim=§c" + victim.getName() + " §7(" + victimTier + ") | Gap: " + tierGap + " | +§a" + gains + "§7/-§c" + losses);
        } else {
            // Fallback to flat tiers
            int comp = Integer.compare(killerTier.ordinal(), victimTier.ordinal());
            if (comp < 0) {
                // Killer has higher tier than victim
                gains = plugin.getConfig().getInt("score-gains.kill-lower", 15);
                losses = plugin.getConfig().getInt("score-losses.death-by-higher", 9);
            } else if (comp == 0) {
                // Same tier kill
                gains = plugin.getConfig().getInt("score-gains.kill-same", 30);
                losses = plugin.getConfig().getInt("score-losses.death-by-same", 15);
            } else {
                // Killer has lower tier than victim
                gains = plugin.getConfig().getInt("score-gains.kill-higher", 60);
                losses = plugin.getConfig().getInt("score-losses.death-by-lower", 30);
            }

            plugin.debug("📊 Flat Math: Killer=§a" + killer.getName() + " §7(" + killerTier + "), Victim=§c" + victim.getName() + " §7(" + victimTier + ") | +§a" + gains + "§7/-§c" + losses);
        }

        killerData.setScore(killerData.getScore() + gains);
        victimData.setScore(Math.max(0, victimData.getScore() - losses));

        plugin.debug("📈 Scores Updated: §a" + killer.getName() + "§7=" + killerData.getScore() + " | §c" + victim.getName() + "§7=" + victimData.getScore());

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
