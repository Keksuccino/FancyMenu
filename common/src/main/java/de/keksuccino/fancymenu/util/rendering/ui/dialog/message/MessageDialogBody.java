package de.keksuccino.fancymenu.util.rendering.ui.dialog.message;

import de.keksuccino.fancymenu.util.input.InputConstants;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.text.TextFormattingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
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

public class MessageDialogBody extends PiPWindowBody {

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
    private final MessageDialogStyle style;
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

    private long delayEnd = -1;
    private boolean forceOkOnly = false;

    public MessageDialogBody(@NotNull Component message, @NotNull MessageDialogStyle style, @Nullable Consumer<Boolean> callback) {
        super(Component.empty());
        this.message = message;
        this.style = style;
        this.callback = callback;
    }

    @Override
    protected void init() {
        rebuildButtons();
        this.updateRenderLines();
    }

    @Override
    public void onWindowClosedExternally() {
        if (this.callback != null) {
            this.callback.accept(this.forceOkOnly); // returns false by default, but since force-ok should always return true, we simply use the forceOkOnly flag here
        }
    }

    @Override
    public void renderBody(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

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
        int textColor = UIBase.shouldBlur() ? UIBase.getUITheme().ui_blur_interface_generic_text_color.getColorInt() : UIBase.getUITheme().ui_interface_generic_text_color.getColorInt();
        for (Component line : this.renderLines) {
            UIBase.renderText(graphics, line, textX, y, textColor);
            y += LINE_SPACING;
        }

        updateButtonPositions();

    }

    @Override
    public void renderBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
    }

    @Override
    public boolean keyPressed(int button, int scanCode, int modifiers) {
        if (button == InputConstants.KEY_ENTER) {
            if (canAccept()) {
                handleResult(true);
            }
            return true;
        }
        if (button == InputConstants.KEY_ESCAPE) {
            handleResult(false);
            return true;
        }
        return super.keyPressed(button, scanCode, modifiers);
    }

    private void handleResult(boolean accepted) {
        if (accepted && !canAccept()) {
            return;
        }
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

    private void rebuildButtons() {
        if (this.okayButton != null) {
            this.removeWidget(this.okayButton);
            this.okayButton = null;
        }
        if (this.cancelButton != null) {
            this.removeWidget(this.cancelButton);
            this.cancelButton = null;
        }
        if (this.acceptButton != null) {
            this.removeWidget(this.acceptButton);
            this.acceptButton = null;
        }

        if (isOkOnlyMode()) {
            this.okayButton = new ExtendedButton(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable("fancymenu.common_components.ok"), (button) -> {
                handleResult(true);
            });
            this.okayButton.setIsActiveSupplier(consumes -> canAccept());
            this.okayButton.setUITooltipSupplier(consumes -> buildDelayTooltip());
            this.okayButton.setFocusable(false);
            this.okayButton.setNavigatable(false);
            this.addRenderableWidget(this.okayButton);
            UIBase.applyDefaultWidgetSkinTo(this.okayButton, UIBase.shouldBlur());
        } else {
            this.cancelButton = new ExtendedButton(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable("fancymenu.common_components.cancel"), (button) -> {
                handleResult(false);
            });
            this.cancelButton.setFocusable(false);
            this.cancelButton.setNavigatable(false);
            this.addRenderableWidget(this.cancelButton);
            UIBase.applyDefaultWidgetSkinTo(this.cancelButton, UIBase.shouldBlur());

            this.acceptButton = new ExtendedButton(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable("fancymenu.common_components.accept"), (button) -> {
                handleResult(true);
            });
            this.acceptButton.setIsActiveSupplier(consumes -> canAccept());
            this.acceptButton.setUITooltipSupplier(consumes -> buildDelayTooltip());
            this.acceptButton.setFocusable(false);
            this.acceptButton.setNavigatable(false);
            this.addRenderableWidget(this.acceptButton);
            UIBase.applyDefaultWidgetSkinTo(this.acceptButton, UIBase.shouldBlur());
        }
    }

    private boolean isOkOnlyMode() {
        return this.callback == null || this.forceOkOnly;
    }

    public MessageDialogBody setForceOkOnly(boolean forceOkOnly) {
        if (this.forceOkOnly == forceOkOnly) {
            return this;
        }
        this.forceOkOnly = forceOkOnly;
        if (this.okayButton != null || this.cancelButton != null || this.acceptButton != null) {
            rebuildButtons();
        }
        return this;
    }

    public MessageDialogBody setDelay(long delayMs) {
        if (delayMs <= 0) {
            this.delayEnd = -1;
        } else {
            this.delayEnd = System.currentTimeMillis() + delayMs;
        }
        return this;
    }

    private boolean canAccept() {
        return this.delayEnd <= System.currentTimeMillis();
    }

    @Nullable
    private UITooltip buildDelayTooltip() {
        if (canAccept()) {
            return null;
        }
        int secs = (int)((this.delayEnd - System.currentTimeMillis()) / 1000);
        if (secs < 1) secs = 1;
        return UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.ui.confirmation_screen.delay.tooltip", "" + secs));
    }

}
