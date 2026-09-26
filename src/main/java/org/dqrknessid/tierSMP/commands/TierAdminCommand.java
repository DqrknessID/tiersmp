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
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&cUsage: /tieradmin <set|reset|resetall|reload|setscore|givescore|listtier|checklimit|clearcooldown|recalculate>"));
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
            case "listtier":
            case "listtiers":
            case "list":
                return handleListTier(sender, args);
            case "checklimit":
            case "limits":
            case "caps":
                return handleCheckLimit(sender);
            case "clearcooldown":
            case "resetcooldown":
                return handleClearCooldown(sender, args);
            case "clearkillcooldown":
            case "clearkillcd":
            case "resetkillcooldown":
                return handleClearKillCooldown(sender, args);
            case "togglekillcooldown":
            case "togglekillcd":
                return handleToggleKillCooldown(sender);
            case "debug":
                return handleDebug(sender);
            case "tournament":
            case "event":
            case "pausebuffs":
            case "fairmode":
                return handleTournament(sender, args);
            case "recalculate":
            case "recalc":
                return handleRecalculate(sender);
            default:
                sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&cUnknown subcommand. Use /tieradmin for help."));
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

        // Check if score qualified for a higher tier but promotion was blocked
        checkPromotionNote(sender, data);
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

        // Check if score qualified for a higher tier but promotion was blocked
        checkPromotionNote(sender, data);
        return true;
    }

    private void checkPromotionNote(CommandSender sender, PlayerData data) {
        Tier natural = plugin.getTierManager().getNaturalTier(data.getScore());
        Tier actual = data.getTier();
        if (natural.ordinal() < actual.ordinal()) {
            if (natural == Tier.S && actual != Tier.S) {
                if (plugin.getTierManager().isSDemotionOnCooldown(data.getUuid())) {
                    long remaining = plugin.getTierManager().getSDemotionRemainingSeconds(data.getUuid());
                    sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                            "&e[Note] Promotion to S-Tier is paused by demotion cooldown (" + remaining + "s left). Use &f/tieradmin clearcooldown " + getPlayerName(data.getUuid()) + " &eto clear it."
                    ));
                } else if (!plugin.getTierManager().hasSlotAvailable(Tier.S)) {
                    sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                            "&e[Note] Promotion to S-Tier is blocked because S-Tier is full (" + plugin.getTierManager().getTierCount(Tier.S) + "/" + plugin.getTierManager().getTierCap(Tier.S) + " players)."
                    ));
                }
            } else {
                sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                        "&e[Note] Player's score qualifies for " + natural.name() + " Tier, but " + natural.name() + " Tier is currently at capacity (" + plugin.getTierManager().getTierCount(natural) + "/" + plugin.getTierManager().getTierCap(natural) + ")."
                ));
            }
        }
    }

    private boolean handleListTier(CommandSender sender, String[] args) {
        if (args.length >= 2) {
            String tierStr = args[1].toUpperCase();
            Tier tier;
            try {
                tier = Tier.valueOf(tierStr);
            } catch (IllegalArgumentException e) {
                sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&cInvalid tier. Use: S, A, B, C, UNRANKED"));
                return true;
            }

            List<PlayerData> players = plugin.getTierManager().getPlayersInTier(tier);
            int cap = plugin.getTierManager().getTierCap(tier);
            String capStr = (tier == Tier.UNRANKED) ? "Unlimited" : String.valueOf(cap);
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                    "&6=== TierSMP - " + tier.name() + " Tier (" + players.size() + "/" + capStr + ") ==="
            ));
            if (players.isEmpty()) {
                sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&7No players in this tier."));
            } else {
                for (PlayerData p : players) {
                    String name = getPlayerName(p.getUuid());
                    String extra = "";
                    if (tier == Tier.S && p.getKillStreak() > 0) {
                        extra += " &7| Streak: &f" + p.getKillStreak();
                    }
                    if (plugin.getTierManager().isSDemotionOnCooldown(p.getUuid())) {
                        extra += " &c(Demotion CD: " + plugin.getTierManager().getSDemotionRemainingSeconds(p.getUuid()) + "s)";
                    }
                    sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                            "&7- &e" + name + " &7| Score: &f" + p.getScore() + extra
                    ));
                }
            }
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&6================================="));
            return true;
        }

        // List all tiers
        sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&6=== TierSMP Tier Members Overview ==="));
        for (Tier t : new Tier[]{Tier.S, Tier.A, Tier.B, Tier.C, Tier.UNRANKED}) {
            List<PlayerData> players = plugin.getTierManager().getPlayersInTier(t);
            int cap = plugin.getTierManager().getTierCap(t);
            String capStr = (t == Tier.UNRANKED) ? "Unlimited" : String.valueOf(cap);
            
            StringBuilder sb = new StringBuilder();
            sb.append("&e[").append(t.name()).append(" Tier] &7(&f").append(players.size()).append("&7/&f").append(capStr).append("&7): ");
            
            if (players.isEmpty()) {
                sb.append("&7None");
            } else {
                for (int i = 0; i < players.size(); i++) {
                    if (i > 0) sb.append("&7, ");
                    PlayerData p = players.get(i);
                    sb.append("&a").append(getPlayerName(p.getUuid())).append(" &7(&f").append(p.getScore()).append("&7)");
                }
            }
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(sb.toString()));
        }
        sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&6======================================"));
        return true;
    }

    private boolean handleCheckLimit(CommandSender sender) {
        sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&6=== TierSMP Capacity Limits & Status ==="));
        for (Tier t : new Tier[]{Tier.S, Tier.A, Tier.B, Tier.C}) {
            int count = plugin.getTierManager().getTierCount(t);
            int cap = plugin.getTierManager().getTierCap(t);
            int minScore = plugin.getTierManager().getTierMinScore(t);
            int free = Math.max(0, cap - count);
            String status = free > 0 ? "&a(" + free + " slots free)" : "&c(FULL)";

            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                    "&e" + t.name() + " Tier: &f" + count + "&7/&e" + cap + " " + status + " &7| Min Score: &f" + minScore
            ));
        }

        Map<UUID, Long> cds = plugin.getTierManager().getActiveSDemotionCooldowns();
        if (cds.isEmpty()) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&eActive S-Demotion Cooldowns: &aNone"));
        } else {
            StringBuilder sb = new StringBuilder("&eActive S-Demotion Cooldowns: ");
            int idx = 0;
            for (Map.Entry<UUID, Long> entry : cds.entrySet()) {
                if (idx++ > 0) sb.append("&7, ");
                sb.append("&c").append(getPlayerName(entry.getKey())).append(" &7(&f").append(entry.getValue()).append("s left&7)");
            }
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(sb.toString()));
        }
        sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&6========================================="));
        return true;
    }

    private boolean handleClearCooldown(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&cUsage: /tieradmin clearcooldown <player>"));
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        plugin.getTierManager().clearSDemotionCooldown(target.getUniqueId());

        // Recheck tier now that cooldown is gone
        PlayerData data = plugin.getDataManager().getOrCreate(target.getUniqueId());
        plugin.getTierManager().recalculateTier(data, true, false);
        plugin.getDataManager().saveAll();

        sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                "&a[TierSMP] Cleared S-demotion cooldown for &e" + (target.getName() != null ? target.getName() : args[1]) + "&a. Current Tier: &f" + data.getTier().name()
        ));
        return true;
    }

    private boolean handleClearKillCooldown(CommandSender sender, String[] args) {
        if (args.length < 2 || args[1].equalsIgnoreCase("all")) {
            plugin.getCombatListener().clearAllKillCooldowns();
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                    "&a[TierSMP] Cleared all PvP kill cooldowns for all players."
            ));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        plugin.getCombatListener().clearKillCooldown(target.getUniqueId());
        sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                "&a[TierSMP] Cleared PvP kill cooldowns involving &e" + (target.getName() != null ? target.getName() : args[1]) + "&a."
        ));
        return true;
    }

    private boolean handleToggleKillCooldown(CommandSender sender) {
        boolean enabled = plugin.getCombatListener().toggleKillCooldown();
        if (enabled) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                    "&a[TierSMP] PvP kill cooldown is now &2ENABLED &a(5m cooldown active between kills)."
            ));
        } else {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                    "&e[TierSMP] PvP kill cooldown is now &cDISABLED &e(Players can kill repeatedly with score rewards & no cooldown for testing/debug)."
            ));
        }
        return true;
    }

    private boolean handleDebug(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            boolean enabled = plugin.toggleDebugSubscriber(player.getUniqueId());
            if (enabled) {
                player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                        "&a[TierSMP Debug] Live chat debug mode &2ENABLED&a! You will now receive real-time damage, kill score math, and demotion events in chat."
                ));
            } else {
                player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                        "&e[TierSMP Debug] Live chat debug mode &cDISABLED&e."
                ));
            }
        } else {
            boolean enabled = plugin.toggleConsoleDebug();
            sender.sendMessage("[TierSMP Debug] Console debug mode: " + (enabled ? "ENABLED" : "DISABLED"));
        }
        return true;
    }

    private boolean handleTournament(CommandSender sender, String[] args) {
        boolean newState;
        if (args.length >= 2) {
            String sub = args[1].toLowerCase();
            if (sub.equals("on") || sub.equals("start") || sub.equals("enable") || sub.equals("true")) {
                newState = true;
            } else if (sub.equals("off") || sub.equals("stop") || sub.equals("disable") || sub.equals("false")) {
                newState = false;
            } else {
                newState = !plugin.isTournamentMode();
            }
        } else {
            newState = !plugin.isTournamentMode();
        }

        plugin.setTournamentMode(newState);

        if (newState) {
            plugin.getServer().broadcast(LegacyComponentSerializer.legacyAmpersand().deserialize(
                    "&6&l[TierSMP Event] &e⚔ Tournament / Event Mode has been &2&lENABLED&e! All tier health boosts, speed effects, XP multipliers, and extra inventories are temporarily &fPAUSED &efor fair vanilla combat!"
            ));
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                    "&a[TierSMP] Tournament/Fair mode is now &2ACTIVE&a. All players normalized to 10 hearts & 0 buffs."
            ));
        } else {
            plugin.getServer().broadcast(LegacyComponentSerializer.legacyAmpersand().deserialize(
                    "&6&l[TierSMP Event] &e⚔ Tournament / Event Mode has &c&lENDED&e! All tier health perks, speed buffs, multipliers, and extra inventories have been restored."
            ));
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                    "&e[TierSMP] Tournament/Fair mode has ended. Normal tier perks and benefits restored."
            ));
        }
        return true;
    }

    private boolean handleRecalculate(CommandSender sender) {
        plugin.getTierManager().recalculateAll(true);
        plugin.getDataManager().saveAll();
        sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                "&a[TierSMP] Successfully recalculated tiers and benefits for all players!"
        ));
        return true;
    }

    private String getPlayerName(UUID uuid) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        return op.getName() != null ? op.getName() : uuid.toString().substring(0, 8);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterPrefix(Arrays.asList("set", "reset", "resetall", "reload", "setscore", "givescore", "listtier", "checklimit", "clearcooldown", "clearkillcooldown", "togglekillcooldown", "debug", "tournament", "recalculate"), args[0]);
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("tournament") || args[0].equalsIgnoreCase("event") || args[0].equalsIgnoreCase("pausebuffs") || args[0].equalsIgnoreCase("fairmode")) {
                return filterPrefix(Arrays.asList("on", "off", "toggle", "start", "stop"), args[1]);
            }
            if (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("reset") || args[0].equalsIgnoreCase("setscore") || args[0].equalsIgnoreCase("givescore") || args[0].equalsIgnoreCase("clearcooldown") || args[0].equalsIgnoreCase("resetcooldown") || args[0].equalsIgnoreCase("clearkillcooldown") || args[0].equalsIgnoreCase("clearkillcd") || args[0].equalsIgnoreCase("resetkillcooldown")) {
                List<String> list = new ArrayList<>();
                list.add("all");
                for (Player p : Bukkit.getOnlinePlayers()) {
                    list.add(p.getName());
                }
                return filterPrefix(list, args[1]);
            }
            if (args[0].equalsIgnoreCase("listtier") || args[0].equalsIgnoreCase("listtiers") || args[0].equalsIgnoreCase("list")) {
                return filterPrefix(Arrays.asList("S", "A", "B", "C", "UNRANKED"), args[1]);
            }
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            return filterPrefix(Arrays.asList("S", "A", "B", "C", "UNRANKED"), args[2]);
        }
        return Collections.emptyList();
    }

    private List<String> filterPrefix(List<String> options, String prefix) {
        String lower = prefix.toLowerCase();
        List<String> result = new ArrayList<>();
        for (String opt : options) {
            if (opt.toLowerCase().startsWith(lower)) {
                result.add(opt);
            }
        }
        return result;
    }
}
