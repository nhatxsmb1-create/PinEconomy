package dev.pin.economy;

import dev.pin.manager.PinStorage;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class PinEconomyProvider implements Economy {

    private final JavaPlugin plugin;
    private final PinStorage storage;

    public PinEconomyProvider(JavaPlugin plugin, PinStorage storage) {
        this.plugin = plugin;
        this.storage = storage;
    }

    @Override public boolean isEnabled() { return plugin.isEnabled(); }
    @Override public String getName() { return "PinEconomy"; }
    @Override public boolean hasBankSupport() { return false; }
    @Override public int fractionalDigits() { return plugin.getConfig().getInt("economy.decimal-places", 0); }
    @Override public String format(double amount) {
        String symbol = plugin.getConfig().getString("economy.currency-symbol", "⚡");
        int decimals = plugin.getConfig().getInt("economy.decimal-places", 0);
        if (decimals == 0) return symbol + " " + (long) amount + " Pin";
        return symbol + " " + String.format("%." + decimals + "f", amount) + " Pin";
    }
    @Override public String currencyNamePlural() { return plugin.getConfig().getString("economy.currency-name-plural", "Pin"); }
    @Override public String currencyNameSingular() { return plugin.getConfig().getString("economy.currency-name", "Pin"); }

    @Override
    public boolean hasAccount(OfflinePlayer player) {
        return storage.hasAccount(player.getUniqueId());
    }
    @Override
    public boolean hasAccount(OfflinePlayer player, String worldName) { return hasAccount(player); }
    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        double start = plugin.getConfig().getDouble("economy.starting-balance", 100);
        storage.createAccount(player.getUniqueId(), player.getName() != null ? player.getName() : "Unknown", start);
        return true;
    }
    @Override
    public boolean createPlayerAccount(OfflinePlayer player, String worldName) { return createPlayerAccount(player); }

    @Override
    public double getBalance(OfflinePlayer player) { return storage.getBalance(player.getUniqueId()); }
    @Override
    public double getBalance(OfflinePlayer player, String world) { return getBalance(player); }
    @Override
    public boolean has(OfflinePlayer player, double amount) { return getBalance(player) >= amount; }
    @Override
    public boolean has(OfflinePlayer player, String worldName, double amount) { return has(player, amount); }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        double balance = getBalance(player);
        if (balance < amount)
            return new EconomyResponse(0, balance, EconomyResponse.ResponseType.FAILURE, "Không đủ Pin.");
        storage.setBalance(player.getUniqueId(), balance - amount);
        return new EconomyResponse(amount, balance - amount, EconomyResponse.ResponseType.SUCCESS, "");
    }
    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) { return withdrawPlayer(player, amount); }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        double balance = getBalance(player);
        storage.setBalance(player.getUniqueId(), balance + amount);
        return new EconomyResponse(amount, balance + amount, EconomyResponse.ResponseType.SUCCESS, "");
    }
    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) { return depositPlayer(player, amount); }

    @Override public boolean hasAccount(String playerName) { return false; }
    @Override public boolean hasAccount(String playerName, String worldName) { return false; }
    @Override public double getBalance(String playerName) { return 0; }
    @Override public double getBalance(String playerName, String world) { return 0; }
    @Override public boolean has(String playerName, double amount) { return false; }
    @Override public boolean has(String playerName, String worldName, double amount) { return false; }
    @Override public EconomyResponse withdrawPlayer(String playerName, double amount) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, ""); }
    @Override public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, ""); }
    @Override public EconomyResponse depositPlayer(String playerName, double amount) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, ""); }
    @Override public EconomyResponse depositPlayer(String playerName, String worldName, double amount) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, ""); }
    @Override public boolean createPlayerAccount(String playerName) { return false; }
    @Override public boolean createPlayerAccount(String playerName, String worldName) { return false; }

    @Override public EconomyResponse createBank(String name, String player) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, ""); }
    @Override public EconomyResponse createBank(String name, OfflinePlayer player) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, ""); }
    @Override public EconomyResponse deleteBank(String name) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, ""); }
    @Override public EconomyResponse bankBalance(String name) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, ""); }
    @Override public EconomyResponse bankHas(String name, double amount) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, ""); }
    @Override public EconomyResponse bankWithdraw(String name, double amount) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, ""); }
    @Override public EconomyResponse bankDeposit(String name, double amount) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, ""); }
    @Override public EconomyResponse isBankOwner(String name, String playerName) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, ""); }
    @Override public EconomyResponse isBankOwner(String name, OfflinePlayer player) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, ""); }
    @Override public EconomyResponse isBankMember(String name, String playerName) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, ""); }
    @Override public EconomyResponse isBankMember(String name, OfflinePlayer player) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, ""); }
    @Override public List<String> getBanks() { return List.of(); }
  }
