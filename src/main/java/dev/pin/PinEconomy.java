package dev.pin;

import dev.pin.command.PinCommand;
import dev.pin.display.ScoreboardManager;
import dev.pin.display.TabManager;
import dev.pin.economy.PinEconomyProvider;
import dev.pin.manager.PlayerListener;
import dev.pin.manager.PinStorage;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class PinEconomy extends JavaPlugin {

    private PinStorage storage;
    private PinEconomyProvider economyProvider;
    private ScoreboardManager scoreboardManager;
    private TabManager tabManager;
    private BukkitTask saveTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        storage = new PinStorage(this);

        economyProvider = new PinEconomyProvider(this, storage);
        getServer().getServicesManager().register(
                Economy.class, economyProvider, this, ServicePriority.Highest);
        getLogger().info("[PinEconomy] Đã đăng ký Vault Economy provider.");

        scoreboardManager = new ScoreboardManager(this);
        tabManager = new TabManager(this);

        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);

        PinCommand pinCmd = new PinCommand(this);
        getCommand("pin").setExecutor(pinCmd);
        getCommand("pin").setTabCompleter(pinCmd);

        saveTask = Bukkit.getScheduler().runTaskTimer(this, () -> storage.save(), 6000L, 6000L);

        double startBal = getConfig().getDouble("economy.starting-balance", 100);
        for (Player p : Bukkit.getOnlinePlayers()) {
            storage.createAccount(p.getUniqueId(), p.getName(), startBal);
        }

        getLogger().info("[PinEconomy] ⚡ Hệ thống Pin đã khởi động!");
    }

    @Override
    public void onDisable() {
        if (saveTask != null) saveTask.cancel();
        if (storage != null) storage.save();
        if (scoreboardManager != null) scoreboardManager.shutdown();
        if (tabManager != null) tabManager.shutdown();
        getLogger().info("[PinEconomy] Đã lưu dữ liệu và tắt.");
    }

    public PinStorage getStorage() { return storage; }
    public ScoreboardManager getScoreboardManager() { return scoreboardManager; }
    public TabManager getTabManager() { return tabManager; }
    public PinEconomyProvider getEconomyProvider() { return economyProvider; }
}
