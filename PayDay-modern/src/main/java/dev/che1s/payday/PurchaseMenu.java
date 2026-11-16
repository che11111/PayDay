package dev.che1s.payday;

import java.util.function.Consumer;
import java.util.function.Function;

import org.bukkit.entity.Player;

/**
 * 与具体菜单实现解耦的购买菜单接口。
 * PayDay 主类仅依赖此接口，避免旧服务端加载 Paper Dialog API。
 */
interface PurchaseMenu {

    void open(Player player, String title, String description,
              Function<PurchaseType, String> buttonLabel,
              Function<PurchaseType, String> buttonDetail,
              String closeLabel, String closeTooltip,
              Consumer<PurchaseType> onSelect);
}
