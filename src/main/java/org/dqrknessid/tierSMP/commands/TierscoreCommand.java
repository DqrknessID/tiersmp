package org.dqrknessid.tierSMP.commands;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.dqrknessid.tierSMP.TierSMP;
import org.dqrknessid.tierSMP.data.PlayerData;
import org.dqrknessid.tierSMP.tier.Tier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TierscoreCommand implements CommandExecutor, TabCompleter {
    private final TierSMP plugin;

    public TierscoreCommand(TierSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!plugin.getConfig().getBoolean("score-transaction-enabled", true)) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                    plugin.getVisualManager().getMessage("transaction-disabled", "&cScore transactions are currently disabled.")
            ));
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can give score.");
            return true;
        }

        if (args.length < 3 || !args[0].equalsIgnoreCase("give")) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&cUsage: /tierscore give <player> <amount>"));
            return true;
        }

        Player player = (Player) sender;
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&cPlayer not found or offline."));
            return true;
        }

        if (player.getUniqueId().equals(target.getUniqueId())) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                    plugin.getVisualManager().getMessage("transaction-self", "&cYou cannot give score to yourself.")
            ));
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&cAmount must be a positive integer."));
            return true;
        }

        if (amount <= 0) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&cAmount must be a positive integer."));
            return true;
        }

        PlayerData senderData = plugin.getDataManager().getOrCreate(player.getUniqueId());
        if (senderData.getScore() < amount) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                    plugin.getVisualManager().getMessage("transaction-insufficient", "&cYou don't have enough score.")
            ));
            return true;
        }

        // World toggle checks
        if (plugin.getConfig().getStringList("disabled-worlds").contains(player.getWorld().getName()) ||
                plugin.getConfig().getStringList("disabled-worlds").contains(target.getWorld().getName())) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                    plugin.getVisualManager().getMessage("transaction-disabled-world", "&cScore transactions are disabled in this world.")
            ));
            return true;
        }

        // Combat tag check
        Long expiry = plugin.getCombatListener().getCombatTags().get(player.getUniqueId());
        if (expiry != null && expiry > System.currentTimeMillis()) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                    plugin.getVisualManager().getMessage("transaction-combat-tagged", "&cYou cannot transfer score while in combat.")
            ));
            return true;
        }

        // Zero-sum transfer
        PlayerData targetData = plugin.getDataManager().getOrCreate(target.getUniqueId());
        senderData.setScore(senderData.getScore() - amount);
        
        int targetNewScore = targetData.getScore() + amount;
        int sCap = plugin.getConfig().getInt("s-score-cap", 505);
        if (targetData.getTier() == Tier.S && targetNewScore > sCap) {
            targetNewScore = sCap;
        }
        targetData.setScore(targetNewScore);

        // Recalculate tiers
        plugin.getTierManager().recalculateTier(senderData, true, false);
        plugin.getTierManager().recalculateTier(targetData, true, false);
        
        plugin.getDataManager().saveAll();

        // Send messages privately (no server-wide broadcast)
        String msgSender = plugin.getVisualManager().getMessage("transaction-success-sender", "&a[TierSMP] You gave {amount} score to {player}. Your score: {newScore}.")
                .replace("{amount}", String.valueOf(amount))
                .replace("{player}", target.getName())
                .replace("{newScore}", String.valueOf(senderData.getScore()));
        player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(msgSender));

        String msgReceiver = plugin.getVisualManager().getMessage("transaction-success-receiver", "&a[TierSMP] You received {amount} score from {player}. Your score: {newScore}.")
                .replace("{amount}", String.valueOf(amount))
                .replace("{player}", player.getName())
                .replace("{newScore}", String.valueOf(targetData.getScore()));
        target.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(msgReceiver));

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Collections.singletonList("give");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            List<String> list = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                list.add(p.getName());
            }
            return list;
        }
        return Collections.emptyList();
    }
}
