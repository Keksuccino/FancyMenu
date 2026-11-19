package de.keksuccino.fancymenu.util.rendering.ui.dialog.message;

import de.keksuccino.fancymenu.customization.aiexport.AiExportMenus;
import de.keksuccino.fancymenu.customization.aiexport.exporters.DialogAiExporter;
import de.keksuccino.fancymenu.util.input.InputConstants;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcon;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollbar.ScrollBar;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fStack;
import org.joml.Vector2f;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class MessageDialogBody extends PiPWindowBody {

    private static final int ICON_SIZE = MessageDialogLayout.ICON_SIZE;
    private static final int PADDING = MessageDialogLayout.PADDING;
    private static final int BUTTON_HEIGHT = MessageDialogLayout.BUTTON_HEIGHT;
    private static final int BUTTON_WIDTH = MessageDialogLayout.BUTTON_WIDTH;
    private static final int BUTTON_GAP = MessageDialogLayout.BUTTON_GAP;
    private static final int LINE_SPACING = MessageDialogLayout.LINE_SPACING;
    private static final float SCISSOR_EPSILON = 0.001F;

    @NotNull
    private final Component message;
    @NotNull
    private final MessageDialogStyle style;
    @Nullable
    private final Consumer<Boolean> callback;

    @NotNull
    private List<MutableComponent> renderLines = new ArrayList<>();
    private int lastWrapWidth = -1;
    @NotNull
    private MessageDialogLayout layout = MessageDialogLayout.of(0, 0, 0, false);
    /** Scrolls a message that is too long for the dialog. It keeps its scroll when the dialog gets resized. */
    @NotNull
    private final ScrollBar scrollBar = new ScrollBar(ScrollBar.ScrollBarDirection.VERTICAL, UIBase.VERTICAL_SCROLL_BAR_WIDTH, UIBase.VERTICAL_SCROLL_BAR_HEIGHT, 0, 0, 0, 0, () -> UIBase.getUITheme().scroll_grabber_color_normal, () -> UIBase.getUITheme().scroll_grabber_color_hover);
    /** The smallest body height this dialog last gave its window, or -1 before it gave one. */
    private int windowMinBodyHeight = -1;
    /** Reused for the corners of the scissor of the message area, which gets enabled every frame while the message scrolls. */
    private final Vector2f scissorCorner = new Vector2f();

    @Nullable
    private ExtendedButton cancelButton;
    @Nullable
    private ExtendedButton acceptButton;
    @Nullable
    private ExtendedButton okayButton;
    @Nullable
    private ExtendedButton alternativeButton;
    @Nullable
    private ExtendedButton aiExportButton;
    @Nullable
    private Component acceptLabel;
    @Nullable
    private Component alternativeLabel;
    @Nullable
    private Runnable alternativeAction;

    private long delayEnd = -1;
    private boolean forceOkOnly = false;

    public MessageDialogBody(@NotNull Component message, @NotNull MessageDialogStyle style, @Nullable Consumer<Boolean> callback) {
        super(Component.empty());
        this.message = message;
        this.style = style;
        this.callback = callback;
        this.scrollBar.setRoundedGrabberEnabled(true);
        this.scrollBar.setGrabberEdgeInset(0.0F);
    }

    @Override
    protected void init() {
        rebuildButtons();
        this.addWidget(this.scrollBar);
        this.updateRenderLines();
    }

    @Override
    public void tick() {
        super.tick();
        // A new body width wraps the message into a new number of lines. The window gets its new minimum here instead of in init(),
        // because init() runs while the window is still busy resizing the body.
        this.fitWindowToMessage();
    }

    /**
     * Makes the window of this dialog at least as tall as its message needs to fit above the buttons without scrolling.
     * A very long message scrolls instead of making the dialog cover the whole screen, see {@link MessageDialogLayout#getMinBodyHeight(int, boolean, float)}.<br>
     * Windows open centered with the size they have at that moment, so this has to get called once after the window got this dialog
     * and before it opens. After that, the dialog keeps the window in sync by itself.
     */
    public void fitWindowToMessage() {
        PiPWindow window = this.getWindow();
        if (window == null) return;
        int minBodyHeight = MessageDialogLayout.getMinBodyHeight(this.renderLines.size(), this.style.getIcon() != null, getScreenHeight());
        if (minBodyHeight == this.windowMinBodyHeight) return;
        this.windowMinBodyHeight = minBodyHeight;
        window.setMinBodySize(0, minBodyHeight);
    }

    private static float getScreenHeight() {
        // Dialogs draw their body at the FancyMenu UI scale, which makes this the screen height in the units of the body
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        float renderScale = UIBase.getFixedUIRenderScale();
        if (!(renderScale > 0.0F)) return screenHeight;
        return screenHeight / renderScale;
    }

    @Override
    public void onWindowClosedExternally() {
        if (this.callback != null) {
            this.callback.accept(this.forceOkOnly); // returns false by default, but since force-ok should always return true, we simply use the forceOkOnly flag here
        }
    }

    @Override
    public void renderBody(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {

        this.updateRenderLinesIfNeeded();
        MessageDialogLayout layout = this.layout;

        MaterialIcon icon = this.style.getIcon();
        if (icon != null) {
            DrawableColor tint = resolveIconTint();
            icon.render(graphics, PADDING, layout.iconY(), ICON_SIZE, ICON_SIZE, (tint != null) ? tint.getColorInt() : -1);
        }

        boolean scrollable = layout.isScrollable();
        int scrollOffset = layout.getScrollOffset(this.scrollBar.getScroll());
        if (scrollable) {
            this.enableMessageAreaScissor(graphics, layout);
        }
        int textColor = UIBase.shouldBlur() ? UIBase.getUITheme().ui_blur_interface_generic_text_color.getColorInt() : UIBase.getUITheme().ui_interface_generic_text_color.getColorInt();
        int lineEnd = layout.getVisibleLineEnd(scrollOffset, this.renderLines.size());
        for (int i = layout.getFirstVisibleLine(scrollOffset); i < lineEnd; i++) {
            UIBase.renderText(graphics, this.renderLines.get(i), layout.textX(), layout.textY() - scrollOffset + (i * LINE_SPACING), textColor);
        }
        if (scrollable) {
            graphics.disableScissor();
            this.scrollBar.extractRenderState(graphics, mouseX, mouseY, partial);
        }

        updateButtonPositions();

    }

    /**
     * Cuts everything drawn after this to the message area of the given layout, covering at least the whole area.<br>
     * The GUI cuts at whole GUI pixels, and {@link GuiGraphicsExtractor#enableScissor(int, int, int, int)} rounds the corners of a scissor under
     * the scaled pose of the body down. That moves the bottom edge up by up to almost two GUI pixels, which are several units of the body at
     * small UI scales, enough to cut off the descenders of the last line when the message is scrolled to its end. So the corners get rounded
     * outward here, and the scissor gets enabled without the pose. The small epsilon keeps float errors of edges that sit right on a GUI pixel
     * from adding a pixel.
     */
    private void enableMessageAreaScissor(@NotNull GuiGraphicsExtractor graphics, @NotNull MessageDialogLayout layout) {
        Matrix3x2fStack pose = graphics.pose();
        Vector2f corner = this.scissorCorner;
        pose.transformPosition(0.0F, layout.messageAreaTop(), corner);
        int left = Mth.floor(corner.x + SCISSOR_EPSILON);
        int top = Mth.floor(corner.y + SCISSOR_EPSILON);
        pose.transformPosition(this.width, layout.messageAreaTop() + layout.messageAreaHeight(), corner);
        int right = Mth.ceil(corner.x - SCISSOR_EPSILON);
        int bottom = Mth.ceil(corner.y - SCISSOR_EPSILON);
        pose.pushMatrix();
        pose.identity();
        graphics.enableScissor(left, top, right, bottom);
        pose.popMatrix();
    }

    @Override
    public void extractBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        return this.keyPressed(event.key(), event.keycode(), event.modifiers());
    }
    
    public boolean keyPressed(int button, int scanCode, int modifiers) {
        if (button == InputConstants.KEY_RETURN) {
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

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
        if (super.mouseScrolled(mouseX, mouseY, scrollDeltaX, scrollDeltaY)) return true;
        // The wheel scrolls a message that is too long for the dialog from anywhere in the dialog, not only over the message
        if ((scrollDeltaY != 0.0D) && this.layout.isScrollable() && !this.scrollBar.isGrabberGrabbed()) {
            this.scrollBar.setScroll(this.layout.getScrollAfterWheel(this.scrollBar.getScroll(), scrollDeltaY));
            return true;
        }
        return false;
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
        // The body gets drawn at the UI scale, where the UI font is one size bigger on small UI scales, so the lines have to get measured like that
        UIBase.startUIScaleRendering();
        try {
            List<MutableComponent> linesCopy = UIBase.lineWrapUIComponentsNormal(this.message, 100000.0F);
            this.renderLines = UIBase.lineWrapUIComponentsNormal(linesCopy, (float)wrapWidth);
        } finally {
            UIBase.stopUIScaleRendering();
        }
        this.lastWrapWidth = wrapWidth;
        this.updateLayout();
    }

    private int getWrapWidth() {
        return MessageDialogLayout.getWrapWidth(this.width, this.style.getIcon() != null);
    }

    private void updateLayout() {
        this.layout = MessageDialogLayout.of(this.width, this.height, this.renderLines.size(), this.style.getIcon() != null);
        boolean scrollable = this.layout.isScrollable();
        this.scrollBar.active = scrollable;
        if (!scrollable) this.scrollBar.setScroll(0.0F);
        // The grabber moves along the right side of the message area, in the middle of the padding next to the message.
        // It is at most half as tall as the area, so it still has room to move in very small dialogs.
        int grabberWidth = (int) this.scrollBar.grabberWidth;
        this.scrollBar.grabberHeight = Math.max(1, Math.min(UIBase.VERTICAL_SCROLL_BAR_HEIGHT, this.layout.messageAreaHeight() / 2));
        this.scrollBar.scrollAreaStartX = this.layout.textX();
        this.scrollBar.scrollAreaStartY = this.layout.messageAreaTop();
        this.scrollBar.scrollAreaEndX = this.layout.scrollBarCenterX() + (grabberWidth - (grabberWidth / 2));
        this.scrollBar.scrollAreaEndY = this.layout.messageAreaTop() + this.layout.messageAreaHeight();
    }

    private void updateButtonPositions() {
        int y = this.layout.buttonY();
        if (this.okayButton != null) {
            int buttonCount = (this.aiExportButton != null) ? 2 : 1;
            int buttonWidth = MessageDialogLayout.getButtonWidth(this.width, buttonCount);
            int x = MessageDialogLayout.getButtonRowX(this.width, buttonCount, buttonWidth);
            if (this.aiExportButton != null) {
                placeButton(this.aiExportButton, x, y, buttonWidth);
                x += buttonWidth + BUTTON_GAP;
            }
            placeButton(this.okayButton, x, y, buttonWidth);
        } else if (this.cancelButton != null && this.acceptButton != null) {
            int buttonCount = (this.alternativeButton != null) ? 3 : 2;
            int buttonWidth = MessageDialogLayout.getButtonWidth(this.width, buttonCount);
            int x = MessageDialogLayout.getButtonRowX(this.width, buttonCount, buttonWidth);
            placeButton(this.cancelButton, x, y, buttonWidth);
            x += buttonWidth + BUTTON_GAP;
            // The other way to go on sits between canceling and accepting
            if (this.alternativeButton != null) {
                placeButton(this.alternativeButton, x, y, buttonWidth);
                x += buttonWidth + BUTTON_GAP;
            }
            placeButton(this.acceptButton, x, y, buttonWidth);
        }
    }

    private static void placeButton(@NotNull ExtendedButton button, int x, int y, int width) {
        button.setX(x);
        button.setY(y);
        button.setWidth(width);
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
        if (this.alternativeButton != null) {
            this.removeWidget(this.alternativeButton);
            this.alternativeButton = null;
        }
        if (this.aiExportButton != null) {
            this.removeWidget(this.aiExportButton);
            this.aiExportButton = null;
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
            // Errors and warnings that only inform the user are what users ask for help with, so they can export them for AI chats
            if ((this.style == MessageDialogStyle.ERROR) || (this.style == MessageDialogStyle.WARNING)) {
                this.aiExportButton = new ExtendedButton(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable("fancymenu.ai_export"), (button) -> {
                    AiExportMenus.openMenuAtMouse(AiExportMenus.buildExportMenu(() -> DialogAiExporter.export(this.message, this.style)));
                });
                this.aiExportButton.setUITooltipSupplier(consumes -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.ai_export.desc")));
                this.aiExportButton.setFocusable(false);
                this.aiExportButton.setNavigatable(false);
                this.addRenderableWidget(this.aiExportButton);
                UIBase.applyDefaultWidgetSkinTo(this.aiExportButton, UIBase.shouldBlur());
            }
        } else {
            this.cancelButton = new ExtendedButton(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable("fancymenu.common_components.cancel"), (button) -> {
                handleResult(false);
            });
            this.cancelButton.setFocusable(false);
            this.cancelButton.setNavigatable(false);
            this.addRenderableWidget(this.cancelButton);
            UIBase.applyDefaultWidgetSkinTo(this.cancelButton, UIBase.shouldBlur());

            if ((this.alternativeLabel != null) && (this.alternativeAction != null)) {
                this.alternativeButton = new ExtendedButton(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT, this.alternativeLabel, (button) -> {
                    if (!canAccept()) return;
                    this.alternativeAction.run();
                    this.closeWindow();
                });
                this.alternativeButton.setIsActiveSupplier(consumes -> canAccept());
                this.alternativeButton.setUITooltipSupplier(consumes -> buildDelayTooltip());
                this.alternativeButton.setFocusable(false);
                this.alternativeButton.setNavigatable(false);
                this.addRenderableWidget(this.alternativeButton);
                UIBase.applyDefaultWidgetSkinTo(this.alternativeButton, UIBase.shouldBlur());
            }

            this.acceptButton = new ExtendedButton(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT, (this.acceptLabel != null) ? this.acceptLabel : Component.translatable("fancymenu.common_components.accept"), (button) -> {
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

    @Nullable
    private DrawableColor resolveIconTint() {
        return switch (this.style) {
            case INFO -> UIBase.getUITheme().info_color;
            case WARNING -> UIBase.getUITheme().warning_color;
            case ERROR -> UIBase.getUITheme().error_color;
            default -> null;
        };
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

    /**
     * Sets what the button that accepts the dialog says, for dialogs where "Accept" would not tell what happens.
     */
    public MessageDialogBody setAcceptLabel(@NotNull Component acceptLabel) {
        this.acceptLabel = acceptLabel;
        this.rebuildButtonsIfBuilt();
        return this;
    }

    /**
     * Gives the dialog a second way to go on next to accepting it, with a button of its own between the ones that cancel and accept.
     * Dialogs that only have an OK button do not show it.
     *
     * @param action Runs when the user picks the button, right before the dialog closes. The callback of the dialog does not get called then.
     */
    public MessageDialogBody setAlternative(@NotNull Component label, @NotNull Runnable action) {
        this.alternativeLabel = label;
        this.alternativeAction = action;
        this.rebuildButtonsIfBuilt();
        return this;
    }

    private void rebuildButtonsIfBuilt() {
        if (this.okayButton != null || this.cancelButton != null || this.acceptButton != null) {
            rebuildButtons();
        }
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
