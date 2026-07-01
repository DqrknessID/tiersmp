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
import org.dqrknessid.tierSMP.data.PlayerData;
import org.dqrknessid.tierSMP.tier.Tier;

import java.util.*;

public class TierAdminCommand implements CommandExecutor, TabCompleter {
    private final TierSMP plugin;
    private final Map<UUID, Long> confirmations = new HashMap<>();

    public TierAdminCommand(TierSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&cUsage: /tieradmin <set|reset|resetall|reload|setscore|givescore>"));
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "set":
                return handleSet(sender, args);
            case "reset":
                return handleReset(sender, args);
            case "resetall":
                return handleResetAll(sender);
            case "reload":
                return handleReload(sender);
            case "setscore":
                return handleSetScore(sender, args);
            case "givescore":
                return handleGiveScore(sender, args);
            default:
                sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&cUnknown subcommand."));
                return true;
        }
    }

    private boolean handleSet(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&cUsage: /tieradmin set <player> <tier>"));
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        String tierStr = args[2].toUpperCase();
        Tier tier;
        try {
            tier = Tier.valueOf(tierStr);
        } catch (IllegalArgumentException e) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&cInvalid tier. Use: S, A, B, C, UNRANKED"));
            return true;
        }

        PlayerData data = plugin.getDataManager().getOrCreate(target.getUniqueId());
        Tier oldTier = data.getTier();
        
        int threshold = 0;
        switch (tier) {
            case S: threshold = plugin.getConfig().getInt("tier-thresholds.s-min", 500); break;
            case A: threshold = plugin.getConfig().getInt("tier-thresholds.a-min", 300); break;
            case B: threshold = plugin.getConfig().getInt("tier-thresholds.b-min", 150); break;
            case C: threshold = plugin.getConfig().getInt("tier-thresholds.c-min", 50); break;
            case UNRANKED:
            default: threshold = 0; break;
        }

        data.setScore(threshold);
        data.setTier(tier); // Force set the tier!

        if (tier == Tier.S) {
            plugin.getTierManager().clearSDemotionCooldown(data.getUuid());
        }

        if (oldTier != tier) {
            plugin.getVisualManager().handleTierChange(data, oldTier, tier);
            plugin.getBenefitManager().applyBenefits(data.getUuid(), tier);
            plugin.getExtraInventoryManager().handleTierDowngrade(data.getUuid(), tier);
        } else {
            plugin.getBenefitManager().applyBenefits(data.getUuid(), tier);
            Player p = Bukkit.getPlayer(data.getUuid());
            if (p != null) {
                plugin.getVisualManager().updateNametag(p);
            }
        }

        // Fill vacancies in the tier they left
        plugin.getTierManager().fillVacancies(oldTier, true);

        plugin.getDataManager().saveAll();

        String msg = plugin.getVisualManager().getMessage("admin-set-success", "&aSuccessfully set {player}'s tier to {tier} (score adjusted to {score}).")
                .replace("{player}", target.getName() != null ? target.getName() : args[1])
                .replace("{tier}", tier.name())
                .replace("{score}", String.valueOf(threshold));
        sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(msg));
        return true;
    }

    private boolean handleReset(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&cUsage: /tieradmin reset <player>"));
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        PlayerData data = plugin.getDataManager().getOrCreate(target.getUniqueId());
        data.setScore(0);
        data.setKillStreak(0);
        data.setEinvContents(new ArrayList<>());
        data.setTier(Tier.UNRANKED);

        Player onlineTarget = target.getPlayer();
        if (onlineTarget != null) {
            onlineTarget.closeInventory();
            plugin.getBenefitManager().applyBenefits(onlineTarget, Tier.UNRANKED);
            plugin.getVisualManager().updateNametag(onlineTarget);
        }

        plugin.getTierManager().recalculateTier(data, true, false);
        plugin.getDataManager().saveAll();

        String msg = plugin.getVisualManager().getMessage("admin-reset-success", "&aSuccessfully reset data for {player}.")
                .replace("{player}", target.getName() != null ? target.getName() : args[1]);
        sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(msg));
        return true;
    }

    private boolean handleResetAll(CommandSender sender) {
        if (!(sender instanceof Player)) {
            // Console bypasses confirmation
            performResetAll();
            sender.sendMessage("Console reset all data successfully.");
            return true;
        }

        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (confirmations.containsKey(uuid) && (now - confirmations.get(uuid)) < 10000L) {
            confirmations.remove(uuid);
            performResetAll();
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(plugin.getVisualManager().getMessage("admin-resetall-done", "&a[TierSMP] All player data has been reset!")));
        } else {
            confirmations.put(uuid, now);
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(plugin.getVisualManager().getMessage("admin-resetall-confirm", "&cPlease run this command again within 10 seconds to confirm reset all data.")));
        }
        return true;
    }

    private void performResetAll() {
        plugin.getDataManager().clearAll();
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.closeInventory();
            plugin.getBenefitManager().applyBenefits(p, Tier.UNRANKED);
            plugin.getVisualManager().updateNametag(p);
        }
        plugin.getServer().broadcast(LegacyComponentSerializer.legacyAmpersand().deserialize(
                plugin.getVisualManager().getMessage("admin-resetall-done", "&a[TierSMP] All player data has been reset!")
        ));
    }

    private boolean handleReload(CommandSender sender) {
        plugin.reloadConfig();
        plugin.getVisualManager().reloadMessages();

        for (Player p : Bukkit.getOnlinePlayers()) {
            plugin.getBenefitManager().applyBenefits(p);
            plugin.getVisualManager().updateNametag(p);
        }

        sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                plugin.getVisualManager().getMessage("admin-reload-done", "&aConfig and messages reloaded successfully.")
        ));
        return true;
    }

    private boolean handleSetScore(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&cUsage: /tieradmin setscore <player> <amount>"));
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&cAmount must be an integer."));
            return true;
        }

        PlayerData data = plugin.getDataManager().getOrCreate(target.getUniqueId());
        int finalScore = Math.max(0, amount);

        // Clamp S tier score cap
        int sCap = plugin.getConfig().getInt("s-score-cap", 505);
        if (data.getTier() == Tier.S && finalScore > sCap) {
            finalScore = sCap;
        }

        data.setScore(finalScore);
        plugin.getTierManager().recalculateTier(data, true, false);
        plugin.getDataManager().saveAll();

        String msg = plugin.getVisualManager().getMessage("admin-setscore-success", "&a[TierSMP] Set {player}'s score to {amount}.")
                .replace("{player}", target.getName() != null ? target.getName() : args[1])
                .replace("{amount}", String.valueOf(finalScore));
        sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(msg));
        return true;
    }

    private boolean handleGiveScore(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&cUsage: /tieradmin givescore <player> <amount>"));
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&cAmount must be an integer."));
            return true;
        }

        PlayerData data = plugin.getDataManager().getOrCreate(target.getUniqueId());
        int finalScore = Math.max(0, data.getScore() + amount);

        // Clamp S tier score cap
        int sCap = plugin.getConfig().getInt("s-score-cap", 505);
        if (data.getTier() == Tier.S && finalScore > sCap) {
            finalScore = sCap;
        }

        data.setScore(finalScore);
        plugin.getTierManager().recalculateTier(data, true, false);
        plugin.getDataManager().saveAll();

        String msg = plugin.getVisualManager().getMessage("admin-givescore-success", "&a[TierSMP] Given {amount} score to {player}. New score: {newScore}.")
                .replace("{player}", target.getName() != null ? target.getName() : args[1])
                .replace("{amount}", String.valueOf(amount))
                .replace("{newScore}", String.valueOf(finalScore));
        sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(msg));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("set", "reset", "resetall", "reload", "setscore", "givescore");
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("reset") || args[0].equalsIgnoreCase("setscore") || args[0].equalsIgnoreCase("givescore"))) {
            List<String> list = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                list.add(p.getName());
            }
            return list;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            return Arrays.asList("S", "A", "B", "C", "UNRANKED");
        }
        return Collections.emptyList();
    }
}
