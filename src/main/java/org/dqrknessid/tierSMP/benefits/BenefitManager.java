package org.dqrknessid.tierSMP.benefits;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.dqrknessid.tierSMP.TierSMP;
import org.dqrknessid.tierSMP.tier.Tier;

import java.util.UUID;

public class BenefitManager {
    private final TierSMP plugin;

    public BenefitManager(TierSMP plugin) {
        this.plugin = plugin;
    }

    public void applyBenefits(UUID uuid, Tier tier) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            applyBenefits(player, tier);
        }
    }

    public void applyBenefits(Player player) {
        Tier tier = plugin.getDataManager().getOrCreate(player.getUniqueId()).getTier();
        applyBenefits(player, tier);
    }

    public void applyBenefits(Player player, Tier tier) {
        if (isDisabledWorld(player.getWorld().getName())) {
            removeBenefits(player);
            return;
        }

        double maxHealth = 20.0;
        switch (tier) {
            case S: maxHealth = 32.0; break;
            case A: maxHealth = 28.0; break;
            case B: maxHealth = 24.0; break;
            case C:
            case UNRANKED:
            default: maxHealth = 20.0; break;
        }

        AttributeInstance healthAttr = player.getAttribute(Attribute.MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.setBaseValue(maxHealth);
        }

        if (tier == Tier.S) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, 0, false, false, false));
        } else {
            player.removePotionEffect(PotionEffectType.SPEED);
        }
    }

    public void removeBenefits(Player player) {
        AttributeInstance healthAttr = player.getAttribute(Attribute.MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.setBaseValue(20.0);
        }
        player.removePotionEffect(PotionEffectType.SPEED);
    }

    private boolean isDisabledWorld(String worldName) {
        return plugin.getConfig().getStringList("disabled-worlds").contains(worldName);
    }
}
