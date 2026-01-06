package de.keksuccino.fancymenu.util.rendering.ui.dialog;

import de.keksuccino.fancymenu.util.input.InputConstants;
import de.keksuccino.fancymenu.util.rendering.text.TextFormattingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPScreen;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class DialogBody extends PiPScreen {

    private static final int ICON_SIZE = 32;
    private static final int ICON_GAP = 12;
    private static final int PADDING = 16;
    private static final int BUTTON_WIDTH = 120;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 8;
    private static final int BUTTON_BOTTOM_PADDING = 20;
    private static final int LINE_SPACING = 14;

    @NotNull
    private final Component message;
    @NotNull
    private final DialogStyle style;
    @Nullable
    private final Consumer<Boolean> callback;

    @NotNull
    private List<MutableComponent> renderLines = new ArrayList<>();
    private int lastWrapWidth = -1;

    @Nullable
    private ExtendedButton cancelButton;
    @Nullable
    private ExtendedButton acceptButton;
    @Nullable
    private ExtendedButton okayButton;

    public DialogBody(@NotNull Component message, @NotNull DialogStyle style, @Nullable Consumer<Boolean> callback) {
        super(Component.empty());
        this.message = message;
        this.style = style;
        this.callback = callback;
    }

    @Override
    protected void init() {
        if (this.callback != null) {
            this.cancelButton = new ExtendedButton(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable("fancymenu.common_components.cancel"), (button) -> {
                handleResult(false);
            });
            this.addRenderableWidget(this.cancelButton);
            UIBase.applyDefaultWidgetSkinTo(this.cancelButton);

            this.acceptButton = new ExtendedButton(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable("fancymenu.common_components.accept"), (button) -> {
                handleResult(true);
            });
            this.addRenderableWidget(this.acceptButton);
            UIBase.applyDefaultWidgetSkinTo(this.acceptButton);
        } else {
            this.okayButton = new ExtendedButton(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable("fancymenu.common_components.ok"), (button) -> {
                handleResult(true);
            });
            this.addRenderableWidget(this.okayButton);
            UIBase.applyDefaultWidgetSkinTo(this.okayButton);
        }

        this.updateRenderLines();
    }

    @Override
    public void onWindowClosedExternally() {
        if (this.callback != null) {
            this.callback.accept(false);
        }
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        PiPWindow window = this.getWindow();
        if (window != null) {
            window.setCustomBodyScale((double)UIBase.calculateFixedScale(UIBase.getUIScale()));
        }

        graphics.fill(0, 0, this.width, this.height, UIBase.getUIColorTheme().screen_background_color.getColorInt());

        this.updateRenderLinesIfNeeded();

        int buttonAreaHeight = BUTTON_HEIGHT + BUTTON_BOTTOM_PADDING;
        int contentTop = PADDING;
        int contentBottom = Math.max(contentTop, this.height - buttonAreaHeight);
        int contentHeight = Math.max(0, contentBottom - contentTop);

        int textHeight = this.renderLines.size() * LINE_SPACING;
        int iconHeight = (this.style.getIcon() != null) ? ICON_SIZE : 0;
        int contentBlockHeight = Math.max(textHeight, iconHeight);
        int contentY = contentTop + Math.max(0, (contentHeight - contentBlockHeight) / 2);

        int textX = PADDING;
        if (this.style.getIcon() != null) {
            int iconX = PADDING;
            int iconY = contentY + (contentBlockHeight - ICON_SIZE) / 2;
            renderIcon(graphics, this.style.getIcon(), iconX, iconY);
            textX = iconX + ICON_SIZE + ICON_GAP;
        }

        int textY = contentY + Math.max(0, (contentBlockHeight - textHeight) / 2);
        int y = textY;
        int textColor = UIBase.getUIColorTheme().generic_text_base_color.getColorInt();
        for (Component line : this.renderLines) {
            graphics.drawString(this.font, line, textX, y, textColor, false);
            y += LINE_SPACING;
        }

        updateButtonPositions();
        super.render(graphics, mouseX, mouseY, partial);
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
    }

    @Override
    public boolean keyPressed(int button, int scanCode, int modifiers) {
        if (button == InputConstants.KEY_ENTER) {
            handleResult(true);
            return true;
        }
        return super.keyPressed(button, scanCode, modifiers);
    }

    private void handleResult(boolean accepted) {
        if (this.callback != null) {
            this.callback.accept(accepted);
        }
        this.closeWindow();
    }

    private void updateRenderLinesIfNeeded() {
        int wrapWidth = getWrapWidth();
        if (wrapWidth == this.lastWrapWidth) {
            return;
        }
        updateRenderLines();
    }

    private void updateRenderLines() {
        int wrapWidth = getWrapWidth();
        List<MutableComponent> linesCopy = TextFormattingUtils.lineWrapComponents(this.message, 100000);
        this.renderLines = TextFormattingUtils.lineWrapComponents(linesCopy, wrapWidth);
        this.lastWrapWidth = wrapWidth;
    }

    private int getWrapWidth() {
        int iconSpace = (this.style.getIcon() != null) ? (ICON_SIZE + ICON_GAP) : 0;
        return Math.max(20, this.width - (PADDING * 2) - iconSpace);
    }

    private void updateButtonPositions() {
        int y = this.height - BUTTON_HEIGHT - BUTTON_BOTTOM_PADDING;
        if (this.okayButton != null) {
            this.okayButton.setX((this.width - BUTTON_WIDTH) / 2);
            this.okayButton.setY(y);
        } else if (this.cancelButton != null && this.acceptButton != null) {
            int totalWidth = (BUTTON_WIDTH * 2) + BUTTON_GAP;
            int startX = (this.width - totalWidth) / 2;
            this.cancelButton.setX(startX);
            this.cancelButton.setY(y);
            this.acceptButton.setX(startX + BUTTON_WIDTH + BUTTON_GAP);
            this.acceptButton.setY(y);
        }
    }

    private void renderIcon(@NotNull GuiGraphics graphics, @NotNull ResourceLocation icon, int x, int y) {
        graphics.blit(icon, x, y, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
    }

}
