package org.dqrknessid.tierSMP.visual;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.dqrknessid.tierSMP.TierSMP;
import org.dqrknessid.tierSMP.tier.Tier;

import java.io.File;
import java.util.UUID;

@SuppressWarnings("deprecation")
public class VisualManager {
    private final TierSMP plugin;
    private FileConfiguration messagesConfig;
    private File messagesFile;

    public VisualManager(TierSMP plugin) {
        this.plugin = plugin;
        this.messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        reloadMessages();
        setupTeams();
    }

    public void reloadMessages() {
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public String getMessage(String key, String def) {
        return messagesConfig.getString(key, def);
    }

    public void broadcastMessage(String key, String def, Player player, String tierName) {
        String msg = getMessage(key, def)
                .replace("{player}", player.getName())
                .replace("{tier}", tierName);
        plugin.getServer().broadcast(LegacyComponentSerializer.legacyAmpersand().deserialize(msg));
    }

    public void sendMessage(org.bukkit.command.CommandSender sender, String key, String def) {
        String msg = getMessage(key, def);
        sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(msg));
    }

    public String translate(String msg) {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', msg);
    }

    public net.kyori.adventure.text.Component parse(String msg) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(msg);
    }

    public void setupTeams() {
        Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
        setupTeam(sb, "tsmp_s", "&6[S] ", org.bukkit.scoreboard.Team.OptionStatus.ALWAYS);
        setupTeam(sb, "tsmp_a", "&b[A] ", org.bukkit.scoreboard.Team.OptionStatus.ALWAYS);
        setupTeam(sb, "tsmp_b", "&a[B] ", org.bukkit.scoreboard.Team.OptionStatus.ALWAYS);
        setupTeam(sb, "tsmp_c", "&8[C] ", org.bukkit.scoreboard.Team.OptionStatus.ALWAYS);
        setupTeam(sb, "tsmp_unranked", "&7", org.bukkit.scoreboard.Team.OptionStatus.ALWAYS);
    }

    private void setupTeam(Scoreboard sb, String name, String prefix, org.bukkit.scoreboard.Team.OptionStatus status) {
        Team team = sb.getTeam(name);
        if (team == null) {
            team = sb.registerNewTeam(name);
        }
        team.prefix(LegacyComponentSerializer.legacyAmpersand().deserialize(prefix));
        team.setOption(Team.Option.NAME_TAG_VISIBILITY, status);
    }

    public void updateNametag(Player player) {
        UUID uuid = player.getUniqueId();
        if (plugin.getConfig().getStringList("disabled-worlds").contains(player.getWorld().getName())) {
            removeFromAllTeams(player.getName());
            return;
        }

        Tier tier = plugin.getDataManager().getOrCreate(uuid).getTier();
        Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
        removeFromAllTeams(player.getName());

        String teamName = "tsmp_" + tier.name().toLowerCase();
        Team team = sb.getTeam(teamName);
        if (team != null) {
            team.addEntry(player.getName());
        }
    }

    private void removeFromAllTeams(String playerName) {
        Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Team team : sb.getTeams()) {
            if (team.getName().startsWith("tsmp_") && team.hasEntry(playerName)) {
                team.removeEntry(playerName);
            }
        }
    }

    public void handleTierChange(org.dqrknessid.tierSMP.data.PlayerData data, Tier oldTier, Tier newTier) {
        Player player = Bukkit.getPlayer(data.getUuid());
        if (player == null) return;

        updateNametag(player);

        // Broadcast
        boolean isPromo = newTier.ordinal() < oldTier.ordinal(); // ordinal S=0, A=1, B=2, C=3, UNRANKED=4
        if (isPromo) {
            broadcastMessage("tier-promotion", "&6[TierSMP] &e{player} &ahas risen to {tier} tier!", player, newTier.name());
            
            // Sound and Title
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
            String title = translate(getMessage("title-promotion", "&6Tier Up!"));
            String subtitle = translate(getMessage("subtitle-promotion", "&aYou are now {tier} tier!").replace("{tier}", newTier.name()));
            player.sendTitle(title, subtitle, 10, 40, 10);
        } else {
            broadcastMessage("tier-demotion", "&6[TierSMP] &e{player} &chas fallen to {tier} tier.", player, newTier.name());
            
            // Sound and Title
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_WITHER_HURT, 0.5f, 0.8f);
            String title = translate(getMessage("title-demotion", "&cTier Down"));
            String subtitle = translate(getMessage("subtitle-demotion", "&7You have fallen to {tier} tier.").replace("{tier}", newTier.name()));
            player.sendTitle(title, subtitle, 10, 40, 10);
        }

        // Particle Burst
        playParticleBurst(player, newTier);
    }

    private void playParticleBurst(Player player, Tier tier) {
        Particle particle;
        switch (tier) {
            case S: particle = Particle.TOTEM_OF_UNDYING; break;
            case A: particle = Particle.ENCHANT; break;
            case B: particle = Particle.COMPOSTER; break;
            case C: particle = Particle.HAPPY_VILLAGER; break;
            default: return;
        }

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 20 || !player.isOnline()) {
                    cancel();
                    return;
                }
                Location loc = player.getLocation().add(0, 1, 0);
                // 30 particles total, so 2 particles per tick for 20 ticks (total 40 particles) is perfect
                player.getWorld().spawnParticle(particle, loc, 2, 0.5, 0.5, 0.5, 0.05);
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}
