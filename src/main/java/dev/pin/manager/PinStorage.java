package dev.pin.manager;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PinStorage {

    private final JavaPlugin plugin;
    private File dataFile;
    private YamlConfiguration dataConfig;
    private final Map<UUID, Double> balances = new HashMap<>();
    private final Map<UUID, String> names = new HashMap<>();

    public PinStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        dataFile = new File(plugin.getDataFolder(), "balances.yml");
        if (!dataFile.exists()) {
            try { dataFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        balances.clear();
        names.clear();

        if (!dataConfig.contains("accounts")) return;
        for (String uuidStr : dataConfig.getConfigurationSection("accounts").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                balances.put(uuid, dataConfig.getDouble("accounts." + uuidStr + ".balance", 0));
                names.put(uuid, dataConfig.getString("accounts." + uuidStr + ".name", "Unknown"));
            } catch (Exception ignored) {}
        }
        plugin.getLogger().info("[PinEconomy] Loaded " + balances.size() + " accounts.");
    }

    public void save() {
        if (dataFile == null) return;
        dataConfig = new YamlConfiguration();
        for (Map.Entry<UUID, Double> entry : balances.entrySet()) {
            String path = "accounts." + entry.getKey();
            dataConfig.set(path + ".balance", entry.getValue());
            dataConfig.set(path + ".name", names.getOrDefault(entry.getKey(), "Unknown"));
        }
        try { dataConfig.save(dataFile); }
        catch (IOException e) { plugin.getLogger().severe("[PinEconomy] Không lưu được balances.yml!"); }
    }

    public double getBalance(UUID uuid) {
        return balances.getOrDefault(uuid, plugin.getConfig().getDouble("economy.starting-balance", 100));
    }

    public void setBalance(UUID uuid, double amount) {
        balances.put(uuid, Math.max(0, amount));
    }

    public void setName(UUID uuid, String name) {
        names.put(uuid, name);
    }

    public String getName(UUID uuid) {
        return names.getOrDefault(uuid, "Unknown");
    }

    public boolean hasAccount(UUID uuid) {
        return balances.containsKey(uuid);
    }

    public void createAccount(UUID uuid, String name, double startBalance) {
        if (!balances.containsKey(uuid)) {
            balances.put(uuid, startBalance);
            names.put(uuid, name);
        }
    }

    public Map<UUID, Double> getAllBalances() {
        return Collections.unmodifiableMap(balances);
    }

    public Map<UUID, String> getAllNames() {
        return Collections.unmodifiableMap(names);
    }
            }
