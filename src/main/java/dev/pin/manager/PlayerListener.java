package dev.pin.manager;

import dev.pin.PinEconomy;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final PinEconomy plugin;

    public PlayerListener(PinEconomy plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        double startBal = plugin.getConfig().getDouble("economy.starting-balance", 100);
        plugin.getStorage().createAccount(player.getUniqueId(), player.getName(), startBal);
        plugin.getStorage().setName(player.getUniqueId(), player.getName());
        plugin.getScoreboardManager().update(player);
        plugin.getTabManager().update(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getScoreboardManager().remove(event.getPlayer());
    }
}
