package dev.che1s.payday;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

/**
 * 使用 Paper 原生 Dialog API 实现的天气/时间购买菜单。
 * 该类只应在服务端存在 io.papermc.paper.dialog.Dialog 时加载。
 */
final class PayDayDialogManager implements PurchaseMenu {

    @Override
    public void open(Player player, String title, String description,
                     Function<PurchaseType, String> buttonLabel,
                     Function<PurchaseType, String> buttonDetail,
                     String closeLabel, String closeTooltip,
                     Consumer<PurchaseType> onSelect) {
        List<ActionButton> actions = new ArrayList<ActionButton>();
        for (PurchaseType type : PurchaseType.values()) {
            actions.add(ActionButton.create(
                    Component.text(buttonLabel.apply(type), NamedTextColor.GOLD),
                    Component.text(buttonDetail.apply(type), NamedTextColor.GRAY),
                    100,
                    DialogAction.customClick(
                            (response, audience) -> onSelect.accept(type),
                            ClickCallback.Options.builder()
                                    .uses(1)
                                    .lifetime(Duration.ofMinutes(10L))
                                    .build())));
        }

        ActionButton close = ActionButton.create(
                Component.text(closeLabel, NamedTextColor.RED),
                Component.text(closeTooltip, NamedTextColor.GRAY),
                100,
                DialogAction.customClick(
                        (response, audience) -> {
                        },
                        ClickCallback.Options.builder()
                                .uses(1)
                                .lifetime(Duration.ofMinutes(10L))
                                .build()));

        Dialog dialog = Dialog.create(factory -> factory.empty()
                .base(DialogBase.builder(Component.text(title, NamedTextColor.GOLD))
                        .canCloseWithEscape(true)
                        .pause(false)
                        .body(Collections.singletonList(DialogBody.plainMessage(
                                Component.text(description, NamedTextColor.WHITE), 300)))
                        .build())
                .type(DialogType.multiAction(actions)
                        .exitAction(close)
                        .columns(2)
                        .build()));

        player.showDialog(dialog);
    }
}
