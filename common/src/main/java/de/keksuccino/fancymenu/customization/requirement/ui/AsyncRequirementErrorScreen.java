package de.keksuccino.fancymenu.customization.requirement.ui;

import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.screen.queueable.QueueableNotificationScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;

public class AsyncRequirementErrorScreen extends QueueableNotificationScreen {

    public AsyncRequirementErrorScreen(@NotNull Component requirementName) {
        super(Component.empty());
        Component requirementNameFormatted = requirementName.copy().withStyle(Style.EMPTY.withBold(true).withColor(UIBase.getUITheme().error_text_color.getColorInt()));
        this.text = Component.translatable("fancymenu.requirements.async.cant_run_async", requirementNameFormatted);
    }

}
