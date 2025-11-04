package de.keksuccino.fancymenu.util.rendering.ui.screen.queueable;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.util.input.InputConstants;
import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.NotNull;
import java.util.List;

public class QueueableNotificationScreen extends QueueableScreen {

    @NotNull
    protected Component text;
    protected List<FormattedCharSequence> renderText;

    public QueueableNotificationScreen(@NotNull Component text) {
        super(Component.empty());
        this.text = text;
    }

    @Override
    protected void init() {

        int centerX = this.width / 2;

        super.init();

        this.renderText = this.font.split(this.text, this.width - 60);

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
    public void renderBackground(@NotNull GuiGraphics graphics) {
        RenderSystem.enableBlend();
        graphics.fill(0, 0, this.width, this.height, UIBase.getUIColorTheme().screen_background_color.getColorInt());
    }

    @Override
    public boolean keyPressed(int key, int scancode, int modifiers) {

        if ((key == InputConstants.KEY_ENTER) || (key == InputConstants.KEY_NUMPADENTER)) {
            this.onClose();
            return true;
        }

        return super.keyPressed(key, scancode, modifiers);

    }

}
