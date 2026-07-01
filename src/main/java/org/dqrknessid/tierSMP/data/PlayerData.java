package org.dqrknessid.tierSMP.data;

import org.bukkit.inventory.ItemStack;
import org.dqrknessid.tierSMP.tier.Tier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerData {
    private final UUID uuid;
    private int score;
    private Tier tier;
    private int killStreak;
    private List<ItemStack> einvContents;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        this.score = 0;
        this.tier = Tier.UNRANKED;
        this.killStreak = 0;
        this.einvContents = new ArrayList<>();
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = Math.max(0, score);
    }

    public Tier getTier() {
        return tier;
    }

    public void setTier(Tier tier) {
        this.tier = tier;
    }

    public int getKillStreak() {
        return killStreak;
    }

    public void setKillStreak(int killStreak) {
        this.killStreak = Math.max(0, killStreak);
    }

    public List<ItemStack> getEinvContents() {
        return einvContents;
    }

    public void setEinvContents(List<ItemStack> einvContents) {
        this.einvContents = einvContents != null ? einvContents : new ArrayList<>();
    }
}
