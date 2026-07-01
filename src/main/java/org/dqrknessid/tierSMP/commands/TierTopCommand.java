package org.dqrknessid.tierSMP.commands;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.dqrknessid.tierSMP.TierSMP;
import org.dqrknessid.tierSMP.data.PlayerData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TierTopCommand implements CommandExecutor {
    private final TierSMP plugin;

    public TierTopCommand(TierSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        List<PlayerData> list = new ArrayList<>(plugin.getDataManager().getAllData());
        list.sort(Comparator.comparingInt(PlayerData::getScore).reversed());

        String header = plugin.getVisualManager().getMessage("tiertop-header", "&6=== TierSMP Leaderboard ===");
        String footer = plugin.getVisualManager().getMessage("tiertop-footer", "&6==========================");
        String entryFormat = plugin.getVisualManager().getMessage("tiertop-entry", "&e{rank}. {player} &7- Score: &f{score} &7(Tier: &e{tier}&7)");

        sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(header));

        int limit = Math.min(10, list.size());
        for (int i = 0; i < limit; i++) {
            PlayerData data = list.get(i);
            OfflinePlayer op = Bukkit.getOfflinePlayer(data.getUuid());
            String name = op.getName() != null ? op.getName() : "Unknown";
            String line = entryFormat.replace("{rank}", String.valueOf(i + 1))
                    .replace("{player}", name)
                    .replace("{score}", String.valueOf(data.getScore()))
                    .replace("{tier}", data.getTier().name());
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(line));
        }

        sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(footer));
        return true;
    }
}
