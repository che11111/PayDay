package dev.che1s.payday;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.floodgate.api.FloodgateApi;

/**
 * 基于 Floodgate + Cumulus 的基岩版表单。
 * 该类的任何实例只有在服务器已安装 Floodgate 时才应创建，
 * 以避免类加载失败。
 */
public class BedrockFormManager {

    private final FloodgateApi floodgateApi;

    public BedrockFormManager() {
        this.floodgateApi = FloodgateApi.getInstance();
    }

    public boolean isBedrockPlayer(UUID uuid) {
        return floodgateApi.isFloodgatePlayer(uuid);
    }

    /**
     * 打开天气/时间购买表单。
     *
     * @param player      目标基岩版玩家
     * @param title       表单标题（纯文本，不包含颜色代码）
     * @param buttonLabel 按钮文本生成器（纯文本，不包含颜色代码）
     * @param onSelect    点击按钮后的回调（回调可能在非主线程触发，调用方需自行调度）
     */
    public void openWeatherForm(Player player, String title, Function<PurchaseType, String> buttonLabel, Consumer<PurchaseType> onSelect) {
        PurchaseType[] types = PurchaseType.values();

        SimpleForm.Builder builder = SimpleForm.builder().title(title);
        for (PurchaseType type : types) {
            builder.button(buttonLabel.apply(type));
        }
        builder.validResultHandler(response -> {
            int id = response.clickedButtonId();
            if (id < 0 || id >= types.length) {
                return;
            }
            onSelect.accept(types[id]);
        });

        floodgateApi.sendForm(player.getUniqueId(), builder);
    }
}
