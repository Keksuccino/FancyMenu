package de.keksuccino.fancymenu.customization.action.ui;

import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.screen.queueable.QueueableNotificationScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;

public class AsyncActionErrorScreen extends QueueableNotificationScreen {

    public AsyncActionErrorScreen(@NotNull Component actionName) {
        super(Component.empty());
        Component actionNameFormatted = actionName.copy().withStyle(Style.EMPTY.withBold(true).withColor(UIBase.getUITheme().error_text_color.getColorInt()));
        this.text = Component.translatable("fancymenu.actions.async.cant_run_async", actionNameFormatted);
    }

}
