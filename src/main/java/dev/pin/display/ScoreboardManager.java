package dev.pin.display;

import dev.pin.PinEconomy;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;

import java.util.*;

public class ScoreboardManager {

    private final PinEconomy plugin;
    private BukkitTask task;
    private final Map<UUID, Scoreboard> boards = new HashMap<>();
    private final boolean hasPAPI;

    public ScoreboardManager(PinEconomy plugin) {
        this.plugin = plugin;
        this.hasPAPI = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        start();
    }

    private void start() {
        int interval = plugin.getConfig().getInt("scoreboard.update-interval-ticks", 40);
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) update(p);
        }, interval, interval);
    }

    public void update(Player player) {
        if (!plugin.getConfig().getBoolean("scoreboard.enabled", true)) return;

        Scoreboard board = boards.computeIfAbsent(player.getUniqueId(),
                k -> Bukkit.getScoreboardManager().getNewScoreboard());

        Objective obj = board.getObjective("pin");
        if (obj != null) obj.unregister();

        obj = board.registerNewObjective("pin", Criteria.DUMMY,
                ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfig().getString("scoreboard.title",
                                "§4§l★ THE LAST BROADCAST")));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        List<String> lines = plugin.getConfig().getStringList("scoreboard.lines");
        double balance = plugin.getStorage().getBalance(player.getUniqueId());
        String balStr = String.format("%,.0f", balance);

        List<String> processed = new ArrayList<>();
        for (String line : lines) {
            // Replace custom placeholders
            line = line.replace("{balance}", balStr)
                       .replace("{chapter}", getChapter())
                       .replace("{zone}", getZone(player))
                       .replace("{wanted}", getWantedStatus(player));

            // Parse PlaceholderAPI placeholders
            if (hasPAPI) {
                line = PlaceholderAPI.setPlaceholders(player, line);
            }

            processed.add(ChatColor.translateAlternateColorCodes('&', line));
        }

        int score = processed.size();
        Set<String> usedEntries = new HashSet<>();
        for (String line : processed) {
            String entry = line;
            while (usedEntries.contains(entry)) entry += " ";
            usedEntries.add(entry);
            obj.getScore(entry).setScore(score--);
        }

        player.setScoreboard(board);
    }

    private String getChapter() {
        return "0";
    }

    private String getZone(Player player) {
        double x = player.getLocation().getX();
        double z = player.getLocation().getZ();
        double dist = Math.sqrt(x * x + z * z);
        if (dist <= 500) return "§a[SIGNAL]";
        if (dist <= 1800) return "§c[DEAD ZONE]";
        return "§4[GROUND ZERO]";
    }

    private String getWantedStatus(Player player) {
        try {
            var wantedPlugin = Bukkit.getPluginManager().getPlugin("WantedPlugin");
            if (wantedPlugin != null) {
                var method = wantedPlugin.getClass().getMethod("getWantedManager");
                var manager = method.invoke(wantedPlugin);
                var getData = manager.getClass().getMethod("getData", UUID.class);
                var data = getData.invoke(manager, player.getUniqueId());
                if (data != null) {
                    var isWanted = data.getClass().getMethod("isWanted");
                    var getLevel = data.getClass().getMethod("getWantedLevel");
                    boolean wanted = (boolean) isWanted.invoke(data);
                    if (wanted) {
                        int level = (int) getLevel.invoke(data);
                        return "§c" + "★".repeat(level);
                    }
                }
            }
        } catch (Exception ignored) {}
        return "§a[BÌNH THƯỜNG]";
    }

    public void remove(Player player) {
        boards.remove(player.getUniqueId());
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    public void shutdown() {
        if (task != null) task.cancel();
        for (Player p : Bukkit.getOnlinePlayers()) remove(p);
    }
}
