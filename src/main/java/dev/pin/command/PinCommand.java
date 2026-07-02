package dev.pin.command;

import dev.pin.PinEconomy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class PinCommand implements CommandExecutor, TabCompleter {

    private final PinEconomy plugin;

    public PinCommand(PinEconomy plugin) {
        this.plugin = plugin;
    }

    private String p() { return plugin.getConfig().getString("messages.prefix", "§8[§a⚡ PIN§8] §r"); }
    private String fmt(double amount) { return String.format("%,.0f", amount); }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("balance") || args[0].equalsIgnoreCase("bal")) {
            return cmdBalance(sender, args);
        }
        return switch (args[0].toLowerCase()) {
            case "pay" -> cmdPay(sender, args);
            case "top" -> cmdTop(sender);
            case "add" -> cmdAdmin(sender, args, "add");
            case "remove" -> cmdAdmin(sender, args, "remove");
            case "set" -> cmdAdmin(sender, args, "set");
            case "reset" -> cmdAdmin(sender, args, "reset");
            case "reload" -> cmdReload(sender);
            default -> { sendHelp(sender); yield true; }
        };
    }

    private boolean cmdBalance(CommandSender sender, String[] args) {
        if (args.length >= 2) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            double bal = plugin.getStorage().getBalance(target.getUniqueId());
            sender.sendMessage(p() + plugin.getConfig().getString("messages.balance-other", "")
                    .replace("{player}", args[1])
                    .replace("{balance}", fmt(bal)));
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(p() + "§cPhải nhập tên người chơi."); return true;
        }
        double bal = plugin.getStorage().getBalance(player.getUniqueId());
        sender.sendMessage(p() + plugin.getConfig().getString("messages.balance-self", "")
                .replace("{balance}", fmt(bal)));
        return true;
    }

    private boolean cmdPay(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage(p() + "§cChỉ người chơi dùng được."); return true; }
        if (!player.hasPermission("pin.pay")) { sender.sendMessage(p() + "§cKhông có quyền."); return true; }
        if (args.length < 3) { sender.sendMessage(p() + "§cCú pháp: /pin pay <player> <amount>"); return true; }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) { sender.sendMessage(p() + "§c" + args[1] + " không online."); return true; }
        if (target.equals(player)) { sender.sendMessage(p() + "§cKhông thể tự chuyển cho mình."); return true; }
        double amount;
        try { amount = Double.parseDouble(args[2]); }
        catch (NumberFormatException e) { sender.sendMessage(p() + "§cSố tiền không hợp lệ."); return true; }
        double min = plugin.getConfig().getDouble("transfer.min-amount", 1);
        double max = plugin.getConfig().getDouble("transfer.max-amount", 50000);
        if (amount < min) { sender.sendMessage(p() + "§cTối thiểu §f" + fmt(min) + " Pin."); return true; }
        if (amount > max) { sender.sendMessage(p() + "§cTối đa §f" + fmt(max) + " Pin."); return true; }
        double tax = amount * plugin.getConfig().getDouble("transfer.tax-percent", 5) / 100.0;
        double total = amount + tax;
        double senderBal = plugin.getStorage().getBalance(player.getUniqueId());
        if (senderBal < total) {
            sender.sendMessage(p() + plugin.getConfig().getString("messages.pay-insufficient", "")
                    .replace("{amount}", fmt(total)).replace("{tax}", fmt(tax)));
            return true;
        }
        plugin.getStorage().setBalance(player.getUniqueId(), senderBal - total);
        plugin.getStorage().setBalance(target.getUniqueId(), plugin.getStorage().getBalance(target.getUniqueId()) + amount);
        sender.sendMessage(p() + plugin.getConfig().getString("messages.pay-success", "")
                .replace("{amount}", fmt(amount)).replace("{target}", target.getName()).replace("{tax}", fmt(tax)));
        target.sendMessage(p() + plugin.getConfig().getString("messages.pay-received", "")
                .replace("{sender}", player.getName()).replace("{amount}", fmt(amount)));
        return true;
    }

    private boolean cmdTop(CommandSender sender) {
        List<Map.Entry<UUID, Double>> sorted = new ArrayList<>(plugin.getStorage().getAllBalances().entrySet());
        sorted.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        sender.sendMessage("§8§m══════════════════════════════════");
        sender.sendMessage("§a§l  TOP SỐ DƯ PIN ⚡");
        sender.sendMessage("§8§m══════════════════════════════════");
        String[] medals = {"§6[1]", "§7[2]", "§c[3]"};
        int i = 0;
        for (Map.Entry<UUID, Double> entry : sorted) {
            if (i >= 10) break;
            String medal = i < 3 ? medals[i] : "§8[" + (i+1) + "]";
            sender.sendMessage("  " + medal + " §f" + plugin.getStorage().getName(entry.getKey())
                    + " §8| §a⚡ " + fmt(entry.getValue()) + " Pin");
            i++;
        }
        sender.sendMessage("§8§m══════════════════════════════════");
        return true;
    }

    private boolean cmdAdmin(CommandSender sender, String[] args, String action) {
        if (!sender.hasPermission("pin.admin")) { sender.sendMessage(p() + "§cKhông có quyền."); return true; }
        if (args.length < 2) { sender.sendMessage(p() + "§cCú pháp: /pin " + action + " <player> [amount]"); return true; }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        UUID uuid = target.getUniqueId();
        String name = args[1];
        double amount = 0;
        if (!action.equals("reset")) {
            if (args.length < 3) { sender.sendMessage(p() + "§cCú pháp: /pin " + action + " <player> <amount>"); return true; }
            try { amount = Double.parseDouble(args[2]); }
            catch (NumberFormatException e) { sender.sendMessage(p() + "§cSố tiền không hợp lệ."); return true; }
        }
        double cur = plugin.getStorage().getBalance(uuid);
        double start = plugin.getConfig().getDouble("economy.starting-balance", 100);
        double finalAmount = amount;
        switch (action) {
            case "add" -> {
                plugin.getStorage().setBalance(uuid, cur + finalAmount);
                sender.sendMessage(p() + plugin.getConfig().getString("messages.admin-add", "")
                        .replace("{amount}", fmt(finalAmount)).replace("{player}", name));
                notifyPlayer(target, "+§a" + fmt(finalAmount) + " Pin §fđã được thêm.");
            }
            case "remove" -> {
                plugin.getStorage().setBalance(uuid, Math.max(0, cur - finalAmount));
                sender.sendMessage(p() + plugin.getConfig().getString("messages.admin-remove", "")
                        .replace("{amount}", fmt(finalAmount)).replace("{player}", name));
                notifyPlayer(target, "§c-" + fmt(finalAmount) + " Pin §fđã bị trừ.");
            }
            case "set" -> {
                plugin.getStorage().setBalance(uuid, finalAmount);
                sender.sendMessage(p() + plugin.getConfig().getString("messages.admin-set", "")
                        .replace("{amount}", fmt(finalAmount)).replace("{player}", name));
                notifyPlayer(target, "§fTài khoản set về §a" + fmt(finalAmount) + " Pin.");
            }
            case "reset" -> {
                plugin.getStorage().setBalance(uuid, start);
                sender.sendMessage(p() + plugin.getConfig().getString("messages.admin-reset", "")
                        .replace("{start}", fmt(start)).replace("{player}", name));
                notifyPlayer(target, "§fTài khoản reset về §a" + fmt(start) + " Pin.");
            }
        }
        return true;
    }

    private void notifyPlayer(OfflinePlayer target, String msg) {
        Player online = Bukkit.getPlayer(target.getUniqueId());
        if (online != null) online.sendMessage(p() + msg);
    }

    private boolean cmdReload(CommandSender sender) {
        if (!sender.hasPermission("pin.admin")) { sender.sendMessage(p() + "§cKhông có quyền."); return true; }
        plugin.reloadConfig();
        sender.sendMessage(p() + "§aReload xong.");
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§8§m══════════════════════════════════");
        sender.sendMessage("§a§l  PIN ⚡ — LỆNH");
        sender.sendMessage("§8§m══════════════════════════════════");
        sender.sendMessage("  §f/pin §8- §7Xem số dư");
        sender.sendMessage("  §f/pin balance [player] §8- §7Xem số dư");
        sender.sendMessage("  §f/pin pay <player> <amount> §8- §7Chuyển Pin");
        sender.sendMessage("  §f/pin top §8- §7Bảng xếp hạng");
        if (sender.hasPermission("pin.admin")) {
            sender.sendMessage("  §f/pin add <player> <amount> §8- §7[Admin] Thêm Pin");
            sender.sendMessage("  §f/pin remove <player> <amount> §8- §7[Admin] Trừ Pin");
            sender.sendMessage("  §f/pin set <player> <amount> §8- §7[Admin] Set Pin");
            sender.sendMessage("  §f/pin reset <player> §8- §7[Admin] Reset Pin");
            sender.sendMessage("  §f/pin reload §8- §7[Admin] Reload config");
        }
        sender.sendMessage("§8§m══════════════════════════════════");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1)
            return List.of("balance","pay","top","add","remove","set","reset","reload")
                    .stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        if (args.length == 2 && !args[0].equalsIgnoreCase("top") && !args[0].equalsIgnoreCase("reload"))
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
        return List.of();
    }
            }
