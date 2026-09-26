package org.dqrknessid.tierSMP.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffect;
import org.dqrknessid.tierSMP.TierSMP;
import org.dqrknessid.tierSMP.tier.Tier;

public class PotionListener implements Listener {
    private final TierSMP plugin;

    public PotionListener(TierSMP plugin) {
        this.plugin = plugin;
    }

    private boolean isDisabled(Player p) {
        return plugin.getConfig().getStringList("disabled-worlds").contains(p.getWorld().getName());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPotionEffect(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if (plugin.isTournamentMode() || isDisabled(player)) return;

        if (event.getCause() == EntityPotionEffectEvent.Cause.PLUGIN) return;

        if (event.getAction() == EntityPotionEffectEvent.Action.ADDED) {
            PotionEffect newEffect = event.getNewEffect();
            if (newEffect == null) return;

            // Do not multiply infinite or permanent duration effects
            if (newEffect.getDuration() == PotionEffect.INFINITE_DURATION || newEffect.getDuration() < 0) return;

            Tier tier = plugin.getDataManager().getOrCreate(player.getUniqueId()).getTier();
            double multiplier = plugin.getConfig().getDouble("potion-duration-multipliers." + tier.name().toLowerCase(), getDefaultMultiplier(tier));

            if (multiplier > 1.0) {
                int duration = (int) Math.round(newEffect.getDuration() * multiplier);
                PotionEffect modifiedEffect = new PotionEffect(
                        newEffect.getType(),
                        duration,
                        newEffect.getAmplifier(),
                        newEffect.isAmbient(),
                        newEffect.hasParticles(),
                        newEffect.hasIcon()
                );

                event.setCancelled(true);
                player.addPotionEffect(modifiedEffect);
                plugin.debug("🧪 Potion Duration Boost: §e" + player.getName() + " §7(" + tier + ") got " + newEffect.getType().getKey().getKey() + " for §b" + (duration / 20) + "s §7(Original: " + (newEffect.getDuration() / 20) + "s, " + multiplier + "x)");
            }
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
