package de.keksuccino.fancymenu.customization.action.actions.other;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.action.ActionInstance;
import de.keksuccino.fancymenu.networking.PacketHandler;
import de.keksuccino.fancymenu.networking.packets.fmdata.FmDataToServerPacket;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.Dialogs;
import de.keksuccino.fancymenu.util.rendering.ui.screen.DualTextInputWindowBody;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SendFmDataToServerAction extends Action {

    public SendFmDataToServerAction() {
        super("send_fm_data_to_server");
    }

    @Override
    public boolean canRunAsync() {
        return false;
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public void execute(@Nullable String value) {
        if (value == null || !value.contains("||")) {
            return;
        }

        String[] valueArray = value.split("\\|\\|", 2);
        String dataIdentifier = valueArray[0];
        String data = valueArray[1];

        FmDataToServerPacket packet = new FmDataToServerPacket();
        packet.data_identifier = dataIdentifier;
        packet.data = data;
        PacketHandler.sendToServer(packet);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.actions.send_fm_data_to_server");
    }

    @Override
    public @NotNull Component getDescription() {
        return Component.translatable("fancymenu.actions.send_fm_data_to_server.desc");
    }

    @Override
    public @Nullable Component getValueDisplayName() {
        return Component.empty();
    }

    @Override
    public @Nullable String getValuePreset() {
        return "example_identifier||This is some data";
    }

    @Override
    public void editValue(@NotNull ActionInstance instance, @NotNull Action.ActionEditingCompletedFeedback onEditingCompleted, @NotNull Action.ActionEditingCanceledFeedback onEditingCanceled) {
        String oldValue = instance.value;
        boolean[] handled = {false};

        DualTextInputWindowBody s = DualTextInputWindowBody.build(
                this.getDisplayName(),
                Component.translatable("fancymenu.actions.send_fm_data_to_server.value.identifier"),
                Component.translatable("fancymenu.actions.send_fm_data_to_server.value.data"),
                null,
                callback -> {
                    if (handled[0]) {
                        return;
                    }
                    handled[0] = true;
                    if (callback != null) {
                        String newValue = callback.getFirst() + "||" + callback.getSecond();
                        instance.value = newValue;
                        onEditingCompleted.accept(instance, oldValue, newValue);
                    } else {
                        onEditingCanceled.accept(instance);
                    }
                }
        );

        String val = instance.value;
        if ((val != null) && val.contains("||")) {
            String[] array = val.split("\\|\\|", 2);
            s.setFirstText(array[0]);
            s.setSecondText(array[1]);
        }

        var opened = Dialogs.openGeneric(s, this.getDisplayName(), null, DualTextInputWindowBody.PIP_WINDOW_WIDTH, DualTextInputWindowBody.PIP_WINDOW_HEIGHT);
        opened.getSecond().addCloseCallback(() -> {
            if (handled[0]) {
                return;
            }
            handled[0] = true;
            onEditingCanceled.accept(instance);
        });
    }

}
