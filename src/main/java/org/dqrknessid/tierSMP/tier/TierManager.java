package org.dqrknessid.tierSMP.tier;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.dqrknessid.tierSMP.TierSMP;
import org.dqrknessid.tierSMP.data.PlayerData;

import java.util.*;

public class TierManager {
    private final TierSMP plugin;

    public TierManager(TierSMP plugin) {
        this.plugin = plugin;
    }

    public int getTierCount(Tier tier) {
        int count = 0;
        for (PlayerData d : plugin.getDataManager().getAllData()) {
            if (d.getTier() == tier) {
                count++;
            }
        }
        return count;
    }

    public int getTierCap(Tier tier) {
        if (tier == Tier.UNRANKED) return Integer.MAX_VALUE;
        return plugin.getConfig().getInt("tier-caps." + tier.name().toLowerCase(), getDefaultCap(tier));
    }

    private int getDefaultCap(Tier tier) {
        switch (tier) {
            case S: return 3;
            case A: return 7;
            case B: return 10;
            case C: return 10;
            default: return Integer.MAX_VALUE;
        }
    }

    private final Map<UUID, Long> sDemotionCooldowns = new HashMap<>();

    public boolean isSDemotionOnCooldown(UUID uuid) {
        if (!sDemotionCooldowns.containsKey(uuid)) return false;
        long expiry = sDemotionCooldowns.get(uuid);
        if (System.currentTimeMillis() >= expiry) {
            sDemotionCooldowns.remove(uuid);
            return false;
        }
        return true;
    }

    public void startSDemotionCooldown(UUID uuid) {
        long cooldownMs = plugin.getConfig().getInt("s-demotion-cooldown-seconds", 60) * 1000L;
        sDemotionCooldowns.put(uuid, System.currentTimeMillis() + cooldownMs);
    }

    public void clearSDemotionCooldown(UUID uuid) {
        sDemotionCooldowns.remove(uuid);
    }

    public boolean hasSlotAvailable(Tier tier) {
        return getTierCount(tier) < getTierCap(tier);
    }

    public boolean hasSlotAvailable(Tier tier, PlayerData player) {
        if (tier == Tier.S) {
            if (isSDemotionOnCooldown(player.getUuid())) {
                return false;
            }
        }
        return getTierCount(tier) < getTierCap(tier);
    }

    public Tier calculateTier(PlayerData data) {
        Tier current = data.getTier();
        Tier natural = getNaturalTier(data.getScore());

        // If natural tier is lower or same, demotions/maintenance are unconditional
        if (natural.ordinal() >= current.ordinal()) {
            return natural;
        }

        // Try to promote step-by-step through the tiers above current
        Tier t = current;
        while (t != natural) {
            Tier next = getTierAbove(t);
            if (next == null) break;
            
            // Check if score meets threshold and slot is available
            if (data.getScore() >= getTierMinScore(next) && hasSlotAvailable(next, data)) {
                t = next;
            } else {
                break; // Gated by score or capacity
            }
        }
        return t;
    }

    public void recalculateTier(PlayerData data, boolean triggerVisuals, boolean isDeath) {
        // Clamp S tier score cap
        int sCap = plugin.getConfig().getInt("s-score-cap", 505);
        if (data.getTier() == Tier.S && data.getScore() > sCap) {
            data.setScore(sCap);
        }

        Tier oldTier = data.getTier();

        // High-stakes S demotion check
        if (isDeath && oldTier == Tier.S) {
            int sMin = getTierMinScore(Tier.S);
            if (data.getScore() < sCap) { // score dropped
                List<PlayerData> candidates = new ArrayList<>();
                for (PlayerData d : plugin.getDataManager().getAllData()) {
                    if (d.getTier() == Tier.A && d.getScore() >= sMin && !isSDemotionOnCooldown(d.getUuid())) {
                        candidates.add(d);
                    }
                }
                if (!candidates.isEmpty()) {
                    // Demote S player who died
                    data.setTier(Tier.A);
                    startSDemotionCooldown(data.getUuid());
                    if (triggerVisuals) {
                        plugin.getVisualManager().handleTierChange(data, Tier.S, Tier.A);
                        plugin.getBenefitManager().applyBenefits(data.getUuid(), Tier.A);
                        plugin.getExtraInventoryManager().handleTierDowngrade(data.getUuid(), Tier.A);
                    }
                    
                    // Promote best A player
                    candidates.sort(Comparator.comparingInt(PlayerData::getScore).reversed());
                    PlayerData best = candidates.get(0);
                    best.setTier(Tier.S);
                    if (triggerVisuals) {
                        plugin.getVisualManager().handleTierChange(best, Tier.A, Tier.S);
                        plugin.getBenefitManager().applyBenefits(best.getUuid(), Tier.S);
                    }
                    
                    // Cascade vacancies
                    fillVacancies(Tier.A, triggerVisuals);
                    return;
                }
            }
        }

        Tier newTier = calculateTier(data);

        if (oldTier == newTier) return;

        data.setTier(newTier);

        if (oldTier == Tier.S && newTier != Tier.S) {
            startSDemotionCooldown(data.getUuid());
        }

        if (triggerVisuals) {
            plugin.getVisualManager().handleTierChange(data, oldTier, newTier);
            plugin.getBenefitManager().applyBenefits(data.getUuid(), newTier);
            plugin.getExtraInventoryManager().handleTierDowngrade(data.getUuid(), newTier);
        }

        // Trigger cascade vacancy check on the tier the player left
        fillVacancies(oldTier, triggerVisuals);
    }

    public void fillVacancies(Tier tier, boolean triggerVisuals) {
        if (tier == Tier.UNRANKED) return;

        int cap = getTierCap(tier);
        Tier tierBelow = getTierBelow(tier);
        if (tierBelow == null) return;
        int minScore = getTierMinScore(tier);

        while (getTierCount(tier) < cap) {
            List<PlayerData> candidates = new ArrayList<>();
            for (PlayerData d : plugin.getDataManager().getAllData()) {
                if (d.getTier() == tierBelow && d.getScore() >= minScore && hasSlotAvailable(tier, d)) {
                    candidates.add(d);
                }
            }
            if (candidates.isEmpty()) break;

            candidates.sort(Comparator.comparingInt(PlayerData::getScore).reversed());
            PlayerData best = candidates.get(0);

            // Promote the best player to this tier
            best.setTier(tier);
            if (triggerVisuals) {
                plugin.getVisualManager().handleTierChange(best, tierBelow, tier);
                plugin.getBenefitManager().applyBenefits(best.getUuid(), tier);
            }

            // Promoting this player out of tierBelow creates a vacancy in tierBelow. Fill it!
            fillVacancies(tierBelow, triggerVisuals);
        }
    }

    public void recalculateAll(boolean triggerVisuals) {
        List<PlayerData> all = new ArrayList<>(plugin.getDataManager().getAllData());

        // Cache original tiers
        Map<UUID, Tier> oldTiers = new HashMap<>();
        for (PlayerData d : all) {
            // Apply S score cap if currently S
            int sCap = plugin.getConfig().getInt("s-score-cap", 505);
            if (d.getTier() == Tier.S && d.getScore() > sCap) {
                d.setScore(sCap);
            }
            oldTiers.put(d.getUuid(), d.getTier());
            d.setTier(Tier.UNRANKED); // Reset counts to 0
        }

        // Sort by score descending
        all.sort(Comparator.comparingInt(PlayerData::getScore).reversed());

        for (PlayerData data : all) {
            Tier natural = getNaturalTier(data.getScore());
            Tier assigned = Tier.UNRANKED;

            for (Tier t : new Tier[]{Tier.S, Tier.A, Tier.B, Tier.C}) {
                if (isAtLeast(natural, t) && hasSlotAvailable(t, data)) {
                    assigned = t;
                    break;
                }
            }

            data.setTier(assigned);
            Tier old = oldTiers.get(data.getUuid());
            if (assigned != old) {
                if (old == Tier.S && assigned != Tier.S) {
                    startSDemotionCooldown(data.getUuid());
                }
                if (triggerVisuals) {
                    plugin.getVisualManager().handleTierChange(data, old, assigned);
                    plugin.getBenefitManager().applyBenefits(data.getUuid(), assigned);
                    plugin.getExtraInventoryManager().handleTierDowngrade(data.getUuid(), assigned);
                }
            } else {
                if (triggerVisuals) {
                    plugin.getBenefitManager().applyBenefits(data.getUuid(), assigned);
                    Player p = Bukkit.getPlayer(data.getUuid());
                    if (p != null) {
                        plugin.getVisualManager().updateNametag(p);
                    }
                }
            }
        }
    }

    public Tier getNaturalTier(int score) {
        int sMin = plugin.getConfig().getInt("tier-thresholds.s-min", 500);
        int aMin = plugin.getConfig().getInt("tier-thresholds.a-min", 300);
        int bMin = plugin.getConfig().getInt("tier-thresholds.b-min", 150);
        int cMin = plugin.getConfig().getInt("tier-thresholds.c-min", 50);

        if (score >= sMin) return Tier.S;
        if (score >= aMin) return Tier.A;
        if (score >= bMin) return Tier.B;
        if (score >= cMin) return Tier.C;
        return Tier.UNRANKED;
    }

    public int getTierMinScore(Tier tier) {
        switch (tier) {
            case S: return plugin.getConfig().getInt("tier-thresholds.s-min", 500);
            case A: return plugin.getConfig().getInt("tier-thresholds.a-min", 300);
            case B: return plugin.getConfig().getInt("tier-thresholds.b-min", 150);
            case C: return plugin.getConfig().getInt("tier-thresholds.c-min", 50);
            case UNRANKED:
            default: return 0;
        }
    }

    private Tier getTierBelow(Tier tier) {
        switch (tier) {
            case S: return Tier.A;
            case A: return Tier.B;
            case B: return Tier.C;
            case C: return Tier.UNRANKED;
            default: return null;
        }
    }

    private Tier getTierAbove(Tier tier) {
        switch (tier) {
            case UNRANKED: return Tier.C;
            case C: return Tier.B;
            case B: return Tier.A;
            case A: return Tier.S;
            default: return null;
        }
    }

    private boolean isAtLeast(Tier natural, Tier target) {
        return natural.ordinal() <= target.ordinal(); // ordinal: S=0, A=1, B=2, C=3, UNRANKED=4
    }
}
