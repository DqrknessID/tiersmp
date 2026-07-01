package org.dqrknessid.tierSMP.commands;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.dqrknessid.tierSMP.TierSMP;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EinvseeCommand implements CommandExecutor, TabCompleter {
    private final TierSMP plugin;

    public EinvseeCommand(TierSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Console cannot inspect inventories.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&cUsage: /einvsee <player>"));
            return true;
        }

        Player admin = (Player) sender;
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        plugin.getExtraInventoryManager().openExtraInventoryAdmin(admin, target);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                list.add(p.getName());
            }
            return list;
        }
        return Collections.emptyList();
    }
}
