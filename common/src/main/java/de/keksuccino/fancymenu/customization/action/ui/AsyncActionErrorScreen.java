package de.keksuccino.fancymenu.customization.action.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.screen.queueable.QueueableScreen;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.NotNull;
import java.util.List;

public class AsyncActionErrorScreen extends QueueableScreen {

    @NotNull
    protected Component actionName;
    protected List<FormattedCharSequence> renderText;

    protected AsyncActionErrorScreen(@NotNull Component actionName) {
        super(Component.empty());
        this.actionName = actionName.copy().withStyle(Style.EMPTY.withBold(true).withColor(UIBase.getUIColorTheme().error_text_color.getColorInt()));
    }

    @Override
    protected void init() {

        int centerX = this.width / 2;

        super.init();

        this.renderText = this.font.split(Component.translatable("fancymenu.actions.async.cant_run_async", this.actionName), this.width - 60);

        UIBase.applyDefaultWidgetSkinTo(this.addRenderableWidget(new ExtendedButton(centerX - 100, this.height - 50, 200, 20, Component.translatable("fancymenu.common_components.ok"), button -> {
            this.onClose();
        })));

    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        super.render(graphics, mouseX, mouseY, partial);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        if (this.renderText == null) return;

        // Calculate the total height of all text lines
        int lineHeight = this.font.lineHeight + 2; // Add 2 pixels of spacing between lines
        int totalTextHeight = this.renderText.size() * lineHeight;
        
        // Start rendering from the top of the centered text block
        int renderY = centerY - (totalTextHeight / 2);
        
        for (FormattedCharSequence s : this.renderText) {
            graphics.drawCenteredString(this.font, s, centerX, renderY, UIBase.getUIColorTheme().generic_text_base_color.getColorInt());
            renderY += lineHeight;
        }

    }

    @Override
    public void renderBackground(@NotNull GuiGraphics graphics, int $$1, int $$2, float $$3) {
        RenderSystem.enableBlend();
        graphics.fill(0, 0, this.width, this.height, UIBase.getUIColorTheme().screen_background_color.getColorInt());
    }

}
