package org.dqrknessid.tierSMP.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.dqrknessid.tierSMP.TierSMP;
import org.dqrknessid.tierSMP.inventory.ExtraInventoryManager.EinvHolder;

public class InventoryListener implements Listener {
    private final TierSMP plugin;

    public InventoryListener(TierSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inv = event.getInventory();
        if (inv.getHolder() instanceof EinvHolder) {
            EinvHolder holder = (EinvHolder) inv.getHolder();
            if (holder.isReadOnly()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory inv = event.getInventory();
        if (inv.getHolder() instanceof EinvHolder) {
            EinvHolder holder = (EinvHolder) inv.getHolder();
            if (holder.isReadOnly()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inv = event.getInventory();
        if (inv.getHolder() instanceof EinvHolder) {
            EinvHolder holder = (EinvHolder) inv.getHolder();
            if (!holder.isReadOnly()) {
                plugin.getExtraInventoryManager().saveExtraInventory(holder.getOwnerUuid(), inv);
            }
        }
    }
}
