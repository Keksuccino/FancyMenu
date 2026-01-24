package de.keksuccino.fancymenu.util.rendering.ui.menubar.v2;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.util.ConsumingSupplier;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.GuiBlurRenderer;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.PressState;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.TooltipHandler;
import de.keksuccino.fancymenu.util.rendering.ui.widget.NavigatableWidget;
import de.keksuccino.fancymenu.util.resource.ResourceSource;
import de.keksuccino.fancymenu.util.resource.ResourceSourceType;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import de.keksuccino.fancymenu.util.ListUtils;
import de.keksuccino.fancymenu.util.ScreenUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class MenuBar implements Renderable, GuiEventListener, NarratableEntry, NavigatableWidget {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final int HEIGHT = 28;
    public static final int ENTRY_LABEL_SPACE_LEFT_RIGHT = 6;

    protected final List<MenuBarEntry> leftEntries = new ArrayList<>();
    protected final List<MenuBarEntry> rightEntries = new ArrayList<>();
    protected final List<MenuBarClickListener> clickListeners = new ArrayList<>();
    protected final List<PendingContextMenuOpen> pendingContextMenuOpens = new ArrayList<>();
    protected boolean hovered = false;
    protected boolean expanded = true;
    protected boolean layoutReady = false;
    protected ClickableMenuBarEntry collapseOrExpandEntry;
    protected ResourceSupplier<ITexture> collapseExpandTextureSupplier = ResourceSupplier.image(ResourceSource.of("fancymenu:textures/menubar/icons/collapse_expand.png", ResourceSourceType.LOCATION).getSourceWithPrefix());
    protected boolean clickActive = false;
    protected int clickActiveButton = -1;

    public MenuBar() {
        this.collapseOrExpandEntry = this.addClickableEntry(Side.RIGHT, "collapse_or_expand", Component.empty(), (bar, entry) -> {
            this.setExpanded(!this.expanded);
        }).setIconTextureSupplier((bar, entry) -> this.collapseExpandTextureSupplier.get());
        this.addSpacerEntry(Side.RIGHT, "spacer_after_collapse_or_expand_entry").setWidth(10);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        float scale = getRenderScale();
        int scaledMouseX = (int) ((float)mouseX / scale);
        int scaledMouseY = (int) ((float)mouseY / scale);
        int y = 0;
        int width = ScreenUtils.getScreenWidth();
        int scaledWidth = (width != 0) ? (int)((float)width / scale) : 0;

        this.collapseOrExpandEntry.x = scaledWidth - this.collapseOrExpandEntry.getWidth();
        this.collapseOrExpandEntry.y = y;
        this.collapseOrExpandEntry.height = HEIGHT;
        this.collapseOrExpandEntry.hovered = this.collapseOrExpandEntry.isMouseOver(scaledMouseX, scaledMouseY);

        this.hovered = this.isMouseOver(mouseX, mouseY);

        RenderSystem.enableBlend();

        RenderSystem.disableDepthTest();
        RenderingUtils.setDepthTestLocked(true);

        UIBase.resetShaderColor(graphics);

        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, scale);

        if (this.expanded) {
            this.renderBackground(graphics, 0, y, scaledWidth, HEIGHT, partial, scale);
        } else {
            this.renderBackground(graphics, this.collapseOrExpandEntry.x, y, this.collapseOrExpandEntry.x + this.collapseOrExpandEntry.getWidth(), HEIGHT, partial, scale);
        }

        this.layoutReady = false;
        if (this.expanded) {
            //Render all visible entries
            int leftX = 0;
            for (MenuBarEntry e : this.leftEntries) {
                e.x = leftX;
                e.y = y;
                e.height = HEIGHT;
                e.hovered = e.isMouseOver(scaledMouseX, scaledMouseY);
                if (e.isVisible()) {
                    RenderSystem.enableBlend();
                    UIBase.resetShaderColor(graphics);
                    e.render(graphics, scaledMouseX, scaledMouseY, partial);
                }
                leftX += e.getWidth();
            }
            int rightX = scaledWidth;
            for (MenuBarEntry e : this.rightEntries) {
                e.x = rightX - e.getWidth();
                e.y = y;
                e.height = HEIGHT;
                e.hovered = e.isMouseOver(scaledMouseX, scaledMouseY);
                if (e.isVisible()) {
                    RenderSystem.enableBlend();
                    UIBase.resetShaderColor(graphics);
                    e.render(graphics, scaledMouseX, scaledMouseY, partial);
                }
                rightX -= e.getWidth();
            }
            this.layoutReady = true;
        } else {
            this.collapseOrExpandEntry.render(graphics, scaledMouseX, scaledMouseY, partial);
        }

        this.flushPendingContextMenuOpens();

        if (this.expanded) {
            this.renderBottomLine(graphics, scaledWidth);
        } else {
            this.renderExpandEntryBorder(graphics, scaledWidth);
        }

        graphics.pose().popPose();
        UIBase.resetShaderColor(graphics);

        graphics.pose().pushPose();

        //Render context menus of ContextMenuBarEntries
        for (MenuBarEntry e : ListUtils.mergeLists(this.leftEntries, this.rightEntries)) {
            if (e instanceof ContextMenuBarEntry c) {
                c.contextMenu.render(graphics, mouseX, mouseY, partial);
            }
        }

        RenderingUtils.setDepthTestLocked(false);

        graphics.pose().popPose();

        UIBase.resetShaderColor(graphics);

    }

    protected void renderBackground(GuiGraphics graphics, int xMin, int yMin, int xMax, int yMax, float partial, float scale) {
        boolean blurEnabled = UIBase.shouldBlur();
        int widthScaled = xMax - xMin;
        int heightScaled = yMax - yMin;
        if (!blurEnabled) {
            // Classic solid background in the menu bar's scaled coordinate space.
            graphics.fill(xMin, yMin, xMax, yMax, UIBase.getUITheme().ui_overlay_background_color.getColorInt());
            UIBase.resetShaderColor(graphics);
            return;
        }

        // Menu bar is rendered at a custom scale; convert back to screen-space for blur so the blurred region matches what users see.
        float width = widthScaled * scale;
        float height = heightScaled * scale;
        float blurX = xMin * scale;
        float blurY = yMin * scale;
        if (width > 0 && height > 0) {
            // Blur the menu bar background without rounded corners; use the UI theme color as a light tint.
            GuiBlurRenderer.renderBlurAreaWithIntensity(graphics, blurX, blurY, width, height, UIBase.getBlurRadius(), 0.0F, UIBase.getUITheme().ui_blur_overlay_background_tint, partial);
        }
        UIBase.resetShaderColor(graphics);
    }

    protected void renderBottomLine(GuiGraphics graphics, int width) {
        graphics.fill(0, MenuBar.HEIGHT - this.getBottomLineThickness(), width, MenuBar.HEIGHT, this.getBottomLineColor().getColorInt());
        UIBase.resetShaderColor(graphics);
    }

    protected void renderExpandEntryBorder(GuiGraphics graphics, int width) {
        //bottom line
        graphics.fill(this.collapseOrExpandEntry.x, MenuBar.HEIGHT - this.getBottomLineThickness(), width, MenuBar.HEIGHT, this.getBottomLineColor().getColorInt());
        //left side line
        graphics.fill(this.collapseOrExpandEntry.x - this.getBottomLineThickness(), 0, this.collapseOrExpandEntry.x, MenuBar.HEIGHT, this.getBottomLineColor().getColorInt());
        UIBase.resetShaderColor(graphics);
    }

    protected DrawableColor getBottomLineColor() {
        return UIBase.shouldBlur() ? UIBase.getUITheme().ui_blur_overlay_border_color : UIBase.getUITheme().ui_overlay_border_color;
    }

    @NotNull
    public SpacerMenuBarEntry addSpacerEntryAfter(@NotNull String addAfterIdentifier, @NotNull String identifier) {
        return this.addEntryAfter(addAfterIdentifier, new SpacerMenuBarEntry(identifier, this));
    }

    @NotNull
    public SpacerMenuBarEntry addSpacerEntryBefore(@NotNull String addBeforeIdentifier, @NotNull String identifier) {
        return this.addEntryBefore(addBeforeIdentifier, new SpacerMenuBarEntry(identifier, this));
    }

    @NotNull
    public SpacerMenuBarEntry addSpacerEntry(@NotNull Side side, @NotNull String identifier) {
        return this.addEntry(side, new SpacerMenuBarEntry(identifier, this));
    }

    @NotNull
    public SpacerMenuBarEntry addSpacerEntryAt(int index, @NotNull Side side, @NotNull String identifier) {
        return this.addEntryAt(index, side, new SpacerMenuBarEntry(identifier, this));
    }

    @NotNull
    public SeparatorMenuBarEntry addSeparatorEntryAfter(@NotNull String addAfterIdentifier, @NotNull String identifier) {
        return this.addEntryAfter(addAfterIdentifier, new SeparatorMenuBarEntry(identifier, this));
    }

    @NotNull
    public SeparatorMenuBarEntry addSeparatorEntryBefore(@NotNull String addBeforeIdentifier, @NotNull String identifier) {
        return this.addEntryBefore(addBeforeIdentifier, new SeparatorMenuBarEntry(identifier, this));
    }

    @NotNull
    public SeparatorMenuBarEntry addSeparatorEntry(@NotNull Side side, @NotNull String identifier) {
        return this.addEntry(side, new SeparatorMenuBarEntry(identifier, this));
    }

    @NotNull
    public SeparatorMenuBarEntry addSeparatorEntryAt(int index, @NotNull Side side, @NotNull String identifier) {
        return this.addEntryAt(index, side, new SeparatorMenuBarEntry(identifier, this));
    }

    /**
     * {@link ContextMenuBarEntry}s should only get added to the LEFT {@link Side} of the {@link MenuBar}.
     */
    @NotNull
    public ContextMenuBarEntry addContextMenuEntryAfter(@NotNull String addAfterIdentifier, @NotNull String identifier, @NotNull Component label, @NotNull ContextMenu contextMenu) {
        return this.addEntryAfter(addAfterIdentifier, new ContextMenuBarEntry(identifier, this, label, contextMenu));
    }

    /**
     * {@link ContextMenuBarEntry}s should only get added to the LEFT {@link Side} of the {@link MenuBar}.
     */
    @NotNull
    public ContextMenuBarEntry addContextMenuEntryBefore(@NotNull String addBeforeIdentifier, @NotNull String identifier, @NotNull Component label, @NotNull ContextMenu contextMenu) {
        return this.addEntryBefore(addBeforeIdentifier, new ContextMenuBarEntry(identifier, this, label, contextMenu));
    }

    /**
     * {@link ContextMenuBarEntry}s should only get added to the LEFT {@link Side} of the {@link MenuBar}.
     */
    @NotNull
    public ContextMenuBarEntry addContextMenuEntry(@NotNull String identifier, @NotNull Component label, @NotNull ContextMenu contextMenu) {
        return this.addEntry(Side.LEFT, new ContextMenuBarEntry(identifier, this, label, contextMenu));
    }

    /**
     * {@link ContextMenuBarEntry}s should only get added to the LEFT {@link Side} of the {@link MenuBar}.
     */
    @NotNull
    public ContextMenuBarEntry addContextMenuEntryAt(int index, @NotNull String identifier, @NotNull Component label, @NotNull ContextMenu contextMenu) {
        return this.addEntryAt(index, Side.LEFT, new ContextMenuBarEntry(identifier, this, label, contextMenu));
    }

    @NotNull
    public ClickableMenuBarEntry addClickableEntryAfter(@NotNull String addAfterIdentifier, @NotNull String identifier, @NotNull Component label, @NotNull ClickableMenuBarEntry.ClickAction clickAction) {
        return this.addEntryAfter(addAfterIdentifier, new ClickableMenuBarEntry(identifier, this, label, clickAction));
    }

    @NotNull
    public ClickableMenuBarEntry addClickableEntryBefore(@NotNull String addBeforeIdentifier, @NotNull String identifier, @NotNull Component label, @NotNull ClickableMenuBarEntry.ClickAction clickAction) {
        return this.addEntryBefore(addBeforeIdentifier, new ClickableMenuBarEntry(identifier, this, label, clickAction));
    }

    @NotNull
    public ClickableMenuBarEntry addClickableEntry(@NotNull Side side, @NotNull String identifier, @NotNull Component label, @NotNull ClickableMenuBarEntry.ClickAction clickAction) {
        return this.addEntry(side, new ClickableMenuBarEntry(identifier, this, label, clickAction));
    }

    @NotNull
    public ClickableMenuBarEntry addClickableEntryAt(int index, @NotNull Side side, @NotNull String identifier, @NotNull Component label, @NotNull ClickableMenuBarEntry.ClickAction clickAction) {
        return this.addEntryAt(index, side, new ClickableMenuBarEntry(identifier, this, label, clickAction));
    }

    @NotNull
    public <T extends MenuBarEntry> T addEntryAfter(@NotNull String addAfterIdentifier, @NotNull T entry) {
        Objects.requireNonNull(addAfterIdentifier);
        int index = this.getEntryIndex(addAfterIdentifier);
        Side side = this.getEntrySide(addAfterIdentifier);
        if ((index >= 0) && (side != null)) {
            index++;
        } else {
            LOGGER.error("[FANCYMENU] Failed to add MenuBar entry (" + entry.identifier + ") after other entry (" + addAfterIdentifier + ")! Target entry not found! Will add the entry at the end of left side instead!");
            index = this.leftEntries.size();
            side = Side.LEFT;
        }
        return this.addEntryAt(index, side, entry);
    }

    @NotNull
    public <T extends MenuBarEntry> T addEntryBefore(@NotNull String addBeforeIdentifier, @NotNull T entry) {
        Objects.requireNonNull(addBeforeIdentifier);
        int index = this.getEntryIndex(addBeforeIdentifier);
        Side side = this.getEntrySide(addBeforeIdentifier);
        if ((index < 0) || (side == null)) {
            LOGGER.error("[FANCYMENU] Failed to add MenuBar entry (" + entry.identifier + ") before other entry (" + addBeforeIdentifier + ")! Target entry not found! Will add the entry at the end of left side instead!");
            index = this.leftEntries.size();
            side = Side.LEFT;
        }
        return this.addEntryAt(index, side, entry);
    }

    @NotNull
    public <T extends MenuBarEntry> T addEntry(@NotNull Side side, @NotNull T entry) {
        int index = (side == Side.LEFT) ? this.leftEntries.size() : this.rightEntries.size();
        return this.addEntryAt(index, side, entry);
    }

    @NotNull
    public <T extends MenuBarEntry> T addEntryAt(int index, @NotNull Side side, @NotNull T entry) {
        Objects.requireNonNull(side);
        Objects.requireNonNull(entry);
        Objects.requireNonNull(entry.identifier);
        if (this.hasEntry(entry.identifier)) {
            LOGGER.error("[FANCYMENU] Failed to add MenuBar entry! Identifier already in use: " + entry.identifier);
        } else {
            if (side == Side.LEFT) {
                this.leftEntries.add(Math.max(0, Math.min(index, this.leftEntries.size())), entry);
            }
            if (side == Side.RIGHT) {
                this.rightEntries.add(Math.max(0, Math.min(index, this.rightEntries.size())), entry);
            }
            this.markLayoutDirty();
        }
        return entry;
    }

    public MenuBar removeEntry(@NotNull String identifier) {
        MenuBarEntry e = this.getEntry(identifier);
        if (e != null) {
            this.leftEntries.remove(e);
            this.rightEntries.remove(e);
            this.markLayoutDirty();
        }
        return this;
    }

    public MenuBar clearLeftEntries() {
        this.leftEntries.clear();
        this.markLayoutDirty();
        return this;
    }

    public MenuBar clearRightEntries() {
        this.rightEntries.clear();
        this.markLayoutDirty();
        return this;
    }

    public MenuBar clearEntries() {
        this.leftEntries.clear();
        this.rightEntries.clear();
        this.markLayoutDirty();
        return this;
    }

    public int getEntryIndex(@NotNull String identifier) {
        MenuBarEntry e = this.getEntry(identifier);
        if (e != null) {
            int index = this.leftEntries.indexOf(e);
            if (index == -1) index = this.rightEntries.indexOf(e);
            return index;
        }
        return -1;
    }

    @Nullable
    public Side getEntrySide(@NotNull String identifier) {
        MenuBarEntry e = this.getEntry(identifier);
        if (e != null) {
            if (this.leftEntries.contains(e)) return Side.LEFT;
            return Side.RIGHT;
        }
        return null;
    }

    @Nullable
    public MenuBarEntry getEntry(@NotNull String identifier) {
        Objects.requireNonNull(identifier);
        for (MenuBarEntry e : this.getEntries()) {
            if (e.identifier.equals(identifier)) return e;
        }
        return null;
    }

    public boolean hasEntry(@NotNull String identifier) {
        return this.getEntry(identifier) != null;
    }

    @NotNull
    public List<MenuBarEntry> getLeftEntries() {
        return new ArrayList<>(this.leftEntries);
    }

    @NotNull
    public List<MenuBarEntry> getRightEntries() {
        return new ArrayList<>(this.rightEntries);
    }

    @NotNull
    public List<MenuBarEntry> getEntries() {
        return ListUtils.mergeLists(this.leftEntries, this.rightEntries);
    }

    public int getBottomLineThickness() {
        return 1;
    }

    public boolean isHovered() {
        return this.hovered;
    }

    public boolean isUserNavigatingInMenuBar() {
        if (this.isHovered()) return true;
        for (MenuBarEntry e : ListUtils.mergeLists(this.leftEntries, this.rightEntries)) {
            if (e instanceof ContextMenuBarEntry c) {
                if (c.contextMenu.isUserNavigatingInMenu()) return true;
            }
        }
        return false;
    }

    public boolean isEntryContextMenuOpen() {
        for (MenuBarEntry e : this.getEntries()) {
            if (e instanceof ContextMenuBarEntry c) {
                if (c.contextMenu.isOpen()) return true;
            }
        }
        return false;
    }

    public MenuBar addClickListener(@NotNull MenuBarClickListener listener) {
        this.clickListeners.add(listener);
        return this;
    }

    public MenuBar removeClickListener(@NotNull MenuBarClickListener listener) {
        this.clickListeners.remove(listener);
        return this;
    }

    public MenuBar clearClickListeners() {
        this.clickListeners.clear();
        return this;
    }

    public MenuBar closeAllContextMenus() {
        for (MenuBarEntry e : this.getEntries()) {
            if (e instanceof ContextMenuBarEntry c) {
                c.contextMenu.closeMenu();
            }
        }
        return this;
    }

    public boolean isExpanded() {
        return this.expanded;
    }

    public MenuBar setExpanded(boolean expanded) {
        this.expanded = expanded;
        if (!this.expanded) this.closeAllContextMenus();
        this.markLayoutDirty();
        return this;
    }

    @Override
    public void setFocused(boolean var1) {
    }

    @Override
    public boolean isFocused() {
        return false;
    }

    @Override
    @NotNull
    public NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(@NotNull NarrationElementOutput var1) {
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float scale = getRenderScale();
        int scaledMouseX = (int) ((float)mouseX / scale);
        int scaledMouseY = (int) ((float)mouseY / scale);
        boolean entryClick = false;
        if (this.expanded) {
            for (MenuBarEntry e : ListUtils.mergeLists(this.leftEntries, this.rightEntries)) {
                if (e.isVisible()) {
                    if (e instanceof ContextMenuBarEntry c) {
                        if (c.contextMenu.mouseClicked(mouseX, mouseY, button)) entryClick = true;
                    }
                    if (e.mouseClicked(scaledMouseX, scaledMouseY, button)) entryClick = true;
                }
            }
        } else {
            if (this.collapseOrExpandEntry.mouseClicked(scaledMouseX, scaledMouseY, button)) entryClick = true;
        }
        if (this.isUserNavigatingInMenuBar() || entryClick) {
            fireClickListeners(button, PressState.PRESSED);
            this.clickActive = true;
            this.clickActiveButton = button;
            Screen current = Minecraft.getInstance().screen;
            if (current != null) {
                current.clearFocus();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.clickActive && this.clickActiveButton == button) {
            fireClickListeners(button, PressState.RELEASED);
            this.clickActive = false;
            this.clickActiveButton = -1;
        }
        return GuiEventListener.super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
        float scale = getRenderScale();
        int scaledMouseX = (int) ((float)mouseX / scale);
        int scaledMouseY = (int) ((float)mouseY / scale);
        boolean entryClick = false;
        if (this.expanded) {
            for (MenuBarEntry e : ListUtils.mergeLists(this.leftEntries, this.rightEntries)) {
                if (e.isVisible()) {
                    if (e.mouseScrolled(scaledMouseX, scaledMouseY, scrollDeltaX, scrollDeltaY)) return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (!this.expanded) return this.collapseOrExpandEntry.hovered;
        float scale = getRenderScale();
        int width = ScreenUtils.getScreenWidth();
        int scaledHeight = (MenuBar.HEIGHT != 0) ? (int)((float)MenuBar.HEIGHT * scale) : 0;
        return UIBase.isXYInArea((int)mouseX, (int)mouseY, 0, 0, width, scaledHeight);
    }

    @Override
    public boolean isFocusable() {
        return false;
    }

    @Override
    public void setFocusable(boolean focusable) {
        throw new RuntimeException("MenuBars are not focusable!");
    }

    @Override
    public boolean isNavigatable() {
        return false;
    }

    @Override
    public void setNavigatable(boolean navigatable) {
        throw new RuntimeException("ContextMenus are not navigatable!");
    }

    private void fireClickListeners(int button, @NotNull PressState state) {
        if (this.clickListeners.isEmpty()) {
            return;
        }
        for (MenuBarClickListener listener : new ArrayList<>(this.clickListeners)) {
            listener.onClick(button, state);
        }
    }

    @FunctionalInterface
    public interface MenuBarClickListener {
        void onClick(int button, @NotNull PressState state);
    }

    public static float getBaseScale() {
        return UIBase.getUIScale();
    }

    public static float getRenderScale() {
        return UIBase.calculateFixedScale(getBaseScale());
    }

    protected void markLayoutDirty() {
        this.layoutReady = false;
    }

    protected boolean shouldDeferContextMenuOpen() {
        return !this.layoutReady;
    }

    protected void queueContextMenuOpen(@NotNull ContextMenuBarEntry entry, @Nullable List<String> entryPath) {
        for (int i = 0; i < this.pendingContextMenuOpens.size(); i++) {
            PendingContextMenuOpen pending = this.pendingContextMenuOpens.get(i);
            if (pending.entry == entry) {
                this.pendingContextMenuOpens.set(i, new PendingContextMenuOpen(entry, entryPath));
                return;
            }
        }
        this.pendingContextMenuOpens.add(new PendingContextMenuOpen(entry, entryPath));
    }

    protected void flushPendingContextMenuOpens() {
        if (!this.layoutReady || this.pendingContextMenuOpens.isEmpty()) {
            return;
        }
        List<PendingContextMenuOpen> pending = new ArrayList<>(this.pendingContextMenuOpens);
        this.pendingContextMenuOpens.clear();
        for (PendingContextMenuOpen open : pending) {
            if (this.hasEntry(open.entry.identifier)) {
                open.entry.openContextMenuInternal(open.entryPath);
            }
        }
    }

    public static abstract class MenuBarEntry implements Renderable, GuiEventListener {

        protected final String identifier;
        @NotNull
        protected MenuBar parent;
        protected int x;
        protected int y;
        protected int height;
        protected boolean hovered = false;
        protected MenuBarEntryBooleanSupplier activeSupplier;
        protected MenuBarEntryBooleanSupplier visibleSupplier;
        @Nullable
        protected ConsumingSupplier<MenuBarEntry, UITooltip> tooltipSupplier;

        public MenuBarEntry(@NotNull String identifier, @NotNull MenuBar parent) {
            this.identifier = identifier;
            this.parent = parent;
        }

        @Override
        public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            this.renderEntry(graphics, mouseX, mouseY, partial);
            if (this.hovered && (this.tooltipSupplier != null)) {
                UITooltip tooltip = this.tooltipSupplier.get(this);
                if (tooltip != null) {
                    TooltipHandler.INSTANCE.addRenderTickTooltip(tooltip, () -> true);
                }
            }
        }

        protected abstract void renderEntry(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial);

        protected int getWidth() {
            return 20;
        }

        public boolean isHovered() {
            return this.hovered;
        }

        public boolean isActive() {
            return (this.activeSupplier == null) || this.activeSupplier.get(this.parent, this);
        }

        public MenuBarEntry setActive(boolean active) {
            this.activeSupplier = (menuBar, entry) -> active;
            return this;
        }

        public MenuBarEntry setActiveSupplier(MenuBarEntryBooleanSupplier activeSupplier) {
            this.activeSupplier = activeSupplier;
            return this;
        }

        public boolean isVisible() {
            return (this.visibleSupplier == null) || this.visibleSupplier.get(this.parent, this);
        }

        public MenuBarEntry setVisible(boolean visible) {
            this.visibleSupplier = (menuBar, entry) -> visible;
            return this;
        }

        public MenuBarEntry setVisibleSupplier(MenuBarEntryBooleanSupplier visibleSupplier) {
            this.visibleSupplier = visibleSupplier;
            return this;
        }

        public MenuBarEntry setTooltipSupplier(@Nullable ConsumingSupplier<MenuBarEntry, UITooltip> tooltipSupplier) {
            this.tooltipSupplier = tooltipSupplier;
            return this;
        }

        @NotNull
        public String getIdentifier() {
            return this.identifier;
        }

        @Override
        public void setFocused(boolean var1) {
        }

        @Override
        public boolean isFocused() {
            return false;
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return UIBase.isXYInArea((int) mouseX, (int) mouseY, this.x, this.y, this.getWidth(), HEIGHT);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return GuiEventListener.super.mouseClicked(mouseX, mouseY, button);
        }

        @FunctionalInterface
        public interface MenuBarEntryBooleanSupplier {
            boolean get(MenuBar bar, MenuBarEntry entry);
        }

        @FunctionalInterface
        public interface MenuBarEntrySupplier<T> {
            T get(MenuBar bar, MenuBarEntry entry);
        }

    }

    public static class ClickableMenuBarEntry extends MenuBarEntry {

        @NotNull
        protected MenuBarEntrySupplier<Component> labelSupplier;
        @Nullable
        protected MenuBarEntrySupplier<ITexture> iconTextureSupplier;
        @Nullable
        protected Supplier<DrawableColor> iconTextureColor = () -> UIBase.getUITheme().ui_icon_texture_color;
        @NotNull
        protected ClickAction clickAction;
        protected Font font = Minecraft.getInstance().font;

        public ClickableMenuBarEntry(@NotNull String identifier, @NotNull MenuBar menuBar, @NotNull Component label, @NotNull ClickAction clickAction) {
            super(identifier, menuBar);
            this.labelSupplier = (bar, entry) -> label;
            this.clickAction = clickAction;
        }

        @Override
        protected void renderEntry(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            this.renderBackground(graphics);
            this.renderLabelOrIcon(graphics);
        }

        protected void renderBackground(GuiGraphics graphics) {
            UIBase.resetShaderColor(graphics);
            graphics.fill(this.x, this.y, this.x + this.getWidth(), this.y + HEIGHT, this.getEntryBackgroundColor());
            UIBase.resetShaderColor(graphics);
        }

        protected void renderLabelOrIcon(GuiGraphics graphics) {
            RenderSystem.enableBlend();
            Component label = this.getLabel();
            ITexture iconTexture = this.getIconTexture();
            if (iconTexture != null) {
                int[] size = iconTexture.getAspectRatio().getAspectRatioSizeByMaximumSize(this.getWidth(), HEIGHT);
                UIBase.resetShaderColor(graphics);
                DrawableColor iconColor = (this.iconTextureColor != null) ? this.iconTextureColor.get() : null;
                if (iconColor != null) UIBase.setShaderColor(graphics, iconColor);
                ResourceLocation loc = (iconTexture.getResourceLocation() != null) ? iconTexture.getResourceLocation() : ITexture.MISSING_TEXTURE_LOCATION;
                graphics.blit(loc, this.x, this.y, 0.0F, 0.0F, size[0], size[1], size[0], size[1]);
            } else {
                UIBase.renderText(graphics, label, this.x + ENTRY_LABEL_SPACE_LEFT_RIGHT, this.y + ((float)HEIGHT / 2) - (UIBase.getUITextHeightNormal() / 2), this.getLabelColor());
            }
            UIBase.resetShaderColor(graphics);
        }

        protected int getLabelColor() {
            if (UIBase.shouldBlur()) {
                return this.isActive() ? UIBase.getUITheme().ui_blur_interface_widget_label_color_normal.getColorInt() : UIBase.getUITheme().ui_blur_interface_widget_label_color_inactive.getColorInt();
            }
            return this.isActive() ? UIBase.getUITheme().ui_interface_widget_label_color_normal.getColorInt() : UIBase.getUITheme().ui_interface_widget_label_color_inactive.getColorInt();
        }

        @Override
        protected int getWidth() {
            Component label = this.getLabel();
            ITexture iconTexture = this.getIconTexture();
            if (iconTexture != null) {
                return iconTexture.getAspectRatio().getAspectRatioWidth(HEIGHT);
            }
            return (int) (UIBase.getUITextWidthNormal(label) + (ENTRY_LABEL_SPACE_LEFT_RIGHT * 2));
        }

        @Override
        public ClickableMenuBarEntry setActive(boolean active) {
            return (ClickableMenuBarEntry) super.setActive(active);
        }

        @Override
        public ClickableMenuBarEntry setActiveSupplier(MenuBarEntryBooleanSupplier activeSupplier) {
            return (ClickableMenuBarEntry) super.setActiveSupplier(activeSupplier);
        }

        @Override
        public ClickableMenuBarEntry setVisible(boolean visible) {
            return (ClickableMenuBarEntry) super.setVisible(visible);
        }

        @Override
        public ClickableMenuBarEntry setVisibleSupplier(MenuBarEntryBooleanSupplier visibleSupplier) {
            return (ClickableMenuBarEntry) super.setVisibleSupplier(visibleSupplier);
        }

        public ClickableMenuBarEntry setIconTextureColor(@Nullable Supplier<DrawableColor> iconTextureColor) {
            this.iconTextureColor = iconTextureColor;
            return this;
        }

        protected int getEntryBackgroundColor() {
            if (this.isHovered() && this.isActive()) {
                if (UIBase.shouldBlur()) {
                    return UIBase.getUITheme().ui_blur_interface_widget_background_color_hover_type_1.getColorInt();
                }
                return UIBase.getUITheme().ui_interface_widget_background_color_hover_type_1.getColorInt();
            }
            return UIBase.getUITheme().ui_interface_widget_background_color_normal_type_1.getColorIntWithAlpha(UIBase.shouldBlur() ? 0.0F : 1.0F);
        }

        @NotNull
        protected Component getLabel() {
            Component c = this.labelSupplier.get(this.parent, this);
            return (c != null) ? c : Component.empty();
        }

        public ClickableMenuBarEntry setLabelSupplier(@NotNull MenuBarEntrySupplier<Component> labelSupplier) {
            this.labelSupplier = labelSupplier;
            return this;
        }

        public ClickableMenuBarEntry setLabel(@NotNull Component label) {
            this.labelSupplier = ((bar, entry) -> label);
            return this;
        }

        @Nullable
        protected ITexture getIconTexture() {
            if (this.iconTextureSupplier != null) return this.iconTextureSupplier.get(this.parent, this);
            return null;
        }

        @Nullable
        public MenuBarEntrySupplier<ITexture> getIconTextureSupplier() {
            return this.iconTextureSupplier;
        }

        public ClickableMenuBarEntry setIconTextureSupplier(@Nullable MenuBarEntrySupplier<ITexture> iconTextureSupplier) {
            this.iconTextureSupplier = iconTextureSupplier;
            return this;
        }

        public ClickableMenuBarEntry setIconTexture(@Nullable ITexture iconTexture) {
            this.iconTextureSupplier = (iconTexture != null) ? ((bar, entry) -> iconTexture) : null;
            return this;
        }

        @NotNull
        public ClickAction getClickAction() {
            return this.clickAction;
        }

        public ClickableMenuBarEntry setClickAction(@NotNull ClickAction clickAction) {
            this.clickAction = clickAction;
            return this;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if ((button == 0) && (this.isActive() && this.isVisible() && this.isHovered())) {
                if (FancyMenu.getOptions().playUiClickSounds.getValue()) {
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                }
                this.clickAction.onClick(this.parent, this);
                return true;
            }
            return false;
        }

        @FunctionalInterface
        public interface ClickAction {
            void onClick(MenuBar bar, MenuBarEntry entry);
        }

    }

    public static class ContextMenuBarEntry extends ClickableMenuBarEntry {

        protected ContextMenu contextMenu;

        public ContextMenuBarEntry(@NotNull String identifier, @NotNull MenuBar menuBar, @NotNull Component label, ContextMenu contextMenu) {
            super(identifier, menuBar, label, (bar, entry) -> {});
            this.contextMenu = contextMenu;
            contextMenu.setRoundedCorners(false, false, true, true);
            this.contextMenu.setShadow(false);
            this.contextMenu.setKeepDistanceToEdges(false);
            this.contextMenu.setForceUIScale(false);
            this.contextMenu.setForceRawXY(true);
            this.contextMenu.setForceSide(true);
            this.contextMenu.setForceSideSubMenus(false);
            for (ContextMenu.ContextMenuEntry<?> e : this.contextMenu.getEntries()) {
                if (e instanceof ContextMenu.SubMenuContextMenuEntry s) {
                    s.getSubContextMenu().setForceSide(true);
                    s.getSubContextMenu().setForceSideSubMenus(false);
                }
            }
            this.clickAction = (bar, entry) -> this.openContextMenu();
        }

        /**
         * Opens the {@link ContextMenu}.
         */
        public void openContextMenu() {
            this.openContextMenu(null);
        }

        /**
         * Opens the {@link ContextMenu}.
         *
         * @param entryPath The {@link ContextMenu.SubMenuContextMenuEntry} path of menus to open.
         */
        public void openContextMenu(@Nullable List<String> entryPath) {
            if (this.parent.shouldDeferContextMenuOpen()) {
                this.parent.queueContextMenuOpen(this, entryPath);
                return;
            }
            this.openContextMenuInternal(entryPath);
        }

        private void openContextMenuInternal(@Nullable List<String> entryPath) {
            this.contextMenu.setScale(getBaseScale());
            float scale = getRenderScale();
            float scaledX = (float)this.x * scale;
            float scaledY = (float)this.y * scale;
            float scaledHeight = (float)HEIGHT * scale;
            this.contextMenu.openMenuAt(scaledX, scaledY + scaledHeight - this.contextMenu.getScaledBorderThickness(), entryPath);
        }

        @Override
        public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            this.contextMenu.setScale(getBaseScale());
            this.handleOpenOnHover();
            super.render(graphics, mouseX, mouseY, partial);
        }

        protected void handleOpenOnHover() {
            if (this.isHovered() && this.isActive() && this.isVisible() && !this.contextMenu.isOpen() && this.parent.isEntryContextMenuOpen()) {
                this.parent.closeAllContextMenus();
                this.openContextMenu();
            }
        }

        public ContextMenu getContextMenu() {
            return this.contextMenu;
        }

        @Override
        public ContextMenuBarEntry setActive(boolean active) {
            return (ContextMenuBarEntry) super.setActive(active);
        }

        @Override
        public ContextMenuBarEntry setActiveSupplier(MenuBarEntryBooleanSupplier activeSupplier) {
            return (ContextMenuBarEntry) super.setActiveSupplier(activeSupplier);
        }

        @Override
        public ContextMenuBarEntry setVisible(boolean visible) {
            return (ContextMenuBarEntry) super.setVisible(visible);
        }

        @Override
        public ContextMenuBarEntry setVisibleSupplier(MenuBarEntryBooleanSupplier visibleSupplier) {
            return (ContextMenuBarEntry) super.setVisibleSupplier(visibleSupplier);
        }

        @Override
        public ContextMenuBarEntry setLabel(@NotNull Component label) {
            return (ContextMenuBarEntry) super.setLabel(label);
        }

        @Override
        public ContextMenuBarEntry setLabelSupplier(@NotNull MenuBarEntrySupplier<Component> labelSupplier) {
            return (ContextMenuBarEntry) super.setLabelSupplier(labelSupplier);
        }

        @Override
        public ContextMenuBarEntry setIconTexture(@Nullable ITexture iconTexture) {
            return (ContextMenuBarEntry) super.setIconTexture(iconTexture);
        }

        @Override
        public ContextMenuBarEntry setIconTextureSupplier(@Nullable MenuBarEntrySupplier<ITexture> iconTextureSupplier) {
            return (ContextMenuBarEntry) super.setIconTextureSupplier(iconTextureSupplier);
        }

        @Override
        public ContextMenuBarEntry setClickAction(@NotNull ClickAction clickAction) {
            LOGGER.error("[FANCYMENU] You can't change the click action of ContextMenuBarEntries!");
            return this;
        }

        @Override
        protected int getEntryBackgroundColor() {
            if (this.contextMenu.isOpen()) {
                if (UIBase.shouldBlur()) {
                    return UIBase.getUITheme().ui_blur_interface_widget_background_color_hover_type_1.getColorInt();
                }
                return UIBase.getUITheme().ui_interface_widget_background_color_hover_type_1.getColorInt();
            }
            return super.getEntryBackgroundColor();
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if ((!this.isHovered() || !this.isActive() || !this.isVisible()) && !this.contextMenu.isUserNavigatingInMenu() && this.contextMenu.isOpen()) {
                this.contextMenu.closeMenu();
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
            if (this.contextMenu.mouseScrolled(mouseX, mouseY, scrollDeltaX, scrollDeltaY)) {
                return true;
            }
            return super.mouseScrolled(mouseX, mouseY, scrollDeltaX, scrollDeltaY);
        }

    }

    public static class SpacerMenuBarEntry extends MenuBarEntry {

        protected int width = 10;

        public SpacerMenuBarEntry(@NotNull String identifier, @NotNull MenuBar menuBar) {
            super(identifier, menuBar);
        }

        @Override
        protected void renderEntry(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            RenderSystem.enableBlend();
            UIBase.resetShaderColor(graphics);
            this.renderBackground(graphics);
        }

        protected void renderBackground(GuiGraphics graphics) {
            graphics.fill(this.x, this.y, this.x + this.getWidth(), this.y + HEIGHT, UIBase.getUITheme().ui_interface_widget_background_color_normal_type_1.getColorIntWithAlpha(UIBase.shouldBlur() ? 0.0F : 1.0F));
            UIBase.resetShaderColor(graphics);
        }

        @Override
        protected int getWidth() {
            return this.width;
        }

        @Override
        public SpacerMenuBarEntry setActive(boolean active) {
            return (SpacerMenuBarEntry) super.setActive(active);
        }

        @Override
        public SpacerMenuBarEntry setActiveSupplier(MenuBarEntryBooleanSupplier activeSupplier) {
            return (SpacerMenuBarEntry) super.setActiveSupplier(activeSupplier);
        }

        @Override
        public SpacerMenuBarEntry setVisible(boolean visible) {
            return (SpacerMenuBarEntry) super.setVisible(visible);
        }

        @Override
        public SpacerMenuBarEntry setVisibleSupplier(MenuBarEntryBooleanSupplier visibleSupplier) {
            return (SpacerMenuBarEntry) super.setVisibleSupplier(visibleSupplier);
        }

        public SpacerMenuBarEntry setWidth(int width) {
            this.width = width;
            return this;
        }

    }

    public static class SeparatorMenuBarEntry extends MenuBarEntry {

        @NotNull
        protected Supplier<DrawableColor> color = () -> UIBase.shouldBlur() ? UIBase.getUITheme().ui_blur_overlay_border_color : UIBase.getUITheme().ui_interface_widget_border_color;

        public SeparatorMenuBarEntry(@NotNull String identifier, @NotNull MenuBar parent) {
            super(identifier, parent);
        }

        @Override
        protected void renderEntry(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            RenderSystem.enableBlend();
            UIBase.resetShaderColor(graphics);
            graphics.fill(this.x, this.y, this.x + this.getWidth(), this.y + HEIGHT, this.getColor().getColorInt());
            UIBase.resetShaderColor(graphics);
        }

        @Override
        protected int getWidth() {
            return 1;
        }

        @Override
        public SeparatorMenuBarEntry setActive(boolean active) {
            return (SeparatorMenuBarEntry) super.setActive(active);
        }

        @Override
        public SeparatorMenuBarEntry setActiveSupplier(MenuBarEntryBooleanSupplier activeSupplier) {
            return (SeparatorMenuBarEntry) super.setActiveSupplier(activeSupplier);
        }

        @Override
        public SeparatorMenuBarEntry setVisible(boolean visible) {
            return (SeparatorMenuBarEntry) super.setVisible(visible);
        }

        @Override
        public SeparatorMenuBarEntry setVisibleSupplier(MenuBarEntryBooleanSupplier visibleSupplier) {
            return (SeparatorMenuBarEntry) super.setVisibleSupplier(visibleSupplier);
        }

        @NotNull
        public DrawableColor getColor() {
            return this.color.get();
        }

        public SeparatorMenuBarEntry setColor(@NotNull Supplier<DrawableColor> color) {
            this.color = color;
            return this;
        }

    }

    public enum Side {
        LEFT,
        RIGHT
    }

    protected static final class PendingContextMenuOpen {

        private final ContextMenuBarEntry entry;
        @Nullable
        private final List<String> entryPath;

        private PendingContextMenuOpen(@NotNull ContextMenuBarEntry entry, @Nullable List<String> entryPath) {
            this.entry = entry;
            this.entryPath = entryPath;
        }
    }

}
