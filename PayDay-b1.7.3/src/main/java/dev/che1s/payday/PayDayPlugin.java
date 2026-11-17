package dev.che1s.payday;

import com.iConomy.iConomy;
import com.iConomy.system.Account;
import com.iConomy.system.Holdings;
import dev.che1s.payday.metrics.BukkitMetrics;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * PayDay - Pay to set time / weather
 * Minecraft Beta 1.7.3 (Poseidon) + iConomy
 */
public class PayDayPlugin extends JavaPlugin {

    private static final int BSTATS_PLUGIN_ID = 35365;

    private Logger log;
    private Configuration config;
    private LanguageService language;
    private HistoryManager historyManager;

    private double dayCost;
    private double noonCost;
    private double nightCost;
    private double dawnCost;
    private double duskCost;
    private double weatherCost;
    private double rainCost;
    private double stormCost;
    private int dayTicks;
    private int noonTicks;
    private int nightTicks;
    private int dawnTicks;
    private int duskTicks;
    private int weatherDuration;
    private BukkitMetrics metrics;

    private boolean iConomyEnabled = false;

    @Override
    public void onEnable() {
        log = getServer().getLogger();

        // ========== Load config ==========
        loadConfig();
        language = new LanguageService(this);
        language.load();

        // ========== Check iConomy ==========
        if (getServer().getPluginManager().getPlugin("iConomy") == null
                || !getServer().getPluginManager().getPlugin("iConomy").isEnabled()) {
            language.severe("log.economy_missing", "iConomy not found! Plugin disabled.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        iConomyEnabled = true;

        // ========== Register commands ==========
        getCommand("payday").setExecutor(this);
        getCommand("paynoon").setExecutor(this);
        getCommand("paynight").setExecutor(this);
        getCommand("paydawn").setExecutor(this);
        getCommand("paydusk").setExecutor(this);
        getCommand("payweather").setExecutor(this);
        getCommand("payrain").setExecutor(this);
        getCommand("paystorm").setExecutor(this);
        getCommand("paydayadmin").setExecutor(this);

        // ========== Init execution history ==========
        historyManager = new HistoryManager(getDataFolder(), log);
        if (!historyManager.isAvailable()) {
            language.severe("log.database_missing",
                    "SQLite history is unavailable. PayDay has been disabled.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        language.info("log.enabled", "PayDay v{version} enabled.",
                "version", getDescription().getVersion());
        logPrice("type.day", "Day", dayCost);
        logPrice("type.noon", "Noon", noonCost);
        logPrice("type.clear", "Clear Weather", weatherCost);
        logPrice("type.rain", "Rain", rainCost);
        logPrice("type.storm", "Thunderstorm", stormCost);
        logPrice("type.night", "Night", nightCost);
        logPrice("type.dawn", "Dawn", dawnCost);
        logPrice("type.dusk", "Dusk", duskCost);
        startMetrics();
    }

    // ==================== Config loader (reusable for reload) ====================

    private void startMetrics() {
        try {
            metrics = new BukkitMetrics(this, BSTATS_PLUGIN_ID);
        } catch (Exception | LinkageError exception) {
            metrics = null;
            log.warning("Could not start bStats: " + exception.getMessage());
        }
    }

    private void logPrice(String typeKey, String fallbackType, double cost) {
        language.info("log.price", "{type} price: {cost}",
                "type", language.consoleValue(typeKey, fallbackType),
                "cost", formatMoney(cost));
    }

    private void loadConfig() {
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            getDataFolder().mkdirs();
            try {
                InputStream in = getClass().getClassLoader().getResourceAsStream("config.yml");
                if (in != null) {
                    OutputStream out = new FileOutputStream(configFile);
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                    out.close();
                    in.close();
                }
            } catch (Exception e) {
                log.severe("Failed to save default config: " + e.getMessage());
            }
        }
        config = new Configuration(configFile);
        config.load();

        // ========== Read settings ==========
        dayCost = config.getDouble("costs.time.day",
                config.getDouble("costs.day", config.getDouble("day_cost", 30.0D)));
        noonCost = config.getDouble("costs.time.noon",
                config.getDouble("costs.noon", config.getDouble("noon_cost", 30.0D)));
        nightCost = config.getDouble("costs.time.night",
                config.getDouble("costs.night", config.getDouble("night_cost", 30.0D)));
        dawnCost = config.getDouble("costs.time.dawn", 30.0D);
        duskCost = config.getDouble("costs.time.dusk", 30.0D);
        weatherCost = config.getDouble("costs.weather.clear",
                config.getDouble("costs.clear", config.getDouble("weather_cost", 30.0D)));
        rainCost = config.getDouble("costs.weather.rain", 30.0D);
        stormCost = config.getDouble("costs.weather.storm", 30.0D);
        dayTicks = config.getInt("times.day", 1000);
        noonTicks = config.getInt("times.noon", 6000);
        nightTicks = config.getInt("times.night", 18000);
        dawnTicks = config.getInt("times.dawn", 0);
        duskTicks = config.getInt("times.dusk", 12000);
        weatherDuration = config.getInt("weather_duration", 12000);

        if (language != null) {
            language.load();
        }
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
            language.info("log.disabled", "PayDay disabled!");
        }
    }

    // ==================== Public API (供其它插件调用) ====================

    /**
     * 获取 PayDay 插件实例（供其它插件 hook 使用）。
     */
    public static PayDayPlugin getInstance() {
        try {
            return (PayDayPlugin) Bukkit.getServer().getPluginManager().getPlugin("PayDay");
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 接口1：获取玩家累计执行 /payday 的次数。
     */
    public int getPayDayCount(String playerName) {
        if (historyManager == null) return 0;
        return historyManager.getPayDayCount(playerName);
    }

    /**
     * 接口2：获取玩家累计执行 /payweather 的次数。
     */
    public int getPayWeatherCount(String playerName) {
        if (historyManager == null) return 0;
        return historyManager.getPayWeatherCount(playerName);
    }

    public int getPayNoonCount(String playerName) {
        if (historyManager == null) return 0;
        return historyManager.getPayNoonCount(playerName);
    }

    public int getPayNightCount(String playerName) {
        if (historyManager == null) return 0;
        return historyManager.getPayNightCount(playerName);
    }

    public int getCount(String playerName, String command) {
        if (historyManager == null) return 0;
        return historyManager.getCount(playerName, command);
    }

    /**
     * 接口3：获取玩家历史执行记录列表（按执行先后排列）。
     * 每条记录格式：在 yyyy-MM-dd HH:mm:ss 执行了 /payday（或 /payweather）
     */
    public List<String> getHistory(String playerName) {
        if (historyManager == null) return new ArrayList<String>();
        return historyManager.getHistory(playerName);
    }

    // ==================== iConomy helpers ====================

    private Account getAccount(String playerName) {
        if (!iConomyEnabled) return null;
        return iConomy.getAccount(playerName);
    }

    private double getBalance(String playerName) {
        Account account = getAccount(playerName);
        if (account == null) return 0.0;
        Holdings holdings = account.getHoldings();
        if (holdings == null) return 0.0;
        return holdings.balance();
    }

    private boolean hasEnough(String playerName, double amount) {
        Account account = getAccount(playerName);
        if (account == null) return false;
        Holdings holdings = account.getHoldings();
        if (holdings == null) return false;
        return holdings.hasEnough(amount);
    }

    private boolean takeMoney(String playerName, double amount) {
        Account account = getAccount(playerName);
        if (account == null) return false;
        Holdings holdings = account.getHoldings();
        if (holdings == null) return false;
        if (!holdings.hasEnough(amount)) return false;
        double before = holdings.balance();
        try {
            holdings.subtract(amount);
        } catch (RuntimeException exception) {
            log.warning("iConomy withdrawal failed for " + playerName + ": "
                    + exception.getMessage());
            return false;
        }
        double after = holdings.balance();
        return after <= before - amount + 0.000001D;
    }

    private String formatMoney(double amount) {
        return iConomy.format(amount);
    }

    // ==================== Command router ====================

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String commandName = cmd.getName().toLowerCase();

        if (commandName.equals("payday")) {
            return handlePayDay(sender);
        } else if (commandName.equals("paynoon")) {
            return handlePayNoon(sender);
        } else if (commandName.equals("paynight")) {
            return handlePayNight(sender);
        } else if (commandName.equals("paydawn")) {
            return handlePayDawn(sender);
        } else if (commandName.equals("paydusk")) {
            return handlePayDusk(sender);
        } else if (commandName.equals("payweather")) {
            return handlePayWeather(sender);
        } else if (commandName.equals("payrain")) {
            return handlePayRain(sender);
        } else if (commandName.equals("paystorm")) {
            return handlePayStorm(sender);
        } else if (commandName.equals("paydayadmin")) {
            return handleAdminCommand(sender, args);
        }
        return false;
    }

    // ==================== /payday ====================

    private boolean handlePayDay(CommandSender sender) {
        return handleTimePurchase(sender, "/payday", dayCost, dayTicks,
                "payday.day", "type.day", "Day");
    }

    private boolean handlePayNoon(CommandSender sender) {
        return handleTimePurchase(sender, "/paynoon", noonCost, noonTicks,
                "payday.noon", "type.noon", "Noon");
    }

    private boolean handlePayNight(CommandSender sender) {
        return handleTimePurchase(sender, "/paynight", nightCost, nightTicks,
                "payday.night", "type.night", "Night");
    }

    private boolean handlePayDawn(CommandSender sender) {
        return handleTimePurchase(sender, "/paydawn", dawnCost, dawnTicks,
                "payday.dawn", "type.dawn", "Dawn");
    }

    private boolean handlePayDusk(CommandSender sender) {
        return handleTimePurchase(sender, "/paydusk", duskCost, duskTicks,
                "payday.dusk", "type.dusk", "Dusk");
    }

    private boolean handleTimePurchase(CommandSender sender, String commandName,
                                       double cost, long ticks, String permission,
                                       String typeKey, String fallbackType) {
        if (!(sender instanceof Player)) {
            language.send(sender, "message.no_console",
                    "&cThis command can only be executed by a player!");
            return true;
        }
        Player player = (Player) sender;

        if (!player.hasPermission(permission)) {
            language.send(sender, "message.no_permission",
                    "&cYou don't have permission to use this command!");
            return true;
        }

        if (!hasEnough(player.getName(), cost)) {
            language.send(sender, "message.not_enough_money",
                    "&cYou don't have enough money! Need &6{cost}",
                    "cost", formatMoney(cost));
            language.send(sender, "message.current_balance",
                    "&cCurrent balance: &6{balance}",
                    "balance", formatMoney(getBalance(player.getName())));
            return true;
        }

        if (!takeMoney(player.getName(), cost)) {
            language.send(sender, "message.payment_failed",
                    "&cPayment failed. No weather or time change was made.");
            language.warning("log.payment_failed",
                    "Payment of {cost} by {player} failed.",
                    "player", player.getName(),
                    "cost", formatMoney(cost));
            return true;
        }
        player.getWorld().setTime(ticks);
        language.send(sender, "message.time_success",
                "&aYou paid &6{cost} &aand set the time to {action}!",
                "cost", formatMoney(cost),
                "action", language.gameValue(typeKey, fallbackType));
        language.send(sender, "message.purchase_balance",
                "&aRemaining balance: &6{balance}",
                "balance", formatMoney(getBalance(player.getName())));
        broadcast("message.time_broadcast", player.getName(), commandName, typeKey, fallbackType);
        language.info("log.time_purchase",
                "{player} paid {cost} to set the time to {action}.",
                "player", player.getName(),
                "cost", formatMoney(cost),
                "action", language.consoleValue(typeKey, fallbackType));
        historyManager.recordExecution(
                player.getName(), commandName, historyManager.getCurrentTimeString());
        return true;
    }

    // ==================== /payweather ====================

    private boolean handlePayWeather(CommandSender sender) {
        return handleWeatherPurchase(sender, "/payweather", weatherCost,
                "clear", "type.clear", "Clear Weather");
    }

    private boolean handlePayRain(CommandSender sender) {
        return handleWeatherPurchase(sender, "/payrain", rainCost,
                "rain", "type.rain", "Rain");
    }

    private boolean handlePayStorm(CommandSender sender) {
        return handleWeatherPurchase(sender, "/paystorm", stormCost,
                "storm", "type.storm", "Thunderstorm");
    }

    private boolean handleWeatherPurchase(CommandSender sender, String commandName,
                                          double cost, String weatherType,
                                          String typeKey, String fallbackType) {
        if (!(sender instanceof Player)) {
            language.send(sender, "message.no_console",
                    "&cThis command can only be executed by a player!");
            return true;
        }
        Player player = (Player) sender;

        if (!player.hasPermission("payday.weather")) {
            language.send(sender, "message.no_permission",
                    "&cYou don't have permission to use this command!");
            return true;
        }

        if (!hasEnough(player.getName(), cost)) {
            language.send(sender, "message.not_enough_money",
                    "&cYou don't have enough money! Need &6{cost}",
                    "cost", formatMoney(cost));
            language.send(sender, "message.current_balance",
                    "&cCurrent balance: &6{balance}",
                    "balance", formatMoney(getBalance(player.getName())));
            return true;
        }

        if (!takeMoney(player.getName(), cost)) {
            language.send(sender, "message.payment_failed",
                    "&cPayment failed. No weather or time change was made.");
            language.warning("log.payment_failed",
                    "Payment of {cost} by {player} failed.",
                    "player", player.getName(),
                    "cost", formatMoney(cost));
            return true;
        }
        World world = player.getWorld();
        if ("clear".equals(weatherType)) {
            world.setStorm(false);
            world.setThundering(false);
        } else if ("rain".equals(weatherType)) {
            world.setStorm(true);
            world.setThundering(false);
            world.setWeatherDuration(weatherDuration);
        } else {
            world.setStorm(true);
            world.setThundering(true);
            world.setWeatherDuration(weatherDuration);
            world.setThunderDuration(weatherDuration);
        }
        language.send(sender, "message.weather_success",
                "&aYou paid &6{cost} &aand set the weather to {action}!",
                "cost", formatMoney(cost),
                "action", language.gameValue(typeKey, fallbackType));
        language.send(sender, "message.purchase_balance",
                "&aRemaining balance: &6{balance}",
                "balance", formatMoney(getBalance(player.getName())));
        broadcast("message.weather_broadcast", player.getName(),
                commandName, typeKey, fallbackType);
        language.info("log.weather_purchase",
                "{player} paid {cost} to set the weather to {action}.",
                "player", player.getName(),
                "cost", formatMoney(cost),
                "action", language.consoleValue(typeKey, fallbackType));
        historyManager.recordExecution(
                player.getName(), commandName, historyManager.getCurrentTimeString());
        return true;
    }

    // ==================== /paydayadmin reload ====================

    private boolean handleAdminCommand(CommandSender sender, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("payday.reload")) {
                language.send(sender, "message.no_permission",
                        "&cYou don't have permission to use this command!");
                return true;
            }
            loadConfig();
            if (historyManager != null) {
                historyManager.reload();
            }
            language.send(sender, "message.reload",
                    "&aPayDay configuration and language files reloaded.");
            language.info("log.reload",
                    "{player} reloaded the PayDay configuration.",
                    "player", sender.getName());
            return true;
        }
        language.send(sender, "message.usage_admin",
                "&cUsage: /paydayadmin reload");
        return true;
    }

    private void broadcast(String key, String player, String command,
                           String typeKey, String fallbackType) {
        String message = language.game(key,
                "&6{player} &eused &6{command}&e to bring &6{action}&e to everyone!",
                "player", player,
                "command", command,
                "action", language.gameValue(typeKey, fallbackType));
        getServer().broadcastMessage(message);
    }
}
