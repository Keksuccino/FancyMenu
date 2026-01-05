package de.keksuccino.fancymenu.util.rendering.ui.pipwindow;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.cursor.CursorHandler;
import de.keksuccino.fancymenu.util.rendering.ui.menubar.v2.MenuBar;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import de.keksuccino.fancymenu.util.rendering.ui.theme.UIColorTheme;
import net.minecraft.util.FormattedCharSequence;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("unused")
public class PiPWindow extends AbstractContainerEventHandler implements Renderable {

    public static final int DEFAULT_TITLE_BAR_HEIGHT = 18;
    public static final int DEFAULT_BORDER_THICKNESS = 1;
    public static final int DEFAULT_BUTTON_SIZE = 12;
    public static final int DEFAULT_BUTTON_PADDING = 2;
    public static final int DEFAULT_MIN_WIDTH = 120;
    public static final int DEFAULT_MIN_HEIGHT = 80;
    public static final int DEFAULT_RESIZE_MARGIN = 4;
    private static final long TITLE_BAR_DOUBLE_CLICK_TIME_MS = 500;
    private static final int DEFAULT_ICON_TEXTURE_SIZE = 8;
    private static final ResourceLocation DEFAULT_CLOSE_BUTTON_ICON = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/pip/pip_window_close.png");
    private static final ResourceLocation DEFAULT_MAXIMIZE_BUTTON_ICON = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/pip/pip_window_maximize.png");
    private static final ResourceLocation DEFAULT_NORMALIZE_BUTTON_ICON = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/pip/pip_window_restore.png");

    private final Minecraft minecraft = Minecraft.getInstance();
    private final List<GuiEventListener> children = new ArrayList<>();
    private final List<PiPWindow> childWindows = new ArrayList<>();

    private Component title;
    @Nullable
    private ResourceLocation icon;
    @Nullable
    private Screen screen;

    private int x;
    private int y;
    private int width;
    private int height;
    private int minWidth = DEFAULT_MIN_WIDTH;
    private int minHeight = DEFAULT_MIN_HEIGHT;
    private int titleBarHeight = DEFAULT_TITLE_BAR_HEIGHT;
    private int borderThickness = DEFAULT_BORDER_THICKNESS;
    private int resizeMargin = DEFAULT_RESIZE_MARGIN;
    private int buttonSize = DEFAULT_BUTTON_SIZE;
    private int buttonPadding = DEFAULT_BUTTON_PADDING;
    @Nullable
    private ResourceLocation closeButtonIcon = DEFAULT_CLOSE_BUTTON_ICON;
    @Nullable
    private ResourceLocation maximizeButtonIcon = DEFAULT_MAXIMIZE_BUTTON_ICON;
    @Nullable
    private ResourceLocation normalizeButtonIcon = DEFAULT_NORMALIZE_BUTTON_ICON;

    private boolean visible = true;
    private boolean resizable = true;
    private boolean movable = true;
    private boolean maximizable = true;
    private boolean closable = true;
    private boolean closeScreenWithWindow = true;
    private boolean sizeScaledToGuiScale = true;

    private boolean maximized = false;
    private int restoreX;
    private int restoreY;
    private int restoreRawWidth;
    private int restoreRawHeight;

    private boolean inputLocked = false;
    private boolean inputLockedByChildren = false;

    @Nullable
    private PiPWindow parentWindow;

    private boolean draggingTitleBar = false;
    private PiPWindowResizeHandle activeResizeHandle = PiPWindowResizeHandle.NONE;
    private double dragOffsetX;
    private double dragOffsetY;
    private double resizeStartMouseX;
    private double resizeStartMouseY;
    private int resizeStartX;
    private int resizeStartY;
    private int resizeStartWidth;
    private int resizeStartHeight;
    private long lastTitleBarClickTime = 0;

    @Nullable
    private Runnable closeCallback;
    private boolean closingFromScreen = false;

    private int lastScreenWidth = -1;
    private int lastScreenHeight = -1;
    private boolean screenRendering = false;

    public PiPWindow(@Nonnull Component title, int x, int y, int width, int height) {
        this(title, x, y, width, height, null);
    }

    public PiPWindow(@Nonnull Component title, int width, int height) {
        this(title, 0, 0, width, height, null);
    }

    public PiPWindow(@Nonnull Component title) {
        this(title, 0, 0, 200, 200, null);
    }

    public PiPWindow(@Nullable Screen screen) {
        this(Component.literal("Window"), 0, 0, 200, 200, screen);
    }

    public PiPWindow() {
        this(Component.literal("Window"));
    }

    public PiPWindow(@Nonnull Component title, int x, int y, int width, int height, @Nullable Screen screen) {
        this.title = Objects.requireNonNull(title, "title");
        this.x = x;
        this.y = y;
        this.width = Math.max(width, getRawMinimumWidth());
        this.height = Math.max(height, getRawMinimumHeight());
        clampWindowToScreenSize();
        setScreen(screen);
    }

    @Override
    public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (!this.visible) {
            return;
        }

        RenderSystem.enableBlend();

        RenderSystem.disableDepthTest();
        RenderingUtils.setDepthTestLocked(true);

        renderWindowBackground(graphics);
        renderBodyScreen(graphics, mouseX, mouseY, partial);
        renderWindowForeground(graphics, mouseX, mouseY);
        updateResizeCursor(mouseX, mouseY);

        RenderingUtils.setDepthTestLocked(false);
        RenderSystem.enableDepthTest();

    }

    private void renderWindowBackground(@Nonnull GuiGraphics graphics) {
        float scale = getFrameRenderScale();
        int frameX = getFrameX();
        int frameY = getFrameY();
        int frameWidth = getFrameWidth();
        int frameHeight = getFrameHeight();
        int right = frameX + frameWidth;
        int bottom = frameY + frameHeight;
        UIColorTheme theme = getTheme();
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, scale);
        graphics.fill(frameX, frameY, right, bottom, theme.pip_window_border_color.getColorInt());

        int innerLeft = frameX + this.borderThickness;
        int innerTop = frameY + this.borderThickness;
        int innerRight = right - this.borderThickness;
        int innerBottom = bottom - this.borderThickness;
        if (innerRight <= innerLeft || innerBottom <= innerTop) {
            graphics.pose().popPose();
            return;
        }

        int titleBottom = innerTop + this.titleBarHeight;
        graphics.fill(innerLeft, innerTop, innerRight, Math.min(titleBottom, innerBottom), theme.pip_window_title_bar_color.getColorInt());
        graphics.pose().popPose();

        int bodyX = getBodyX();
        int bodyY = getBodyY();
        int bodyWidth = getBodyWidth();
        int bodyHeight = getBodyHeight();
        if (bodyWidth > 0 && bodyHeight > 0) {
            graphics.fill(bodyX, bodyY, bodyX + bodyWidth, bodyY + bodyHeight, theme.pip_window_body_color.getColorInt());
        }
    }

    private void renderBodyScreen(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        if (this.screen == null) {
            return;
        }

        resizeScreenIfNeeded();

        int bodyWidth = getBodyWidth();
        int bodyHeight = getBodyHeight();
        if (bodyWidth <= 0 || bodyHeight <= 0) {
            return;
        }

        int bodyX = getBodyX();
        int bodyY = getBodyY();
        double renderScale = getScreenRenderScaleFactor();
        double inputScale = renderScale <= 0.0 ? 1.0 : 1.0 / renderScale;
        int localMouseX = (int) Math.floor((mouseX - bodyX) * inputScale);
        int localMouseY = (int) Math.floor((mouseY - bodyY) * inputScale);
        if (!PiPWindowHandler.INSTANCE.isWindowFocused(this)) {
            localMouseX = -100000;
            localMouseY = -100000;
        }

        graphics.enableScissor(bodyX, bodyY, bodyX + bodyWidth, bodyY + bodyHeight);
        graphics.pose().pushPose();
        graphics.pose().translate(bodyX, bodyY, 0);
        if (renderScale != 1.0) {
            graphics.pose().scale((float) renderScale, (float) renderScale, 1.0F);
        }
        PiPWindowHandler.INSTANCE.beginScreenRender(this, renderScale);
        this.screenRendering = true;
        try {
            RenderingUtils.setMenuBlurringBlocked(true);
            this.screen.renderWithTooltip(graphics, localMouseX, localMouseY, partial);
            RenderingUtils.setMenuBlurringBlocked(false);
        } finally {
            this.screenRendering = false;
            PiPWindowHandler.INSTANCE.endScreenRender(this);
        }
        graphics.pose().popPose();
        graphics.disableScissor();
    }

    private void renderWindowForeground(@Nonnull GuiGraphics graphics, int mouseX, int mouseY) {
        Font font = this.minecraft.font;

        float scale = getFrameRenderScale();
        int frameX = getFrameX();
        int frameY = getFrameY();
        int frameWidth = getFrameWidth();
        int titleBarY = frameY + this.borderThickness;
        int titleBarX = frameX + this.borderThickness;
        int titleBarRight = frameX + frameWidth - this.borderThickness;
        int titleBarHeight = Math.max(0, this.titleBarHeight);
        int titleCenterY = titleBarY + (titleBarHeight - font.lineHeight) / 2;

        UIColorTheme theme = getTheme();
        int closeX = getCloseButtonX();
        int buttonY = getButtonY();
        int maximizeX = getMaximizeButtonX();
        int frameCloseX = getFrameCloseButtonX();
        int frameButtonY = getFrameButtonY();
        int frameMaximizeX = getFrameMaximizeButtonX();

        int buttonSize = Math.max(1, toScreenSpace(this.buttonSize));
        boolean maximizeHovered = this.maximizable && isPointInArea(mouseX, mouseY, maximizeX, buttonY, buttonSize, buttonSize);
        boolean closeHovered = this.closable && isPointInArea(mouseX, mouseY, closeX, buttonY, buttonSize, buttonSize);

        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, scale);
        if (this.maximizable) {
            renderButton(graphics, theme, frameMaximizeX, frameButtonY, maximizeHovered, getActiveMaximizeButtonIcon(), getMaximizeButtonLabel());
        }

        if (this.closable) {
            renderButton(graphics, theme, frameCloseX, frameButtonY, closeHovered, this.closeButtonIcon, "X");
        }

        int iconSize = Math.min(this.titleBarHeight - this.buttonPadding * 2, this.titleBarHeight);
        int textStartX = titleBarX + this.buttonPadding + 2;
        if (this.icon != null && iconSize > 0) {
            int iconX = titleBarX + this.buttonPadding;
            int iconY = titleBarY + (this.titleBarHeight - iconSize) / 2;
            graphics.blit(this.icon, iconX, iconY, 0, 0, iconSize, iconSize, iconSize, iconSize);
            textStartX = iconX + iconSize + this.buttonPadding;
        }

        int titleRightLimit = frameMaximizeX - this.buttonPadding;
        if (!this.maximizable) {
            titleRightLimit = frameCloseX - this.buttonPadding;
        }
        int maxTitleWidth = Math.max(0, titleRightLimit - textStartX);
        var split = font.split(this.title, maxTitleWidth);
        FormattedCharSequence titleText = !split.isEmpty() ? split.get(0) : Component.empty().getVisualOrderText();
        graphics.drawString(font, titleText, textStartX, titleCenterY, theme.pip_window_title_text_color.getColorInt(), false);

        if (titleBarHeight > 0) {
            int bottom = titleBarY + titleBarHeight;
            graphics.fill(titleBarX, bottom - 1, titleBarRight, bottom, theme.pip_window_border_color.getColorInt());
        }
        graphics.pose().popPose();
    }

    private void renderButton(@Nonnull GuiGraphics graphics, @Nonnull UIColorTheme theme, int x, int y, boolean hovered, @Nullable ResourceLocation icon, @Nonnull String label) {
        int color = hovered
                ? theme.pip_window_button_color_hover.getColorInt()
                : theme.pip_window_button_color_normal.getColorInt();
        graphics.fill(x, y, x + this.buttonSize, y + this.buttonSize, color);
        if (icon != null) {
            int iconSize = Math.max(1, Math.min(this.buttonSize - 4, DEFAULT_ICON_TEXTURE_SIZE));
            int iconX = x + (this.buttonSize - iconSize) / 2;
            int iconY = y + (this.buttonSize - iconSize) / 2;
            UIBase.getUIColorTheme().setUITextureShaderColor(graphics, 1.0F);
            graphics.blit(icon, iconX, iconY, 0, 0, iconSize, iconSize, DEFAULT_ICON_TEXTURE_SIZE, DEFAULT_ICON_TEXTURE_SIZE);
            RenderingUtils.resetShaderColor(graphics);
        } else {
            int textX = x + (this.buttonSize - this.minecraft.font.width(label)) / 2;
            int textY = y + (this.buttonSize - this.minecraft.font.lineHeight) / 2;
            graphics.drawString(this.minecraft.font, label, textX, textY, theme.pip_window_button_text_color.getColorInt(), false);
        }
    }

    public void tick() {
        clampWindowToScreenSize();
        if (this.screen != null) {
            this.screen.tick();
        }
    }

    public PiPWindow setScreen(@Nullable Screen screen) {
        if (this.screen == screen) {
            return this;
        }
        if (this.screen != null) {
            if (this.screen instanceof PipableScreen ps) {
                ps.setWindow(null);
                ps.onScreenClosed();
            }
            this.screen.removed(); // legacy support for non-PiP screens
        }
        this.screen = screen;
        if (this.screen != null) {
            if (this.screen instanceof PipableScreen ps) {
                ps.setWindow(this);
            }
            int screenWidth = getScreenWidth();
            int screenHeight = getScreenHeight();
            this.screen.init(this.minecraft, screenWidth, screenHeight);
            this.lastScreenWidth = screenWidth;
            this.lastScreenHeight = screenHeight;
        }
        return this;
    }

    @Nullable
    public Screen getScreen() {
        return this.screen;
    }

    public PiPWindow setTitle(@Nonnull Component title) {
        this.title = Objects.requireNonNull(title, "title");
        return this;
    }

    public Component getTitle() {
        return this.title;
    }

    public PiPWindow setIcon(@Nullable ResourceLocation icon) {
        this.icon = icon;
        return this;
    }

    @Nullable
    public ResourceLocation getIcon() {
        return this.icon;
    }

    public PiPWindow setCloseButtonIcon(@Nullable ResourceLocation icon) {
        this.closeButtonIcon = icon;
        return this;
    }

    @Nullable
    public ResourceLocation getCloseButtonIcon() {
        return this.closeButtonIcon;
    }

    public PiPWindow setMaximizeButtonIcon(@Nullable ResourceLocation icon) {
        this.maximizeButtonIcon = icon;
        return this;
    }

    @Nullable
    public ResourceLocation getMaximizeButtonIcon() {
        return this.maximizeButtonIcon;
    }

    public PiPWindow setNormalizeButtonIcon(@Nullable ResourceLocation icon) {
        this.normalizeButtonIcon = icon;
        return this;
    }

    @Nullable
    public ResourceLocation getNormalizeButtonIcon() {
        return this.normalizeButtonIcon;
    }

    public PiPWindow setVisible(boolean visible) {
        this.visible = visible;
        return this;
    }

    public boolean isVisible() {
        return this.visible;
    }

    public boolean isScreenRendering() {
        return this.screenRendering;
    }

    public boolean isSizeScaledToGuiScale() {
        return this.sizeScaledToGuiScale;
    }

    public PiPWindow setSizeScaledToGuiScale(boolean sizeScaledToGuiScale) {
        this.sizeScaledToGuiScale = sizeScaledToGuiScale;
        clampWindowToScreenSize();
        resizeScreenIfNeeded();
        return this;
    }

    public PiPWindow setResizable(boolean resizable) {
        this.resizable = resizable;
        return this;
    }

    public boolean isResizable() {
        return this.resizable;
    }

    public PiPWindow setMovable(boolean movable) {
        this.movable = movable;
        return this;
    }

    public boolean isMovable() {
        return this.movable;
    }

    public PiPWindow setMaximizable(boolean maximizable) {
        this.maximizable = maximizable;
        return this;
    }

    public boolean isMaximizable() {
        return this.maximizable;
    }

    public PiPWindow setClosable(boolean closable) {
        this.closable = closable;
        return this;
    }

    public boolean isClosable() {
        return this.closable;
    }

    public boolean shouldCloseScreenWithWindow() {
        return closeScreenWithWindow;
    }

    public PiPWindow setCloseScreenWithWindow(boolean closeScreenWithWindow) {
        this.closeScreenWithWindow = closeScreenWithWindow;
        return this;
    }

    public void refreshScreen() {
        if (this.screen == null) {
            return;
        }
        int screenWidth = getScreenWidth();
        int screenHeight = getScreenHeight();
        this.lastScreenWidth = screenWidth;
        this.lastScreenHeight = screenHeight;
        this.screen.resize(this.minecraft, screenWidth, screenHeight);
    }

    public PiPWindow setCloseCallback(@Nullable Runnable closeCallback) {
        this.closeCallback = closeCallback;
        return this;
    }

    public void addCloseCallback(@Nonnull Runnable closeCallback) {
        Objects.requireNonNull(closeCallback, "closeCallback");
        if (this.closeCallback == null) {
            this.closeCallback = closeCallback;
        } else {
            Runnable existing = this.closeCallback;
            this.closeCallback = () -> {
                existing.run();
                closeCallback.run();
            };
        }
    }

    public void close() {
        if (this.closeCallback != null) {
            this.closeCallback.run();
        }
    }

    void markClosingFromScreen() {
        this.closingFromScreen = true;
    }

    boolean consumeClosingFromScreen() {
        boolean value = this.closingFromScreen;
        this.closingFromScreen = false;
        return value;
    }

    public void handleClosed() {
        this.closingFromScreen = false;
        if (this.screen != null) {
            this.screen.removed();
        }
        if (!this.childWindows.isEmpty()) {
            for (PiPWindow child : new ArrayList<>(this.childWindows)) {
                child.parentWindow = null;
            }
            this.childWindows.clear();
            this.inputLockedByChildren = false;
        }
    }

    public PiPWindow setPosition(int x, int y) {
        this.x = x;
        this.y = y;
        clampTitleBarToScreen();
        return this;
    }

    public PiPWindow setSize(int width, int height) {
        this.width = Math.max(width, getRawMinimumWidth());
        this.height = Math.max(height, getRawMinimumHeight());
        clampWindowToScreenSize();
        resizeScreenIfNeeded();
        return this;
    }

    public PiPWindow setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = Math.max(width, getRawMinimumWidth());
        this.height = Math.max(height, getRawMinimumHeight());
        clampWindowToScreenSize();
        resizeScreenIfNeeded();
        return this;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getWidth() {
        return getScaledWidth();
    }

    public int getHeight() {
        return getScaledHeight();
    }

    public int getBodyX() {
        int frameX = getFrameX();
        int innerLeft = frameX + this.borderThickness;
        return toScreenSpace(innerLeft);
    }

    public int getBodyY() {
        int frameY = getFrameY();
        int innerTop = frameY + this.borderThickness + this.titleBarHeight;
        return toScreenSpace(innerTop);
    }

    public int getBodyWidth() {
        int frameX = getFrameX();
        int frameWidth = getFrameWidth();
        int innerLeft = frameX + this.borderThickness;
        int innerRight = frameX + frameWidth - this.borderThickness;
        return Math.max(0, toScreenSpace(innerRight) - toScreenSpace(innerLeft));
    }

    public int getBodyHeight() {
        int frameY = getFrameY();
        int frameHeight = getFrameHeight();
        int innerTop = frameY + this.borderThickness + this.titleBarHeight;
        int innerBottom = frameY + frameHeight - this.borderThickness;
        int value = toScreenSpace(innerBottom) - toScreenSpace(innerTop);
        return Math.max(0, value);
    }

    public int getMinimumWidth() {
        return Math.max(1, getScaledSize(getRawMinimumWidth()));
    }

    public int getMinimumHeight() {
        int min = Math.max(1, getScaledSize(getRawMinimumHeight()));
        return Math.max(min, getLayoutMinimumHeight());
    }

    private int getMaximumWidth() {
        return this.minecraft.getWindow().getGuiScaledWidth();
    }

    private int getMaximumHeight() {
        int screenHeight = this.minecraft.getWindow().getGuiScaledHeight();
        return Math.max(0, screenHeight - getMenuBarHeight());
    }

    private int getMenuBarHeight() {
        return (int) ((float) MenuBar.HEIGHT * MenuBar.getRenderScale());
    }

    public PiPWindow setMinSize(int minWidth, int minHeight) {
        this.minWidth = Math.max(1, minWidth);
        this.minHeight = Math.max(1, minHeight);
        setSize(this.width, this.height);
        return this;
    }

    public PiPWindow setTitleBarHeight(int titleBarHeight) {
        this.titleBarHeight = Math.max(0, titleBarHeight);
        setSize(this.width, this.height);
        return this;
    }

    public PiPWindow setBorderThickness(int borderThickness) {
        this.borderThickness = Math.max(0, borderThickness);
        setSize(this.width, this.height);
        return this;
    }

    public PiPWindow setResizeMargin(int resizeMargin) {
        this.resizeMargin = Math.max(0, resizeMargin);
        return this;
    }

    public PiPWindow setButtonSize(int buttonSize) {
        this.buttonSize = Math.max(1, buttonSize);
        return this;
    }

    public PiPWindow setButtonPadding(int buttonPadding) {
        this.buttonPadding = Math.max(0, buttonPadding);
        return this;
    }

    public boolean isMaximized() {
        return this.maximized;
    }

    public void toggleMaximized() {
        setMaximized(!this.maximized);
    }

    public PiPWindow setMaximized(boolean maximized) {
        if (this.maximized == maximized) {
            return this;
        }
        if (maximized) {
            this.restoreX = this.x;
            this.restoreY = this.y;
            this.restoreRawWidth = this.width;
            this.restoreRawHeight = this.height;
            this.maximized = true;
            int maxWidth = getMaximumWidth();
            int maxHeight = getMaximumHeight();
            setScaledBounds(0, getMenuBarHeight(), maxWidth, maxHeight);
        } else {
            this.maximized = false;
            setBounds(this.restoreX, this.restoreY, this.restoreRawWidth, this.restoreRawHeight);
        }
        return this;
    }

    public PiPWindow setInputLocked(boolean inputLocked) {
        this.inputLocked = inputLocked;
        return this;
    }

    public boolean isInputLocked() {
        return this.inputLocked || this.inputLockedByChildren;
    }

    @Nullable
    public PiPWindow getParentWindow() {
        return this.parentWindow;
    }

    public List<PiPWindow> getChildWindows() {
        return Collections.unmodifiableList(this.childWindows);
    }

    void registerChildWindow(@Nonnull PiPWindow child) {
        if (this.childWindows.contains(child)) {
            return;
        }
        this.childWindows.add(child);
        this.inputLockedByChildren = true;
        child.parentWindow = this;
    }

    void unregisterChildWindow(@Nonnull PiPWindow child) {
        if (this.childWindows.remove(child)) {
            child.parentWindow = null;
            this.inputLockedByChildren = !this.childWindows.isEmpty();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return handleMouseClicked(mouseX, mouseY, button, true);
    }

    public boolean mouseClickedWithoutScreen(double mouseX, double mouseY, int button) {
        return handleMouseClicked(mouseX, mouseY, button, false);
    }

    private boolean handleMouseClicked(double mouseX, double mouseY, int button, boolean allowScreenInput) {
        if (!this.visible || isInputLocked()) {
            return false;
        }

        if (button == 0) {
            int buttonSize = Math.max(1, toScreenSpace(this.buttonSize));
            if (this.closable && isPointInArea(mouseX, mouseY, getCloseButtonX(), getButtonY(), buttonSize, buttonSize)) {
                close();
                return true;
            }
            if (this.maximizable && isPointInArea(mouseX, mouseY, getMaximizeButtonX(), getButtonY(), buttonSize, buttonSize)) {
                toggleMaximized();
                return true;
            }
            boolean clickedTitleBar = isPointInTitleBar(mouseX, mouseY);
            if (!clickedTitleBar) {
                this.lastTitleBarClickTime = 0;
            } else if (this.maximizable) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - this.lastTitleBarClickTime < TITLE_BAR_DOUBLE_CLICK_TIME_MS) {
                    this.lastTitleBarClickTime = 0;
                    toggleMaximized();
                    return true;
                }
                this.lastTitleBarClickTime = currentTime;
            }
            PiPWindowResizeHandle handle = getResizeHandleAt(mouseX, mouseY);
            if (handle != PiPWindowResizeHandle.NONE) {
                beginResize(handle, mouseX, mouseY);
                return true;
            }
            if (this.movable && isPointInTitleBar(mouseX, mouseY) && !this.maximized) {
                beginDrag(mouseX, mouseY);
                return true;
            }
        }

        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        if (allowScreenInput && this.screen != null && isPointInBody(mouseX, mouseY)) {
            return this.screen.mouseClicked(toScreenMouseX(mouseX), toScreenMouseY(mouseY), button);
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!this.visible || isInputLocked()) {
            return false;
        }

        boolean wasDragging = this.draggingTitleBar || this.activeResizeHandle != PiPWindowResizeHandle.NONE;
        if (button == 0) {
            this.draggingTitleBar = false;
            this.activeResizeHandle = PiPWindowResizeHandle.NONE;
            this.setDragging(false);
        }
        if (wasDragging) {
            return true;
        }

        boolean handled = super.mouseReleased(mouseX, mouseY, button);
        if (this.screen != null) {
            handled = this.screen.mouseReleased(toScreenMouseX(mouseX), toScreenMouseY(mouseY), button) || handled;
        }
        return handled;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!this.visible || isInputLocked()) {
            return false;
        }

        if (button == 0) {
            if (this.draggingTitleBar) {
                setPosition((int) Math.round(mouseX - this.dragOffsetX), (int) Math.round(mouseY - this.dragOffsetY));
                return true;
            }
            if (this.activeResizeHandle != PiPWindowResizeHandle.NONE) {
                updateResize(mouseX, mouseY);
                return true;
            }
        }

        if (super.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }

        if (this.screen != null) {
            double inputScale = getScreenInputScaleFactor();
            return this.screen.mouseDragged(toScreenMouseX(mouseX), toScreenMouseY(mouseY), button, dragX * inputScale, dragY * inputScale);
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
        if (!this.visible || isInputLocked()) {
            return false;
        }

        if (super.mouseScrolled(mouseX, mouseY, scrollDeltaX, scrollDeltaY)) {
            return true;
        }

        if (this.screen != null && isPointInBody(mouseX, mouseY)) {
            return this.screen.mouseScrolled(toScreenMouseX(mouseX), toScreenMouseY(mouseY), scrollDeltaX, scrollDeltaY);
        }

        return false;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (!this.visible) {
            return;
        }
        for (GuiEventListener child : this.children) {
            child.mouseMoved(mouseX, mouseY);
        }
        if (this.screen != null && !isInputLocked()) {
            double screenMouseX = toScreenMouseX(mouseX);
            double screenMouseY = toScreenMouseY(mouseY);
            if (!PiPWindowHandler.INSTANCE.isWindowFocused(this)) {
                screenMouseX = -100000;
                screenMouseY = -100000;
            }
            this.screen.mouseMoved(screenMouseX, screenMouseY);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.visible || isInputLocked()) {
            return false;
        }
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return this.screen != null && this.screen.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (!this.visible || isInputLocked()) {
            return false;
        }
        if (super.keyReleased(keyCode, scanCode, modifiers)) {
            return true;
        }
        return this.screen != null && this.screen.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (!this.visible || isInputLocked()) {
            return false;
        }
        if (super.charTyped(codePoint, modifiers)) {
            return true;
        }
        return this.screen != null && this.screen.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return isPointInArea(mouseX, mouseY, this.x, this.y, getWidth(), getHeight());
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return this.children;
    }

    public void addChild(@Nonnull GuiEventListener child) {
        this.children.add(Objects.requireNonNull(child, "child"));
    }

    public void removeChild(@Nonnull GuiEventListener child) {
        this.children.remove(child);
    }

    private boolean isPointInTitleBar(double mouseX, double mouseY) {
        int frameX = getFrameX();
        int frameY = getFrameY();
        int frameWidth = getFrameWidth();
        int titleBarY = frameY + this.borderThickness;
        int titleBarBottom = titleBarY + this.titleBarHeight;
        int screenX = toScreenSpace(frameX);
        int screenY = toScreenSpace(titleBarY);
        int screenRight = toScreenSpace(frameX + frameWidth);
        int screenBottom = toScreenSpace(titleBarBottom);
        return isPointInArea(mouseX, mouseY, screenX, screenY, Math.max(0, screenRight - screenX), Math.max(0, screenBottom - screenY));
    }

    private boolean isPointInBody(double mouseX, double mouseY) {
        return isPointInArea(mouseX, mouseY, getBodyX(), getBodyY(), getBodyWidth(), getBodyHeight());
    }

    private int getButtonY() {
        return toScreenSpace(getFrameButtonY());
    }

    private int getCloseButtonX() {
        return toScreenSpace(getFrameCloseButtonX());
    }

    private int getMaximizeButtonX() {
        return toScreenSpace(getFrameMaximizeButtonX());
    }

    @Nullable
    private ResourceLocation getActiveMaximizeButtonIcon() {
        return this.maximized ? this.normalizeButtonIcon : this.maximizeButtonIcon;
    }

    private String getMaximizeButtonLabel() {
        return this.maximized ? "[]" : "[ ]";
    }

    private int getFrameButtonY() {
        int frameY = getFrameY();
        int titleHeight = Math.max(0, this.titleBarHeight);
        int buttonSize = Math.max(1, this.buttonSize);
        return frameY + this.borderThickness + Math.max(0, (titleHeight - buttonSize) / 2);
    }

    private int getFrameCloseButtonX() {
        int frameX = getFrameX();
        int frameWidth = getFrameWidth();
        return frameX + frameWidth - this.borderThickness - this.buttonPadding - this.buttonSize;
    }

    private int getFrameMaximizeButtonX() {
        return getFrameCloseButtonX() - this.buttonPadding - this.buttonSize;
    }

    private void updateResizeCursor(int mouseX, int mouseY) {
        if (!this.visible) {
            return;
        }
        PiPWindowResizeHandle handle = this.activeResizeHandle != PiPWindowResizeHandle.NONE
                ? this.activeResizeHandle
                : getResizeHandleAt(mouseX, mouseY);
        if (handle == PiPWindowResizeHandle.NONE) {
            return;
        }
        switch (handle) {
            case LEFT, RIGHT -> CursorHandler.setClientTickCursor(CursorHandler.CURSOR_RESIZE_HORIZONTAL);
            case TOP, BOTTOM -> CursorHandler.setClientTickCursor(CursorHandler.CURSOR_RESIZE_VERTICAL);
            case TOP_LEFT, BOTTOM_RIGHT -> CursorHandler.setClientTickCursor(CursorHandler.CURSOR_RESIZE_NWSE);
            case TOP_RIGHT, BOTTOM_LEFT -> CursorHandler.setClientTickCursor(CursorHandler.CURSOR_RESIZE_NESW);
            default -> {
            }
        }
    }

    private PiPWindowResizeHandle getResizeHandleAt(double mouseX, double mouseY) {
        if (!this.resizable || this.maximized) {
            return PiPWindowResizeHandle.NONE;
        }
        int frameX = getFrameX();
        int frameY = getFrameY();
        int frameWidth = getFrameWidth();
        int frameHeight = getFrameHeight();
        int screenLeft = toScreenSpace(frameX);
        int screenTop = toScreenSpace(frameY);
        int screenRight = toScreenSpace(frameX + frameWidth);
        int screenBottom = toScreenSpace(frameY + frameHeight);
        int margin = Math.max(1, toScreenSpace(this.resizeMargin));
        boolean left = mouseX >= screenLeft && mouseX <= screenLeft + margin;
        boolean right = mouseX >= screenRight - margin && mouseX <= screenRight;
        boolean top = mouseY >= screenTop && mouseY <= screenTop + margin;
        boolean bottom = mouseY >= screenBottom - margin && mouseY <= screenBottom;

        if (left && top) {
            return PiPWindowResizeHandle.TOP_LEFT;
        }
        if (right && top) {
            return PiPWindowResizeHandle.TOP_RIGHT;
        }
        if (left && bottom) {
            return PiPWindowResizeHandle.BOTTOM_LEFT;
        }
        if (right && bottom) {
            return PiPWindowResizeHandle.BOTTOM_RIGHT;
        }
        if (left) {
            return PiPWindowResizeHandle.LEFT;
        }
        if (right) {
            return PiPWindowResizeHandle.RIGHT;
        }
        if (top) {
            return PiPWindowResizeHandle.TOP;
        }
        if (bottom) {
            return PiPWindowResizeHandle.BOTTOM;
        }
        return PiPWindowResizeHandle.NONE;
    }

    private void beginDrag(double mouseX, double mouseY) {
        this.draggingTitleBar = true;
        this.dragOffsetX = mouseX - this.x;
        this.dragOffsetY = mouseY - this.y;
        this.setDragging(true);
    }

    private void beginResize(@Nonnull PiPWindowResizeHandle handle, double mouseX, double mouseY) {
        this.activeResizeHandle = handle;
        this.resizeStartMouseX = mouseX;
        this.resizeStartMouseY = mouseY;
        this.resizeStartX = this.x;
        this.resizeStartY = this.y;
        this.resizeStartWidth = getWidth();
        this.resizeStartHeight = getHeight();
        this.setDragging(true);
    }

    private void updateResize(double mouseX, double mouseY) {
        int deltaX = (int) Math.round(mouseX - this.resizeStartMouseX);
        int deltaY = (int) Math.round(mouseY - this.resizeStartMouseY);

        int newX = this.resizeStartX;
        int newY = this.resizeStartY;
        int newWidth = this.resizeStartWidth;
        int newHeight = this.resizeStartHeight;

        // Keep the opposite edge anchored while resizing.
        if (this.activeResizeHandle.hasLeftEdge()) {
            newX += deltaX;
            newWidth -= deltaX;
        } else if (this.activeResizeHandle.hasRightEdge()) {
            newWidth += deltaX;
        }

        if (this.activeResizeHandle.hasTopEdge()) {
            newY += deltaY;
            newHeight -= deltaY;
        } else if (this.activeResizeHandle.hasBottomEdge()) {
            newHeight += deltaY;
        }

        int minWidth = getMinimumWidth();
        int minHeight = getMinimumHeight();
        if (newWidth < minWidth) {
            int diff = minWidth - newWidth;
            newWidth = minWidth;
            if (this.activeResizeHandle.hasLeftEdge()) {
                newX -= diff;
            }
        }
        if (newHeight < minHeight) {
            int diff = minHeight - newHeight;
            newHeight = minHeight;
            if (this.activeResizeHandle.hasTopEdge()) {
                newY -= diff;
            }
        }

        setScaledBounds(newX, newY, newWidth, newHeight);
    }

    private double toScreenMouseX(double mouseX) {
        return (mouseX - getBodyX()) * getScreenInputScaleFactor();
    }

    private double toScreenMouseY(double mouseY) {
        return (mouseY - getBodyY()) * getScreenInputScaleFactor();
    }

    private int getScaledWidth() {
        return getScaledSize(this.width);
    }

    private int getScaledHeight() {
        int scaled = getScaledSize(this.height);
        return Math.max(scaled, getLayoutMinimumHeight());
    }

    private float getFrameRenderScale() {
        return MenuBar.getRenderScale();
    }

    private int toFrameSpace(int value) {
        float scale = getFrameRenderScale();
        if (scale <= 0.0F || scale == 1.0F) {
            return value;
        }
        return Math.round(value / scale);
    }

    private int toScreenSpace(int value) {
        float scale = getFrameRenderScale();
        if (scale <= 0.0F || scale == 1.0F) {
            return value;
        }
        return Math.round(value * scale);
    }

    private int getFrameX() {
        return toFrameSpace(this.x);
    }

    private int getFrameY() {
        return toFrameSpace(this.y);
    }

    private int getFrameWidth() {
        return Math.max(0, toFrameSpace(getWidth()));
    }

    private int getFrameHeight() {
        return Math.max(0, toFrameSpace(getHeight()));
    }

    private int getScaledSize(int rawSize) {
        if (!this.sizeScaledToGuiScale) {
            return rawSize;
        }
        double scale = getMainGuiScale();
        if (scale <= 1.0) {
            return rawSize;
        }
        return Math.max(1, (int) Math.floor(rawSize / scale));
    }

    private int getRawSizeForScaled(int scaledSize) {
        if (!this.sizeScaledToGuiScale) {
            return scaledSize;
        }
        double scale = getMainGuiScale();
        if (scale <= 1.0) {
            return scaledSize;
        }
        return Math.max(1, (int) Math.round(scaledSize * scale));
    }

    private int getRawMinimumWidth() {
        return Math.max(1, this.minWidth);
    }

    private int getRawMinimumHeight() {
        int min = Math.max(1, this.minHeight);
        int layoutMin = getLayoutMinimumHeight();
        if (!this.sizeScaledToGuiScale) {
            return Math.max(min, layoutMin);
        }
        double scale = getMainGuiScale();
        if (scale <= 1.0) {
            return Math.max(min, layoutMin);
        }
        int layoutScaled = Math.max(1, (int) Math.ceil(layoutMin * scale));
        return Math.max(min, layoutScaled);
    }

    private int getLayoutMinimumHeight() {
        int titleHeight = Math.max(0, toScreenSpace(this.titleBarHeight));
        int border = Math.max(0, toScreenSpace(this.borderThickness));
        int layoutMin = titleHeight + border * 2 + 1;
        return Math.max(1, layoutMin);
    }

    private int clampScaledWidth(int width) {
        int minWidth = getMinimumWidth();
        int maxWidth = getMaximumWidth();
        if (maxWidth <= 0) {
            return Math.max(1, Math.max(width, minWidth));
        }
        int clampedMin = Math.min(minWidth, maxWidth);
        int clamped = Math.min(Math.max(width, clampedMin), maxWidth);
        return Math.max(1, clamped);
    }

    private int clampScaledHeight(int height) {
        int minHeight = getMinimumHeight();
        int maxHeight = getMaximumHeight();
        if (maxHeight <= 0) {
            return Math.max(1, Math.max(height, minHeight));
        }
        int clampedMin = Math.min(minHeight, maxHeight);
        int clamped = Math.min(Math.max(height, clampedMin), maxHeight);
        return Math.max(1, clamped);
    }

    private int getScreenWidth() {
        return getScaledScreenSize(getBodyWidth());
    }

    private int getScreenHeight() {
        return getScaledScreenSize(getBodyHeight());
    }

    private int getScaledScreenSize(int bodySize) {
        double mainScale = getMainGuiScale();
        double embeddedScale = getEmbeddedGuiScale();
        int framebufferSize = Math.max(1, (int) Math.round(bodySize * mainScale));
        int scaledSize = (int) ((double) framebufferSize / embeddedScale);
        if ((double) framebufferSize / embeddedScale > (double) scaledSize) {
            scaledSize++;
        }
        return Math.max(1, scaledSize);
    }

    private double getScreenRenderScaleFactor() {
        double mainScale = getMainGuiScale();
        if (mainScale <= 0.0) {
            return 1.0;
        }
        return getEmbeddedGuiScale() / mainScale;
    }

    private double getScreenInputScaleFactor() {
        double renderScale = getScreenRenderScaleFactor();
        if (renderScale <= 0.0) {
            return 1.0;
        }
        return 1.0 / renderScale;
    }

    private double getEmbeddedGuiScale() {
        int bodyWidth = getBodyWidth();
        int bodyHeight = getBodyHeight();
        int framebufferWidth = Math.max(1, (int) Math.round(bodyWidth * getMainGuiScale()));
        int framebufferHeight = Math.max(1, (int) Math.round(bodyHeight * getMainGuiScale()));
        int guiScaleSetting = this.minecraft.options.guiScale().get();
        int scale = calculateGuiScale(guiScaleSetting, this.minecraft.isEnforceUnicode(), framebufferWidth, framebufferHeight);
        return Math.max(1, scale);
    }

    private double getMainGuiScale() {
        return this.minecraft.getWindow().getGuiScale();
    }

    private int calculateGuiScale(int guiScaleSetting, boolean forceUnicode, int framebufferWidth, int framebufferHeight) {
        int scale = 1;
        while (scale != guiScaleSetting
                && scale < framebufferWidth
                && scale < framebufferHeight
                && framebufferWidth / (scale + 1) >= 320
                && framebufferHeight / (scale + 1) >= 240) {
            scale++;
        }

        if (forceUnicode && scale % 2 != 0) {
            scale++;
        }

        return scale;
    }

    private void setScaledBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        int scaledWidth = clampScaledWidth(width);
        int scaledHeight = clampScaledHeight(height);
        this.width = getRawSizeForScaled(scaledWidth);
        this.height = getRawSizeForScaled(scaledHeight);
        clampTitleBarToScreen();
        resizeScreenIfNeeded();
    }

    private boolean isPointInArea(double mouseX, double mouseY, int areaX, int areaY, int areaWidth, int areaHeight) {
        return mouseX >= areaX && mouseX < areaX + areaWidth && mouseY >= areaY && mouseY < areaY + areaHeight;
    }

    private UIColorTheme getTheme() {
        return UIBase.getUIColorTheme();
    }

    private void resizeScreenIfNeeded() {
        if (this.screen == null) {
            return;
        }
        int screenWidth = getScreenWidth();
        int screenHeight = getScreenHeight();
        if (screenWidth != this.lastScreenWidth || screenHeight != this.lastScreenHeight) {
            this.lastScreenWidth = screenWidth;
            this.lastScreenHeight = screenHeight;
            this.screen.resize(this.minecraft, screenWidth, screenHeight);
        }
    }

    private void clampWindowToScreenSize() {
        int maxWidth = getMaximumWidth();
        int maxHeight = getMaximumHeight();
        if (maxWidth <= 0 || maxHeight <= 0) {
            return;
        }
        int menuBarHeight = getMenuBarHeight();
        if (this.maximized) {
            if (this.x != 0 || this.y != menuBarHeight || getWidth() != maxWidth || getHeight() != maxHeight) {
                setScaledBounds(0, menuBarHeight, maxWidth, maxHeight);
            }
            return;
        }
        int scaledWidth = getWidth();
        int scaledHeight = getHeight();
        int clampedWidth = clampScaledWidth(scaledWidth);
        int clampedHeight = clampScaledHeight(scaledHeight);
        if (clampedWidth != scaledWidth || clampedHeight != scaledHeight) {
            setScaledBounds(this.x, this.y, clampedWidth, clampedHeight);
        } else {
            clampTitleBarToScreen();
        }
    }

    private void clampTitleBarToScreen() {
        int screenWidth = this.minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = this.minecraft.getWindow().getGuiScaledHeight();
        if (screenWidth <= 0 || screenHeight <= 0) {
            return;
        }

        int scaledWidth = getWidth();
        int minVisible = Math.max(1, (int) Math.ceil(scaledWidth * 0.2F));
        minVisible = Math.min(minVisible, scaledWidth);
        minVisible = Math.min(minVisible, screenWidth);
        int minX = minVisible - scaledWidth;
        int maxX = screenWidth - minVisible;
        if (this.x < minX) {
            this.x = minX;
        } else if (this.x > maxX) {
            this.x = maxX;
        }

        int border = Math.max(0, toScreenSpace(this.borderThickness));
        int titleHeight = Math.max(0, toScreenSpace(this.titleBarHeight));
        int minY = getMenuBarHeight() - border;
        int maxY = screenHeight - border - titleHeight;
        if (maxY < minY) {
            this.y = minY;
        } else if (this.y < minY) {
            this.y = minY;
        } else if (this.y > maxY) {
            this.y = maxY;
        }
    }
}
