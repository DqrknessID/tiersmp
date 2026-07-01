package org.dqrknessid.tierSMP.commands;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.dqrknessid.tierSMP.TierSMP;
import org.dqrknessid.tierSMP.data.PlayerData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class TierCommand implements CommandExecutor {
    private final TierSMP plugin;

    public TierCommand(TierSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        OfflinePlayer target;
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Console must specify a player: /tier <player>");
                return true;
            }
            target = (Player) sender;
        } else {
            target = Bukkit.getOfflinePlayer(args[0]);
        }

        PlayerData data = plugin.getDataManager().getOrCreate(target.getUniqueId());
        int rank = getRank(target.getUniqueId());

        String format = plugin.getVisualManager().getMessage("tier-info-format", 
                "&6[TierSMP] &e{player} &7| Tier: {tier} | Score: {score} | Streak: {streak} | Rank: #{rank}");
        
        String targetName = target.getName() != null ? target.getName() : args.length > 0 ? args[0] : "Unknown";
        String msg = format.replace("{player}", targetName)
                .replace("{tier}", data.getTier().name())
                .replace("{score}", String.valueOf(data.getScore()))
                .replace("{streak}", String.valueOf(data.getKillStreak()))
                .replace("{rank}", String.valueOf(rank));

        sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(msg));
        return true;
    }

    private int getRank(UUID uuid) {
        List<PlayerData> list = new ArrayList<>(plugin.getDataManager().getAllData());
        list.sort(Comparator.comparingInt(PlayerData::getScore).reversed());
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getUuid().equals(uuid)) {
                return i + 1;
            }
        }
        return list.size() + 1;
    }
}
