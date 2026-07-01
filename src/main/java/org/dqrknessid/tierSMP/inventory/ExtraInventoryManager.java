package org.dqrknessid.tierSMP.inventory;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.dqrknessid.tierSMP.TierSMP;
import org.dqrknessid.tierSMP.data.PlayerData;
import org.dqrknessid.tierSMP.tier.Tier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ExtraInventoryManager {
    private final TierSMP plugin;

    public static class EinvHolder implements InventoryHolder {
        private final UUID ownerUuid;
        private final boolean readOnly;
        private Inventory inventory;

        public EinvHolder(UUID ownerUuid, boolean readOnly) {
            this.ownerUuid = ownerUuid;
            this.readOnly = readOnly;
        }

        public UUID getOwnerUuid() {
            return ownerUuid;
        }

        public boolean isReadOnly() {
            return readOnly;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }

        public void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }
    }

    public ExtraInventoryManager(TierSMP plugin) {
        this.plugin = plugin;
    }

    public void openExtraInventory(Player player) {
        PlayerData data = plugin.getDataManager().getOrCreate(player.getUniqueId());
        Tier tier = data.getTier();
        if (tier != Tier.A && tier != Tier.S) {
            plugin.getVisualManager().sendMessage(player, "no-einv-access", "&cYou do not have access to an extra inventory at your current tier.");
            return;
        }

        int size = (tier == Tier.S) ? 18 : 9;
        String title = "Extra Inventory [" + tier.name() + "]";
        
        EinvHolder holder = new EinvHolder(player.getUniqueId(), false);
        Inventory inv = Bukkit.createInventory(holder, size, LegacyComponentSerializer.legacyAmpersand().deserialize(title));
        holder.setInventory(inv);

        fillInventory(inv, data.getEinvContents(), size);
        player.openInventory(inv);
    }

    public void openExtraInventoryAdmin(Player admin, OfflinePlayer target) {
        PlayerData data = plugin.getDataManager().getOrCreate(target.getUniqueId());
        Tier tier = data.getTier();
        if (tier != Tier.A && tier != Tier.S) {
            String msg = plugin.getVisualManager().getMessage("no-einv-to-view", "&c{player} has no extra inventory.")
                    .replace("{player}", target.getName() != null ? target.getName() : "Unknown");
            admin.sendMessage(plugin.getVisualManager().parse(msg));
            return;
        }

        int size = (tier == Tier.S) ? 18 : 9;
        String title = "Inspecting " + (target.getName() != null ? target.getName() : "Player") + "'s Extra Inventory";

        // Admin view is writable (readOnly = false)
        EinvHolder holder = new EinvHolder(target.getUniqueId(), false);
        Inventory inv = Bukkit.createInventory(holder, size, LegacyComponentSerializer.legacyAmpersand().deserialize(title));
        holder.setInventory(inv);

        fillInventory(inv, data.getEinvContents(), size);
        admin.openInventory(inv);
    }

    private void fillInventory(Inventory inv, List<ItemStack> items, int size) {
        for (int i = 0; i < Math.min(size, items.size()); i++) {
            inv.setItem(i, items.get(i));
        }
    }

    public void saveExtraInventory(UUID ownerUuid, Inventory inv) {
        PlayerData data = plugin.getDataManager().getOrCreate(ownerUuid);
        data.setEinvContents(Arrays.asList(inv.getContents()));
    }

    public void dropExtraInventory(UUID uuid, Location location) {
        PlayerData data = plugin.getDataManager().getOrCreate(uuid);
        List<ItemStack> contents = data.getEinvContents();
        if (contents == null || contents.isEmpty()) return;

        for (ItemStack item : contents) {
            if (item != null && item.getType().isItem() && !item.getType().isAir()) {
                location.getWorld().dropItemNaturally(location, item);
            }
        }
        data.setEinvContents(new ArrayList<>());
        
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.getOpenInventory().getTopInventory().getHolder() instanceof EinvHolder) {
            EinvHolder holder = (EinvHolder) player.getOpenInventory().getTopInventory().getHolder();
            if (holder.getOwnerUuid().equals(uuid)) {
                player.closeInventory();
            }
        }
    }

    public void handleTierDowngrade(UUID uuid, Tier newTier) {
        if (newTier == Tier.A || newTier == Tier.S) return;
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;

        Inventory top = player.getOpenInventory().getTopInventory();
        if (top.getHolder() instanceof EinvHolder) {
            EinvHolder holder = (EinvHolder) top.getHolder();
            if (holder.getOwnerUuid().equals(uuid)) {
                // Save first
                saveExtraInventory(uuid, top);
                // Close second
                player.closeInventory();
                // Send messages with proper translation
                plugin.getVisualManager().sendMessage(player, "no-einv-access", "&cYour extra inventory access has been locked due to a tier downgrade.");
            }
        }
    }
}
