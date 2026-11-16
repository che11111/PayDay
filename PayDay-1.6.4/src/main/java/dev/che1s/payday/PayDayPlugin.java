package dev.che1s.payday;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bstats.bukkit.Metrics;

import java.util.ArrayList;
import java.util.List;

/**
 * PayDay - 付费设置白天/晴天的 Bukkit 插件
 * 通过 Vault 经济 API 扣款
 * 适用于 CraftBukkit/Spigot 1.6.4
 */
public class PayDayPlugin extends JavaPlugin {

    private static final int BSTATS_PLUGIN_ID = 35365;

    private Economy economy;
    private LanguageManager language;
    private HistoryManager historyManager;
    private double dayCost;
    private double noonCost;
    private double nightCost;
    private double dawnCost;
    private double duskCost;
    private double weatherCost;
    private double rainCost;
    private double stormCost;
    private long dayTicks;
    private long noonTicks;
    private long nightTicks;
    private long dawnTicks;
    private long duskTicks;
    private long weatherDuration;
    private Metrics metrics;

    @Override
    public void onEnable() {
        // 保存默认配置并加载语言
        saveDefaultConfig();
        language = new LanguageManager(this);

        // 检查 Vault 是否可用
        if (!setupEconomy()) {
            getLogger().severe(language.console("log.economy_missing"));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        loadConfigValues();

        // 注册命令执行器
        getCommand("payday").setExecutor(this);
        getCommand("paynoon").setExecutor(this);
        getCommand("paynight").setExecutor(this);
        getCommand("paydawn").setExecutor(this);
        getCommand("paydusk").setExecutor(this);
        getCommand("payweather").setExecutor(this);
        getCommand("payrain").setExecutor(this);
        getCommand("paystorm").setExecutor(this);
        getCommand("paydayadmin").setExecutor(this);

        historyManager = new HistoryManager(getDataFolder(), getLogger());
        if (!historyManager.isAvailable()) {
            getLogger().severe(language.console("log.database_missing"));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getLogger().info(language.console("log.enabled", "version", getDescription().getVersion()));
        logPrice("type.day", dayCost);
        logPrice("type.noon", noonCost);
        logPrice("type.clear", weatherCost);
        logPrice("type.rain", rainCost);
        logPrice("type.storm", stormCost);
        logPrice("type.night", nightCost);
        logPrice("type.dawn", dawnCost);
        logPrice("type.dusk", duskCost);
        startMetrics();
    }

    @Override
    public void onDisable() {
        if (historyManager != null) {
            historyManager.close();
        }
        if (metrics != null) {
            metrics.shutdown();
            metrics = null;
        }
        if (language != null) {
            getLogger().info(language.console("log.disabled"));
        }
    }

    public static PayDayPlugin getInstance() {
        try {
            return (PayDayPlugin) Bukkit.getServer().getPluginManager().getPlugin("PayDay");
        } catch (Exception exception) {
            return null;
        }
    }

    public int getPayDayCount(String playerName) {
        return historyManager == null ? 0 : historyManager.getPayDayCount(playerName);
    }

    public int getPayNoonCount(String playerName) {
        return historyManager == null ? 0 : historyManager.getPayNoonCount(playerName);
    }

    public int getPayNightCount(String playerName) {
        return historyManager == null ? 0 : historyManager.getPayNightCount(playerName);
    }

    public int getPayWeatherCount(String playerName) {
        return historyManager == null ? 0 : historyManager.getPayWeatherCount(playerName);
    }

    public int getCount(String playerName, String command) {
        return historyManager == null ? 0 : historyManager.getCount(playerName, command);
    }

    public List<String> getHistory(String playerName) {
        return historyManager == null
                ? new ArrayList<String>()
                : historyManager.getHistory(playerName);
    }

    private void startMetrics() {
        try {
            metrics = new Metrics(this, BSTATS_PLUGIN_ID);
        } catch (RuntimeException | LinkageError exception) {
            metrics = null;
            getLogger().warning("Could not start bStats: " + exception.getMessage());
        }
    }

    private void logPrice(String typeKey, double cost) {
        getLogger().info(language.console("log.price",
                "type", language.console(typeKey),
                "cost", economy.format(cost)));
    }

    /**
     * 初始化 Vault 经济 API
     */
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    /**
     * 加载配置文件
     */
    private void loadConfigValues() {
        getConfig().addDefault("costs.time.day", 30.0D);
        getConfig().addDefault("costs.time.noon", 30.0D);
        getConfig().addDefault("costs.time.night", 30.0D);
        getConfig().addDefault("costs.time.dawn", 30.0D);
        getConfig().addDefault("costs.time.dusk", 30.0D);
        getConfig().addDefault("costs.weather.clear", 30.0D);
        getConfig().addDefault("costs.weather.rain", 30.0D);
        getConfig().addDefault("costs.weather.storm", 30.0D);
        getConfig().addDefault("times.day", 1000L);
        getConfig().addDefault("times.noon", 6000L);
        getConfig().addDefault("times.night", 18000L);
        getConfig().addDefault("times.dawn", 0L);
        getConfig().addDefault("times.dusk", 12000L);
        getConfig().addDefault("weather_duration", 12000L);
        getConfig().options().copyDefaults(true);
        saveConfig();

        dayCost = getConfig().getDouble("costs.time.day",
                getConfig().getDouble("costs.day", 30.0D));
        noonCost = getConfig().getDouble("costs.time.noon",
                getConfig().getDouble("costs.noon", 30.0D));
        nightCost = getConfig().getDouble("costs.time.night",
                getConfig().getDouble("costs.night", 30.0D));
        dawnCost = getConfig().getDouble("costs.time.dawn", 30.0D);
        duskCost = getConfig().getDouble("costs.time.dusk", 30.0D);
        weatherCost = getConfig().getDouble("costs.weather.clear",
                getConfig().getDouble("costs.clear", 30.0D));
        rainCost = getConfig().getDouble("costs.weather.rain", 30.0D);
        stormCost = getConfig().getDouble("costs.weather.storm", 30.0D);
        dayTicks = getConfig().getLong("times.day", 1000L);
        noonTicks = getConfig().getLong("times.noon", 6000L);
        nightTicks = getConfig().getLong("times.night", 18000L);
        dawnTicks = getConfig().getLong("times.dawn", 0L);
        duskTicks = getConfig().getLong("times.dusk", 12000L);
        weatherDuration = getConfig().getLong("weather_duration", 12000L);
        language.reload();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String commandName = cmd.getName().toLowerCase();

        switch (commandName) {
            case "payday":
                return handlePayDay(sender);
            case "paynoon":
                return handlePayNoon(sender);
            case "paynight":
                return handlePayNight(sender);
            case "paydawn":
                return handlePayDawn(sender);
            case "paydusk":
                return handlePayDusk(sender);
            case "payweather":
                return handlePayWeather(sender);
            case "payrain":
                return handlePayRain(sender);
            case "paystorm":
                return handlePayStorm(sender);
            case "paydayadmin":
                return handleAdminCommand(sender, args);
            default:
                return false;
        }
    }

    // ==================== /payday 命令 ====================

    /**
     * /payday - 付费设置玩家当前世界的白天
     */
    private boolean handlePayDay(CommandSender sender) {
        return handleTimePurchase(
                sender, "/payday", dayCost, dayTicks, "type.day", "payday.day");
    }

    private boolean handlePayNoon(CommandSender sender) {
        return handleTimePurchase(
                sender, "/paynoon", noonCost, noonTicks, "type.noon", "payday.noon");
    }

    private boolean handlePayNight(CommandSender sender) {
        return handleTimePurchase(
                sender, "/paynight", nightCost, nightTicks, "type.night", "payday.night");
    }

    private boolean handlePayDawn(CommandSender sender) {
        return handleTimePurchase(
                sender, "/paydawn", dawnCost, dawnTicks, "type.dawn", "payday.dawn");
    }

    private boolean handlePayDusk(CommandSender sender) {
        return handleTimePurchase(
                sender, "/paydusk", duskCost, duskTicks, "type.dusk", "payday.dusk");
    }

    private boolean handleTimePurchase(CommandSender sender, String commandName,
                                       double cost, long ticks, String typeKey,
                                       String permission) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(language.get(sender, "message.no_console"));
            return true;
        }

        Player player = (Player) sender;

        // 检查权限
        if (!player.hasPermission(permission)) {
            player.sendMessage(language.get(player, "message.no_permission"));
            return true;
        }

        String playerName = player.getName();

        // 通过 Vault 检查余额
        if (!economy.has(playerName, cost)) {
            player.sendMessage(language.get(player, "message.not_enough_money",
                    "cost", economy.format(cost)));
            player.sendMessage(language.get(player, "message.current_balance",
                    "balance", economy.format(economy.getBalance(playerName))));
            return true;
        }

        // 通过 Vault 扣款
        EconomyResponse payment = economy.withdrawPlayer(playerName, cost);
        if (payment == null || !payment.transactionSuccess()) {
            player.sendMessage(language.get(player, "message.payment_failed"));
            getLogger().warning(language.console("log.payment_failed",
                    "player", playerName,
                    "cost", economy.format(cost),
                    "error", payment == null ? "unknown economy error" : payment.errorMessage));
            return true;
        }

        // 只修改玩家当前所在世界，避免影响默认世界
        World world = player.getWorld();
        world.setTime(ticks);

        player.sendMessage(language.get(player, "message.time_success",
                "cost", economy.format(cost), "action", language.get(player, typeKey)));
        player.sendMessage(language.get(player, "message.purchase_balance",
                "balance", economy.format(economy.getBalance(playerName))));

        // 全服广播
        broadcast(player, "message.time_broadcast", commandName, typeKey);

        getLogger().info(language.console("log.time_purchase",
                "player", playerName,
                "cost", economy.format(cost),
                "action", language.console(typeKey)));
        historyManager.recordExecution(
                playerName, commandName, historyManager.getCurrentTimeString());

        return true;
    }

    // ==================== /payweather 命令 ====================

    /**
     * /payweather - 付费设置玩家当前世界的晴天
     */
    private boolean handlePayWeather(CommandSender sender) {
        return handleWeatherPurchase(
                sender, "/payweather", weatherCost, "clear", "type.clear");
    }

    private boolean handlePayRain(CommandSender sender) {
        return handleWeatherPurchase(
                sender, "/payrain", rainCost, "rain", "type.rain");
    }

    private boolean handlePayStorm(CommandSender sender) {
        return handleWeatherPurchase(
                sender, "/paystorm", stormCost, "storm", "type.storm");
    }

    private boolean handleWeatherPurchase(CommandSender sender, String commandName,
                                          double cost, String weatherType, String typeKey) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(language.get(sender, "message.no_console"));
            return true;
        }

        Player player = (Player) sender;

        // 检查权限
        if (!player.hasPermission("payday.weather")) {
            player.sendMessage(language.get(player, "message.no_permission"));
            return true;
        }

        String playerName = player.getName();

        // 通过 Vault 检查余额
        if (!economy.has(playerName, cost)) {
            player.sendMessage(language.get(player, "message.not_enough_money",
                    "cost", economy.format(cost)));
            player.sendMessage(language.get(player, "message.current_balance",
                    "balance", economy.format(economy.getBalance(playerName))));
            return true;
        }

        // 通过 Vault 扣款
        EconomyResponse payment = economy.withdrawPlayer(playerName, cost);
        if (payment == null || !payment.transactionSuccess()) {
            player.sendMessage(language.get(player, "message.payment_failed"));
            getLogger().warning(language.console("log.payment_failed",
                    "player", playerName,
                    "cost", economy.format(cost),
                    "error", payment == null ? "unknown economy error" : payment.errorMessage));
            return true;
        }

        // 只修改玩家当前所在世界，避免影响默认世界
        World world = player.getWorld();
        if ("clear".equals(weatherType)) {
            world.setStorm(false);
            world.setThundering(false);
        } else if ("rain".equals(weatherType)) {
            world.setStorm(true);
            world.setThundering(false);
            world.setWeatherDuration((int) weatherDuration);
        } else {
            world.setStorm(true);
            world.setThundering(true);
            world.setWeatherDuration((int) weatherDuration);
            world.setThunderDuration((int) weatherDuration);
        }

        player.sendMessage(language.get(player, "message.weather_success",
                "cost", economy.format(cost), "action", language.get(player, typeKey)));
        player.sendMessage(language.get(player, "message.purchase_balance",
                "balance", economy.format(economy.getBalance(playerName))));

        // 全服广播
        broadcast(player, "message.weather_broadcast", commandName, typeKey);

        getLogger().info(language.console("log.weather_purchase",
                "player", playerName,
                "cost", economy.format(cost),
                "action", language.console(typeKey)));
        historyManager.recordExecution(
                playerName, commandName, historyManager.getCurrentTimeString());

        return true;
    }

    private void broadcast(Player buyer, String key, String command, String typeKey) {
        for (Player recipient : getServer().getOnlinePlayers()) {
            recipient.sendMessage(language.get(recipient, key,
                    "player", buyer.getName(),
                    "command", command,
                    "action", language.get(recipient, typeKey)));
        }
    }

    private boolean handleAdminCommand(CommandSender sender, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("payday.reload")) {
                sender.sendMessage(language.get(sender, "message.no_permission"));
                return true;
            }
            reloadConfig();
            loadConfigValues();
            if (historyManager != null) {
                historyManager.reload();
            }
            sender.sendMessage(language.get(sender, "message.reload"));
            getLogger().info(language.console("log.reload", "player", sender.getName()));
            return true;
        }
        sender.sendMessage(language.get(sender, "message.usage_admin"));
        return true;
    }
}
