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
        if (isDisabled(player)) return;

        if (event.getCause() == EntityPotionEffectEvent.Cause.PLUGIN) return;

        if (event.getAction() == EntityPotionEffectEvent.Action.ADDED) {
            PotionEffect newEffect = event.getNewEffect();
            if (newEffect == null) return;

            // Do not multiply infinite or permanent duration effects
            if (newEffect.getDuration() == PotionEffect.INFINITE_DURATION || newEffect.getDuration() < 0) return;

            Tier tier = plugin.getDataManager().getOrCreate(player.getUniqueId()).getTier();
            double multiplier = 1.0;

            if (tier == Tier.S) {
                multiplier = 1.3;
            } else if (tier == Tier.A) {
                multiplier = 1.2;
            }

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
            }
        }
    }
}
