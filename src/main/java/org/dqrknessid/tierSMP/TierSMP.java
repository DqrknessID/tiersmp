package org.dqrknessid.tierSMP;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.dqrknessid.tierSMP.benefits.BenefitManager;
import org.dqrknessid.tierSMP.commands.EinvCommand;
import org.dqrknessid.tierSMP.commands.EinvseeCommand;
import org.dqrknessid.tierSMP.commands.TierAdminCommand;
import org.dqrknessid.tierSMP.commands.TierCommand;
import org.dqrknessid.tierSMP.commands.TierTopCommand;
import org.dqrknessid.tierSMP.commands.TierscoreCommand;
import org.dqrknessid.tierSMP.data.DataManager;
import org.dqrknessid.tierSMP.hooks.PlaceholderHook;
import org.dqrknessid.tierSMP.inventory.ExtraInventoryManager;
import org.dqrknessid.tierSMP.listeners.*;
import org.dqrknessid.tierSMP.tasks.DecayTask;
import org.dqrknessid.tierSMP.tasks.SaveTask;
import org.dqrknessid.tierSMP.tasks.SpeedReapplyTask;
import org.dqrknessid.tierSMP.tier.TierManager;
import org.dqrknessid.tierSMP.visual.StreakScoreboard;
import org.dqrknessid.tierSMP.visual.VisualManager;

public class TierSMP extends JavaPlugin {
    private DataManager dataManager;
    private TierManager tierManager;
    private BenefitManager benefitManager;
    private ExtraInventoryManager extraInventoryManager;
    private VisualManager visualManager;
    private StreakScoreboard streakScoreboard;

    private CombatListener combatListener;
    private CombatLogListener combatLogListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Managers
        dataManager = new DataManager(this);
        dataManager.loadAll();
        tierManager = new TierManager(this);
        benefitManager = new BenefitManager(this);
        extraInventoryManager = new ExtraInventoryManager(this);
        visualManager = new VisualManager(this);
        streakScoreboard = new StreakScoreboard(this);

        // Listeners
        combatListener = new CombatListener(this);
        combatLogListener = new CombatLogListener(this);

        getServer().getPluginManager().registerEvents(combatListener, this);
        getServer().getPluginManager().registerEvents(combatLogListener, this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryListener(this), this);
        getServer().getPluginManager().registerEvents(new XpListener(this), this);
        getServer().getPluginManager().registerEvents(new PotionListener(this), this);

        // Commands
        getCommand("tier").setExecutor(new TierCommand(this));
        getCommand("tiertop").setExecutor(new TierTopCommand(this));
        getCommand("einv").setExecutor(new EinvCommand(this));
        
        TierAdminCommand adminCmd = new TierAdminCommand(this);
        getCommand("tieradmin").setExecutor(adminCmd);
        getCommand("tieradmin").setTabCompleter(adminCmd);

        TierscoreCommand tierscoreCmd = new TierscoreCommand(this);
        getCommand("tierscore").setExecutor(tierscoreCmd);
        getCommand("tierscore").setTabCompleter(tierscoreCmd);

        EinvseeCommand einvseeCmd = new EinvseeCommand(this);
        getCommand("einvsee").setExecutor(einvseeCmd);
        getCommand("einvsee").setTabCompleter(einvseeCmd);

        // Tasks
        scheduleTasks();

        // PlaceholderAPI hook
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderHook(this).register();
            getLogger().info("PlaceholderAPI hook registered successfully.");
        }
    }

    @Override
    public void onDisable() {
        if (dataManager != null) {
            dataManager.saveAll();
        }
        getServer().getScheduler().cancelTasks(this);
    }

    private void scheduleTasks() {
        // Save task
        long saveInterval = getConfig().getInt("save-interval-minutes", 5) * 1200L;
        new SaveTask(this).runTaskTimerAsynchronously(this, saveInterval, saveInterval);

        // Decay task
        long decayInterval = getConfig().getInt("decay-interval-hours", 24) * 72000L;
        new DecayTask(this).runTaskTimerAsynchronously(this, decayInterval, decayInterval);

        // Scoreboard update task
        long scoreboardInterval = getConfig().getInt("streak-scoreboard-update-seconds", 10) * 20L;
        getServer().getScheduler().runTaskTimer(this, () -> streakScoreboard.update(), scoreboardInterval, scoreboardInterval);

        // Speed reapply task
        long speedInterval = getConfig().getInt("speed-reapply-seconds", 30) * 20L;
        new SpeedReapplyTask(this).runTaskTimer(this, speedInterval, speedInterval);
    }

    public DataManager getDataManager() { return dataManager; }
    public TierManager getTierManager() { return tierManager; }
    public BenefitManager getBenefitManager() { return benefitManager; }
    public ExtraInventoryManager getExtraInventoryManager() { return extraInventoryManager; }
    public VisualManager getVisualManager() { return visualManager; }
    public StreakScoreboard getStreakScoreboard() { return streakScoreboard; }
    public CombatListener getCombatListener() { return combatListener; }
    public CombatLogListener getCombatLogListener() { return combatLogListener; }
}
