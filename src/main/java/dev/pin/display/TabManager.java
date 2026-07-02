package dev.pin.display;

import dev.pin.PinEconomy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public class TabManager {

    private final PinEconomy plugin;
    private BukkitTask task;

    public TabManager(PinEconomy plugin) {
        this.plugin = plugin;
        start();
    }

    private void start() {
        if (!plugin.getConfig().getBoolean("tab.enabled", true)) return;
        int interval = plugin.getConfig().getInt("tab.update-interval-ticks", 60);
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) update(p);
        }, interval, interval);
    }

    public void update(Player player) {
        if (!plugin.getConfig().getBoolean("tab.enabled", true)) return;
        double balance = plugin.getStorage().getBalance(player.getUniqueId());
        String balStr = String.format("%,.0f", balance);

        String header = plugin.getConfig().getString("tab.header", "")
                .replace("\\n", "\n")
                .replace("{balance}", balStr);

        String footer = plugin.getConfig().getString("tab.footer", "")
                .replace("{balance}", balStr)
                .replace("{zone}", getZone(player));

        header = ChatColor.translateAlternateColorCodes('&', header);
        footer = ChatColor.translateAlternateColorCodes('&', footer);

        player.setPlayerListHeaderFooter(header, footer);
    }

    private String getZone(Player player) {
        double x = player.getLocation().getX();
        double z = player.getLocation().getZ();
        double dist = Math.sqrt(x * x + z * z);
        if (dist <= 500) return "§a[SIGNAL]";
        if (dist <= 1800) return "§c[DEAD ZONE]";
        return "§4[GROUND ZERO]";
    }

    public void shutdown() {
        if (task != null) task.cancel();
    }
}
