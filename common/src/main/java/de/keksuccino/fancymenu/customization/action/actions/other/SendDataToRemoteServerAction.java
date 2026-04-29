package de.keksuccino.fancymenu.customization.action.actions.other;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.action.ActionInstance;
import de.keksuccino.fancymenu.customization.remote.RemoteServerConnectionManager;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.Dialogs;
import de.keksuccino.fancymenu.util.rendering.ui.screen.DualTextInputWindowBody;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SendDataToRemoteServerAction extends Action {

    public SendDataToRemoteServerAction() {
        super("send_data_to_remote_server");
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
        String remoteServerUrl = valueArray[0].trim();
        if (remoteServerUrl.isBlank()) {
            return;
        }
        String data = valueArray[1];

        RemoteServerConnectionManager.sendData(remoteServerUrl, data);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.actions.send_data_to_remote_server");
    }

    @Override
    public @NotNull Component getDescription() {
        return Component.translatable("fancymenu.actions.send_data_to_remote_server.desc");
    }

    @Override
    public @Nullable Component getValueDisplayName() {
        return Component.empty();
    }

    @Override
    public @Nullable String getValuePreset() {
        return "wss://example.com/ws||Hello remote server!";
    }

    @Override
    public void editValue(@NotNull ActionInstance instance, @NotNull Action.ActionEditingCompletedFeedback onEditingCompleted, @NotNull Action.ActionEditingCanceledFeedback onEditingCanceled) {
        String oldValue = instance.value;
        boolean[] handled = {false};

        DualTextInputWindowBody s = DualTextInputWindowBody.build(
                this.getDisplayName(),
                Component.translatable("fancymenu.actions.send_data_to_remote_server.value.remote_server_url"),
                Component.translatable("fancymenu.actions.send_data_to_remote_server.value.data"),
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
