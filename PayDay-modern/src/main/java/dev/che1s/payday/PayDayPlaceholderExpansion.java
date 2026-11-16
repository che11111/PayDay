package dev.che1s.payday;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

/**
 * PlaceholderAPI 占位符：
 *   %payday_payday%     —— 玩家累计执行 /payday（白天）的次数
 *   %payday_payweather% —— 玩家累计执行 /payweather（晴天）的次数
 *   %payday_payrain%    —— 玩家累计执行 /payrain（雨天）的次数
 *   %payday_paystorm%   —— 玩家累计执行 /paystorm（雷暴天气）的次数
 *   %payday_paynight%   —— 玩家累计执行 /paynight（夜晚）的次数
 *   %payday_paydawn%    —— 玩家累计执行 /paydawn（黎明）的次数
 *   %payday_paydusk%    —— 玩家累计执行 /paydusk（落日）的次数
 */
public class PayDayPlaceholderExpansion extends PlaceholderExpansion {

    private final PayDayPlugin plugin;

    public PayDayPlaceholderExpansion(PayDayPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "payday";
    }

    @Override
    public String getAuthor() {
        return "che1s";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null || player.getName() == null) {
            return "";
        }

        for (PurchaseType type : PurchaseType.values()) {
            String commandName = type.getCommand().substring(1);
            if (params.equalsIgnoreCase(commandName)) {
                return String.valueOf(plugin.getCount(player.getName(), type.getCommand()));
            }
        }
        return null;
    }
}
