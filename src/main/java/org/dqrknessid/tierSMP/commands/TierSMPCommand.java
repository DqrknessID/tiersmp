package org.dqrknessid.tierSMP.commands;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.dqrknessid.tierSMP.TierSMP;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TierSMPCommand implements CommandExecutor, TabCompleter {
    private final TierSMP plugin;

    public TierSMPCommand(TierSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&6=== &eTierSMP v" + plugin.getDescription().getVersion() + " &6==="));
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&7- &f/" + label + " debug &7: Toggle live debug messages in your chat."));
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&7- &f/" + label + " reload &7: Reload plugin configuration."));
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&7- &f/" + label + " info &7: Show plugin information & stats."));
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "debug":
                return handleDebug(sender, args);
            case "tournament":
            case "event":
            case "pausebuffs":
                return handleTournament(sender, args);
            case "reload":
                if (!sender.hasPermission("tiersmp.admin")) {
                    sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&cYou do not have permission to use this command."));
                    return true;
                }
                plugin.reloadConfig();
                plugin.getVisualManager().reloadMessages();
                sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&a[TierSMP] Configuration and messages reloaded!"));
                return true;
            case "info":
            case "version":
                sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&6=== TierSMP Information ==="));
                sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&eVersion: &f" + plugin.getDescription().getVersion()));
                sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&eAuthor: &f" + String.join(", ", plugin.getDescription().getAuthors())));
                sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&eLoaded Players: &f" + plugin.getDataManager().getAllData().size()));
                sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&eDynamic Scaling: &f" + (plugin.getConfig().getBoolean("dynamic-tier-scaling", true) ? "&aENABLED" : "&cDISABLED")));
                sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&6==========================="));
                return true;
            default:
                sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&cUnknown subcommand. Use /" + label + " for help."));
                return true;
        }
    }

    private boolean handleDebug(CommandSender sender, String[] args) {
        if (!sender.hasPermission("tiersmp.admin")) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&cYou do not have permission to use this command."));
            return true;
        }

        if (sender instanceof Player) {
            Player player = (Player) sender;
            boolean enabled = plugin.toggleDebugSubscriber(player.getUniqueId());
            if (enabled) {
                player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                        "&a[TierSMP Debug] Live chat debug mode &2ENABLED&a! You will now see real-time damage, kill score math, tier demotion, and einv logs in chat."
                ));
            } else {
                player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                        "&e[TierSMP Debug] Live chat debug mode &cDISABLED&e."
                ));
            }
        } else {
            boolean enabled = plugin.toggleConsoleDebug();
            if (enabled) {
                sender.sendMessage("[TierSMP Debug] Console debug mode ENABLED.");
            } else {
                sender.sendMessage("[TierSMP Debug] Console debug mode DISABLED.");
            }
        }
        return true;
    }

    private boolean handleTournament(CommandSender sender, String[] args) {
        if (!sender.hasPermission("tiersmp.admin")) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&cYou do not have permission to use this command."));
            return true;
        }

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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            if (sender.hasPermission("tiersmp.admin")) {
                options.add("debug");
                options.add("tournament");
                options.add("reload");
            }
            options.add("info");
            String lower = args[0].toLowerCase();
            List<String> result = new ArrayList<>();
            for (String opt : options) {
                if (opt.toLowerCase().startsWith(lower)) {
                    result.add(opt);
                }
            }
            return result;
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("tournament") || args[0].equalsIgnoreCase("event") || args[0].equalsIgnoreCase("pausebuffs"))) {
            String lower = args[1].toLowerCase();
            List<String> result = new ArrayList<>();
            for (String opt : Arrays.asList("on", "off", "toggle", "start", "stop")) {
                if (opt.startsWith(lower)) {
                    result.add(opt);
                }
            }
            return result;
        }
        return Collections.emptyList();
    }
}
