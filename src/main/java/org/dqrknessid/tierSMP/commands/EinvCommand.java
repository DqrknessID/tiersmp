package org.dqrknessid.tierSMP.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.dqrknessid.tierSMP.TierSMP;

public class EinvCommand implements CommandExecutor {
    private final TierSMP plugin;

    public EinvCommand(TierSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        Player player = (Player) sender;
        plugin.getExtraInventoryManager().openExtraInventory(player);
        return true;
    }
}
