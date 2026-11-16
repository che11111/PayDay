package dev.che1s.payday;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bstats.bukkit.Metrics;

/**
 * PayDay - 付费设置天气/时间的 Bukkit 插件
 * 通过 Vault 经济 API 扣款
 * Java 8 字节码，适用于 Bukkit/Spigot 1.8 至 Paper 26.3
 * 原生 Dialog 仅在 Paper 1.21.7+ 动态启用
 */
@SuppressWarnings("deprecation")
public class PayDayPlugin extends JavaPlugin {

    private static final int BSTATS_PLUGIN_ID = 35365;

    private Logger log;
    private Economy economy;
    private LanguageManager language;
    private HistoryManager historyManager;
    private BedrockFormManager bedrockFormManager;
    private PurchaseMenu dialogMenu;
    private Metrics metrics;

    private long weatherDuration;

    @Override
    public void onEnable() {
        log = getLogger();
        saveDefaultConfig();
        language = new LanguageManager(this);

        if (!setupEconomy()) {
            log.severe(language.console("log.economy_missing"));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        loadConfigValues();

        getCommand("payday").setExecutor(this);
        getCommand("paynoon").setExecutor(this);
        getCommand("payweather").setExecutor(this);
        getCommand("payrain").setExecutor(this);
        getCommand("paystorm").setExecutor(this);
        getCommand("paynight").setExecutor(this);
        getCommand("paydawn").setExecutor(this);
        getCommand("paydusk").setExecutor(this);
        getCommand("paydayadmin").setExecutor(this);
        getCommand("pform").setExecutor(this);
        getCommand("paydaydialog").setExecutor(this);

        historyManager = new HistoryManager(getDataFolder(), log);
        if (!historyManager.isAvailable()) {
            log.severe("SQLite is unavailable. PayDay has been disabled.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (getServer().getPluginManager().getPlugin("floodgate") != null) {
            try {
                bedrockFormManager = new BedrockFormManager();
                log.info(language.console("log.floodgate_enabled"));
            } catch (Exception e) {
                bedrockFormManager = null;
                log.warning(language.console("log.floodgate_init_failed", "error", e.getMessage()));
            }
        } else {
            bedrockFormManager = null;
            log.info(language.console("log.floodgate_missing"));
        }

        if (isDialogApiAvailable()) {
            try {
                dialogMenu = loadDialogMenu();
                log.info(language.console("log.dialog_enabled"));
            } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
                dialogMenu = null;
                log.warning(language.console("log.dialog_init_failed", "error", e.getMessage()));
            }
        } else {
            dialogMenu = null;
            log.info(language.console("log.dialog_missing"));
        }

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PayDayPlaceholderExpansion(this).register();
            log.info(language.console("log.placeholder_registered"));
        }

        log.info(language.console("log.enabled", "version", getDescription().getVersion()));
        for (PurchaseType type : PurchaseType.values()) {
            log.info(language.console("log.price",
                    "type", language.console(type.getTranslationKey()),
                    "cost", formatMoney(getCost(type))));
        }
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
        if (log != null && language != null) {
            log.info(language.console("log.disabled"));
        }
    }

    // ==================== Config ====================

    private void startMetrics() {
        try {
            metrics = new Metrics(this, BSTATS_PLUGIN_ID);
        } catch (RuntimeException | LinkageError exception) {
            metrics = null;
            log.warning("Could not start bStats: " + exception.getMessage());
        }
    }

    private void loadConfigValues() {
        getConfig().options().copyDefaults(true);
        saveConfig();

        weatherDuration = getConfig().getLong("weather_duration", 12000L);

        language.reload();
    }

    private double getCost(PurchaseType type) {
        String category = type.isTimePurchase() ? "time" : "weather";
        return getConfig().getDouble("costs." + category + "." + type.getKey(),
                getConfig().getDouble("costs." + type.getKey(), 30.0));
    }

    private long getTime(PurchaseType type) {
        long fallback;
        switch (type) {
            case NIGHT:
                fallback = 18000L;
                break;
            case DAWN:
                fallback = 0L;
                break;
            case DUSK:
                fallback = 12000L;
                break;
            case NOON:
                fallback = 6000L;
                break;
            case DAY:
            default:
                fallback = 1000L;
                break;
        }
        return getConfig().getLong("times." + type.getKey(), fallback);
    }

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

    // ==================== Public API (供其它插件调用) ====================

    public static PayDayPlugin getInstance() {
        try {
            return (PayDayPlugin) Bukkit.getServer().getPluginManager().getPlugin("PayDay");
        } catch (Exception e) {
            return null;
        }
    }

    public int getPayDayCount(String playerName) {
        if (historyManager == null) return 0;
        return historyManager.getPayDayCount(playerName);
    }

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

    public List<String> getHistory(String playerName) {
        if (historyManager == null) return new ArrayList<String>();
        return historyManager.getHistory(playerName);
    }

    // ==================== Economy helpers ====================

    private boolean hasEnough(Player player, double amount) {
        return economy != null && economy.has(player, amount);
    }

    private EconomyResponse takeMoney(Player player, double amount) {
        if (economy == null) {
            return null;
        }
        return economy.withdrawPlayer(player, amount);
    }

    private double getBalance(OfflinePlayer player) {
        if (economy == null) return 0.0;
        return economy.getBalance(player);
    }

    private String formatMoney(double amount) {
        if (economy != null) {
            return economy.format(amount);
        }
        return String.format("%.2f", amount);
    }

    // ==================== Command router ====================

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String commandName = cmd.getName().toLowerCase();

        PurchaseType type = PurchaseType.fromCommandName(commandName);
        if (type != null) {
            return handlePurchase(sender, type);
        }

        switch (commandName) {
            case "paydayadmin":
                return handleAdminCommand(sender, args);
            case "paydaydialog":
                return handleDialogMenu(sender);
            case "pform":
                return handlePaydayForm(sender);
            default:
                return false;
        }
    }

    // ==================== 购买逻辑 ====================

    private boolean handlePurchase(CommandSender sender, PurchaseType type) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(language.get(sender, "message.no_console"));
            return true;
        }
        Player player = (Player) sender;
        doPurchase(player, type);
        return true;
    }

    private void doPurchase(Player player, PurchaseType type) {
        if (!player.hasPermission(type.getPermission())) {
            player.sendMessage(language.player(player, "message.no_permission"));
            return;
        }

        double cost = getCost(type);
        if (!hasEnough(player, cost)) {
            player.sendMessage(language.player(player, "message.not_enough_money",
                    "cost", formatMoney(cost)));
            player.sendMessage(language.player(player, "message.current_balance",
                    "balance", formatMoney(getBalance(player))));
            return;
        }

        EconomyResponse payment = takeMoney(player, cost);
        if (payment == null || !payment.transactionSuccess()) {
            String error = payment == null || payment.errorMessage == null
                    || payment.errorMessage.isEmpty()
                    ? "unknown economy error"
                    : payment.errorMessage;
            log.warning(language.console("log.payment_failed",
                    "player", player.getName(),
                    "cost", formatMoney(cost),
                    "error", error));
            player.sendMessage(language.player(player, "message.payment_failed"));
            return;
        }

        applyEffect(player.getWorld(), type);

        player.sendMessage(language.player(player, "message.purchase_success",
                "cost", formatMoney(cost),
                "action", language.player(player, type.getTranslationKey())));
        player.sendMessage(language.player(player, "message.purchase_balance",
                "balance", formatMoney(getBalance(player))));
        broadcastPurchase(player, type);
        log.info(language.console("log.purchase",
                "player", player.getName(),
                "cost", formatMoney(cost),
                "action", language.console(type.getTranslationKey())));
        historyManager.recordExecution(player.getName(), type.getCommand(), historyManager.getCurrentTimeString());
    }

    private void broadcastPurchase(Player buyer, PurchaseType type) {
        String buyerName = buyer.getName();
        for (Player recipient : getServer().getOnlinePlayers()) {
            SchedulerCompat.runForPlayer(this, recipient, () -> {
                if (!recipient.isOnline()) {
                    return;
                }
                recipient.sendMessage(language.player(recipient, "message.purchase_broadcast",
                        "player", buyerName,
                        "command", type.getCommand(),
                        "action", language.player(recipient, type.getTranslationKey())));
            });
        }
    }

    private void applyEffect(World world, PurchaseType type) {
        switch (type) {
            case DAY:
            case NOON:
            case NIGHT:
            case DAWN:
            case DUSK:
                world.setTime(getTime(type));
                break;
            case CLEAR:
                world.setStorm(false);
                world.setThundering(false);
                break;
            case RAIN:
                world.setStorm(true);
                world.setThundering(false);
                world.setWeatherDuration((int) weatherDuration);
                break;
            case STORM:
                world.setStorm(true);
                world.setThundering(true);
                world.setWeatherDuration((int) weatherDuration);
                world.setThunderDuration((int) weatherDuration);
                break;
            default:
                break;
        }
    }

    // ==================== /paydaydialog ====================

    private boolean handleDialogMenu(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(language.get(sender, "message.no_console"));
            return true;
        }
        Player player = (Player) sender;

        if (!player.hasPermission("payday.dialog")) {
            player.sendMessage(language.player(player, "message.no_permission"));
            return true;
        }

        if (dialogMenu == null) {
            player.sendMessage(language.player(player, "message.dialog_not_supported"));
            return true;
        }

        try {
            dialogMenu.open(player,
                    language.player(player, "menu.title"),
                    language.player(player, "menu.description"),
                    type -> language.player(player, type.getTranslationKey()),
                    type -> language.player(player, "menu.button_detail",
                            "cost", formatMoney(getCost(type)),
                            "count", String.valueOf(getCount(player.getName(), type.getCommand()))),
                    language.player(player, "menu.close"),
                    language.player(player, "menu.close_tooltip"),
                    type -> SchedulerCompat.runForPlayer(
                            this, player, () -> doPurchase(player, type)));
        } catch (LinkageError | RuntimeException e) {
            log.warning(language.console("log.dialog_open_failed", "error", e.getMessage()));
            player.sendMessage(language.player(player, "message.dialog_not_supported"));
        }
        return true;
    }

    // ==================== /pform ====================

    private boolean handlePaydayForm(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(language.get(sender, "message.no_console"));
            return true;
        }
        Player player = (Player) sender;

        if (!player.hasPermission("payday.form")) {
            player.sendMessage(language.player(player, "message.no_permission"));
            return true;
        }

        if (bedrockFormManager == null || !bedrockFormManager.isBedrockPlayer(player.getUniqueId())) {
            player.sendMessage(language.player(player, "message.form_not_bedrock"));
            return true;
        }

        bedrockFormManager.openWeatherForm(player,
                language.player(player, "menu.title"),
                type -> language.player(player, type.getTranslationKey()) + "\n"
                        + language.player(player, "menu.button_detail",
                                "cost", formatMoney(getCost(type)),
                                "count", String.valueOf(getCount(player.getName(), type.getCommand()))),
                type -> SchedulerCompat.runForPlayer(
                        this, player, () -> doPurchase(player, type)));
        return true;
    }

    /**
     * Dialog API 从 Paper 1.21.7 开始提供。
     * 使用类存在性检测，避免在旧版 Paper/Spigot 上提前加载 Dialog 类。
     */
    private static boolean isDialogApiAvailable() {
        try {
            Class.forName("io.papermc.paper.dialog.Dialog", false,
                    PayDayPlugin.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    /**
     * 通过反射隔离 Dialog 实现类，避免不支持 Dialog 的服务端验证并加载其类。
     */
    private static PurchaseMenu loadDialogMenu() throws ReflectiveOperationException {
        Class<?> type = Class.forName("dev.che1s.payday.PayDayDialogManager", true,
                PayDayPlugin.class.getClassLoader());
        return (PurchaseMenu) type.getDeclaredConstructor().newInstance();
    }

    // ==================== /paydayadmin reload ====================

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
            log.info(language.console("log.reload", "player", sender.getName()));
            return true;
        }
        sender.sendMessage(language.get(sender, "message.usage_admin"));
        return true;
    }
}
