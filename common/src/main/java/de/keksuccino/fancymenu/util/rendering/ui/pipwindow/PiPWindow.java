package de.keksuccino.fancymenu.util.rendering.ui.pipwindow;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
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
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class PiPWindow extends AbstractContainerEventHandler implements Renderable {

    public static final int DEFAULT_TITLE_BAR_HEIGHT = 18;
    public static final int DEFAULT_BORDER_THICKNESS = 1;
    public static final int DEFAULT_BUTTON_SIZE = 12;
    public static final int DEFAULT_BUTTON_PADDING = 2;
    public static final int DEFAULT_MIN_WIDTH = 120;
    public static final int DEFAULT_MIN_HEIGHT = 80;
    public static final int DEFAULT_RESIZE_MARGIN = 4;

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

    private boolean visible = true;
    private boolean resizable = true;
    private boolean movable = true;
    private boolean maximizable = true;
    private boolean closable = true;

    private boolean maximized = false;
    private int restoreX;
    private int restoreY;
    private int restoreWidth;
    private int restoreHeight;

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

    @Nullable
    private Runnable closeCallback;

    private int lastBodyWidth = -1;
    private int lastBodyHeight = -1;

    public PiPWindow(@Nonnull Component title, int x, int y, int width, int height) {
        this(title, x, y, width, height, null);
    }

    public PiPWindow(@Nonnull Component title, int x, int y, int width, int height, @Nullable Screen screen) {
        this.title = Objects.requireNonNull(title, "title");
        this.x = x;
        this.y = y;
        this.width = Math.max(width, getMinimumWidth());
        this.height = Math.max(height, getMinimumHeight());
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

        RenderingUtils.setDepthTestLocked(false);
        RenderSystem.enableDepthTest();

    }

    private void renderWindowBackground(@Nonnull GuiGraphics graphics) {
        int right = this.x + this.width;
        int bottom = this.y + this.height;
        UIColorTheme theme = getTheme();
        graphics.fill(this.x, this.y, right, bottom, theme.pip_window_border_color.getColorInt());

        int innerLeft = this.x + this.borderThickness;
        int innerTop = this.y + this.borderThickness;
        int innerRight = right - this.borderThickness;
        int innerBottom = bottom - this.borderThickness;
        if (innerRight <= innerLeft || innerBottom <= innerTop) {
            return;
        }

        int titleBottom = innerTop + this.titleBarHeight;
        graphics.fill(innerLeft, innerTop, innerRight, Math.min(titleBottom, innerBottom), theme.pip_window_title_bar_color.getColorInt());
        if (titleBottom < innerBottom) {
            graphics.fill(innerLeft, titleBottom, innerRight, innerBottom, theme.pip_window_body_color.getColorInt());
        }
    }

    private void renderBodyScreen(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        if (this.screen == null) {
            return;
        }

        int bodyWidth = getBodyWidth();
        int bodyHeight = getBodyHeight();
        if (bodyWidth <= 0 || bodyHeight <= 0) {
            return;
        }

        int bodyX = getBodyX();
        int bodyY = getBodyY();
        int localMouseX = (int) Math.floor(mouseX - bodyX);
        int localMouseY = (int) Math.floor(mouseY - bodyY);

        graphics.enableScissor(bodyX, bodyY, bodyX + bodyWidth, bodyY + bodyHeight);
        graphics.pose().pushPose();
        graphics.pose().translate(bodyX, bodyY, 0);
        this.screen.renderWithTooltip(graphics, localMouseX, localMouseY, partial);
        graphics.pose().popPose();
        graphics.disableScissor();
    }

    private void renderWindowForeground(@Nonnull GuiGraphics graphics, int mouseX, int mouseY) {
        Font font = this.minecraft.font;

        int titleBarY = this.y + this.borderThickness;
        int titleBarX = this.x + this.borderThickness;
        int titleBarRight = this.x + this.width - this.borderThickness;
        int titleBarHeight = Math.max(0, this.titleBarHeight);
        int titleCenterY = titleBarY + (titleBarHeight - font.lineHeight) / 2;

        UIColorTheme theme = getTheme();
        int closeX = getCloseButtonX();
        int buttonY = getButtonY();
        int maximizeX = getMaximizeButtonX();

        if (this.maximizable) {
            renderButton(graphics, theme, maximizeX, buttonY, mouseX, mouseY, getMaximizeButtonLabel());
        }

        if (this.closable) {
            renderButton(graphics, theme, closeX, buttonY, mouseX, mouseY, "X");
        }

        int iconSize = Math.min(this.titleBarHeight - this.buttonPadding * 2, this.titleBarHeight);
        int textStartX = titleBarX + this.buttonPadding;
        if (this.icon != null && iconSize > 0) {
            int iconX = titleBarX + this.buttonPadding;
            int iconY = titleBarY + (this.titleBarHeight - iconSize) / 2;
            graphics.blit(this.icon, iconX, iconY, 0, 0, iconSize, iconSize, iconSize, iconSize);
            textStartX = iconX + iconSize + this.buttonPadding;
        }

        int titleRightLimit = maximizeX - this.buttonPadding;
        if (!this.maximizable) {
            titleRightLimit = closeX - this.buttonPadding;
        }
        int maxTitleWidth = Math.max(0, titleRightLimit - textStartX);
        String titleText = this.title.getString();
        String clippedTitle = font.plainSubstrByWidth(titleText, maxTitleWidth);
        if (!clippedTitle.isEmpty()) {
            graphics.drawString(font, clippedTitle, textStartX, titleCenterY, theme.pip_window_title_text_color.getColorInt(), false);
        }

        if (titleBarHeight > 0) {
            int bottom = titleBarY + titleBarHeight;
            graphics.fill(titleBarX, bottom - 1, titleBarRight, bottom, theme.pip_window_border_color.getColorInt());
        }
    }

    private void renderButton(@Nonnull GuiGraphics graphics, @Nonnull UIColorTheme theme, int x, int y, int mouseX, int mouseY, @Nonnull String label) {
        int color = isPointInArea(mouseX, mouseY, x, y, this.buttonSize, this.buttonSize)
                ? theme.pip_window_button_color_hover.getColorInt()
                : theme.pip_window_button_color_normal.getColorInt();
        graphics.fill(x, y, x + this.buttonSize, y + this.buttonSize, color);
        int textX = x + (this.buttonSize - this.minecraft.font.width(label)) / 2;
        int textY = y + (this.buttonSize - this.minecraft.font.lineHeight) / 2;
        graphics.drawString(this.minecraft.font, label, textX, textY, theme.pip_window_button_text_color.getColorInt(), false);
    }

    public void tick() {
        if (this.screen != null) {
            this.screen.tick();
        }
    }

    public void setScreen(@Nullable Screen screen) {
        if (this.screen == screen) {
            return;
        }
        if (this.screen != null) {
            this.screen.removed();
        }
        this.screen = screen;
        if (this.screen != null) {
            this.screen.init(this.minecraft, getBodyWidth(), getBodyHeight());
            this.lastBodyWidth = getBodyWidth();
            this.lastBodyHeight = getBodyHeight();
        }
    }

    @Nullable
    public Screen getScreen() {
        return this.screen;
    }

    public void setTitle(@Nonnull Component title) {
        this.title = Objects.requireNonNull(title, "title");
    }

    public Component getTitle() {
        return this.title;
    }

    public void setIcon(@Nullable ResourceLocation icon) {
        this.icon = icon;
    }

    @Nullable
    public ResourceLocation getIcon() {
        return this.icon;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isVisible() {
        return this.visible;
    }

    public void setResizable(boolean resizable) {
        this.resizable = resizable;
    }

    public boolean isResizable() {
        return this.resizable;
    }

    public void setMovable(boolean movable) {
        this.movable = movable;
    }

    public boolean isMovable() {
        return this.movable;
    }

    public void setMaximizable(boolean maximizable) {
        this.maximizable = maximizable;
    }

    public boolean isMaximizable() {
        return this.maximizable;
    }

    public void setClosable(boolean closable) {
        this.closable = closable;
    }

    public boolean isClosable() {
        return this.closable;
    }

    public void setCloseCallback(@Nullable Runnable closeCallback) {
        this.closeCallback = closeCallback;
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

    public void handleClosed() {
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

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void setSize(int width, int height) {
        this.width = Math.max(width, getMinimumWidth());
        this.height = Math.max(height, getMinimumHeight());
        resizeScreenIfNeeded();
    }

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = Math.max(width, getMinimumWidth());
        this.height = Math.max(height, getMinimumHeight());
        resizeScreenIfNeeded();
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public int getBodyX() {
        return this.x + this.borderThickness;
    }

    public int getBodyY() {
        return this.y + this.borderThickness + this.titleBarHeight;
    }

    public int getBodyWidth() {
        return Math.max(0, this.width - this.borderThickness * 2);
    }

    public int getBodyHeight() {
        int value = this.height - this.borderThickness * 2 - this.titleBarHeight;
        return Math.max(0, value);
    }

    public int getMinimumWidth() {
        return Math.max(1, this.minWidth);
    }

    public int getMinimumHeight() {
        int min = Math.max(1, this.minHeight);
        int layoutMin = this.titleBarHeight + this.borderThickness * 2 + 1;
        return Math.max(min, layoutMin);
    }

    public void setMinSize(int minWidth, int minHeight) {
        this.minWidth = Math.max(1, minWidth);
        this.minHeight = Math.max(1, minHeight);
        setSize(this.width, this.height);
    }

    public void setTitleBarHeight(int titleBarHeight) {
        this.titleBarHeight = Math.max(0, titleBarHeight);
        setSize(this.width, this.height);
    }

    public void setBorderThickness(int borderThickness) {
        this.borderThickness = Math.max(0, borderThickness);
        setSize(this.width, this.height);
    }

    public void setResizeMargin(int resizeMargin) {
        this.resizeMargin = Math.max(0, resizeMargin);
    }

    public void setButtonSize(int buttonSize) {
        this.buttonSize = Math.max(1, buttonSize);
    }

    public void setButtonPadding(int buttonPadding) {
        this.buttonPadding = Math.max(0, buttonPadding);
    }

    public boolean isMaximized() {
        return this.maximized;
    }

    public void toggleMaximized() {
        setMaximized(!this.maximized);
    }

    public void setMaximized(boolean maximized) {
        if (this.maximized == maximized) {
            return;
        }
        if (maximized) {
            this.restoreX = this.x;
            this.restoreY = this.y;
            this.restoreWidth = this.width;
            this.restoreHeight = this.height;
            int maxWidth = this.minecraft.getWindow().getGuiScaledWidth();
            int maxHeight = this.minecraft.getWindow().getGuiScaledHeight();
            setBounds(0, 0, maxWidth, maxHeight);
        } else {
            setBounds(this.restoreX, this.restoreY, this.restoreWidth, this.restoreHeight);
        }
        this.maximized = maximized;
    }

    public void setInputLocked(boolean inputLocked) {
        this.inputLocked = inputLocked;
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
        if (!this.visible || isInputLocked()) {
            return false;
        }

        if (button == 0) {
            if (this.closable && isPointInArea(mouseX, mouseY, getCloseButtonX(), getButtonY(), this.buttonSize, this.buttonSize)) {
                close();
                return true;
            }
            if (this.maximizable && isPointInArea(mouseX, mouseY, getMaximizeButtonX(), getButtonY(), this.buttonSize, this.buttonSize)) {
                toggleMaximized();
                return true;
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

        if (this.screen != null && isPointInBody(mouseX, mouseY)) {
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
                this.x = (int) Math.round(mouseX - this.dragOffsetX);
                this.y = (int) Math.round(mouseY - this.dragOffsetY);
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
            return this.screen.mouseDragged(toScreenMouseX(mouseX), toScreenMouseY(mouseY), button, dragX, dragY);
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
            this.screen.mouseMoved(toScreenMouseX(mouseX), toScreenMouseY(mouseY));
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
        return isPointInArea(mouseX, mouseY, this.x, this.y, this.width, this.height);
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
        int titleBarY = this.y + this.borderThickness;
        return isPointInArea(mouseX, mouseY, this.x, titleBarY, this.width, this.titleBarHeight);
    }

    private boolean isPointInBody(double mouseX, double mouseY) {
        return isPointInArea(mouseX, mouseY, getBodyX(), getBodyY(), getBodyWidth(), getBodyHeight());
    }

    private int getButtonY() {
        return this.y + this.borderThickness + Math.max(0, (this.titleBarHeight - this.buttonSize) / 2);
    }

    private int getCloseButtonX() {
        int right = this.x + this.width - this.borderThickness;
        return right - this.buttonPadding - this.buttonSize;
    }

    private int getMaximizeButtonX() {
        int closeX = getCloseButtonX();
        return closeX - this.buttonPadding - this.buttonSize;
    }

    private String getMaximizeButtonLabel() {
        return this.maximized ? "[]" : "[ ]";
    }

    private PiPWindowResizeHandle getResizeHandleAt(double mouseX, double mouseY) {
        if (!this.resizable || this.maximized) {
            return PiPWindowResizeHandle.NONE;
        }
        int margin = Math.max(1, this.resizeMargin);
        boolean left = mouseX >= this.x && mouseX <= this.x + margin;
        boolean right = mouseX >= this.x + this.width - margin && mouseX <= this.x + this.width;
        boolean top = mouseY >= this.y && mouseY <= this.y + margin;
        boolean bottom = mouseY >= this.y + this.height - margin && mouseY <= this.y + this.height;

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
        this.resizeStartWidth = this.width;
        this.resizeStartHeight = this.height;
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

        setBounds(newX, newY, newWidth, newHeight);
    }

    private double toScreenMouseX(double mouseX) {
        return mouseX - getBodyX();
    }

    private double toScreenMouseY(double mouseY) {
        return mouseY - getBodyY();
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
        int bodyWidth = getBodyWidth();
        int bodyHeight = getBodyHeight();
        if (bodyWidth != this.lastBodyWidth || bodyHeight != this.lastBodyHeight) {
            this.lastBodyWidth = bodyWidth;
            this.lastBodyHeight = bodyHeight;
            this.screen.resize(this.minecraft, bodyWidth, bodyHeight);
        }
    }
}
