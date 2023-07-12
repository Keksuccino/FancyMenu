package de.keksuccino.fancymenu.util.rendering.ui.menubar.v2;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.resources.texture.ITexture;
import de.keksuccino.fancymenu.util.ListUtils;
import de.keksuccino.fancymenu.util.ScreenUtils;
import de.keksuccino.konkrete.rendering.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class MenuBar extends GuiComponent implements Renderable, GuiEventListener, NarratableEntry {

    private static final Logger LOGGER = LogManager.getLogger();

    protected final List<MenuBarEntry> leftEntries = new ArrayList<>();
    protected final List<MenuBarEntry> rightEntries = new ArrayList<>();

    protected int height = 28;
    protected float scale = UIBase.getUIScale();
    protected boolean hovered = false;
    protected boolean forceUIScale = true;

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

        if (this.forceUIScale) this.scale = UIBase.getUIScale();

        this.hovered = this.isMouseOver(mouseX, mouseY);

        float scale = UIBase.calculateFixedScale(this.scale);
        int scaledMouseX = (int) ((float)mouseX / scale);
        int scaledMouseY = (int) ((float)mouseY / scale);
        int y = 0;
        int width = ScreenUtils.getScreenWidth();
        int scaledWidth = (width != 0) ? (int)((float)width / scale) : 0;

        RenderSystem.enableBlend();
        UIBase.resetShaderColor();

        pose.pushPose();
        pose.scale(scale, scale, scale);

        this.renderBackground(pose, scaledWidth, this.height);

        //Render all visible entries
        int leftX = 0;
        for (MenuBarEntry e : this.leftEntries) {
            e.x = leftX;
            e.y = y;
            e.height = this.height;
            e.hovered = e.isMouseOver(scaledMouseX, scaledMouseY);
            if (e.isVisible()) {
                RenderSystem.enableBlend();
                UIBase.resetShaderColor();
                e.render(pose, scaledMouseX, scaledMouseY, partial);
            }
            leftX += e.getWidth();
        }
        int rightX = scaledWidth;
        for (MenuBarEntry e : this.rightEntries) {
            e.x = rightX - e.getWidth();
            e.y = y;
            e.height = this.height;
            e.hovered = e.isMouseOver(scaledMouseX, scaledMouseY);
            if (e.isVisible()) {
                RenderSystem.enableBlend();
                UIBase.resetShaderColor();
                e.render(pose, scaledMouseX, scaledMouseY, partial);
            }
            rightX -= e.getWidth();
        }

        this.renderBottomLine(pose, scaledWidth, this.height);

        pose.popPose();
        UIBase.resetShaderColor();

        //Render context menus of ContextMenuBarEntries
        for (MenuBarEntry e : ListUtils.mergeLists(this.leftEntries, this.rightEntries)) {
            if (e instanceof ContextMenuBarEntry c) {
                c.contextMenu.render(pose, mouseX, mouseY, partial);
            }
        }

        UIBase.resetShaderColor();

    }

    protected void renderBackground(PoseStack pose, int width, int height) {
        fill(pose, 0, 0, width, height, UIBase.getUIColorScheme().element_background_color_normal.getColorInt());
        UIBase.resetShaderColor();
    }

    protected void renderBottomLine(PoseStack pose, int width, int height) {
        fill(pose, 0, height - this.getBottomLineThickness(), width, height, UIBase.getUIColorScheme().menu_bar_bottom_line_color.getColorInt());
        UIBase.resetShaderColor();
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
        }
        return entry;
    }

    public MenuBar removeEntry(@NotNull String identifier) {
        MenuBarEntry e = this.getEntry(identifier);
        if (e != null) {
            this.leftEntries.remove(e);
            this.rightEntries.remove(e);
        }
        return this;
    }

    public MenuBar clearLeftEntries() {
        this.leftEntries.clear();
        return this;
    }

    public MenuBar clearRightEntries() {
        this.rightEntries.clear();
        return this;
    }

    public MenuBar clearEntries() {
        this.leftEntries.clear();
        this.rightEntries.clear();
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

    public int getHeight() {
        return this.height;
    }

    public MenuBar setHeight(int height) {
        this.height = height;
        return this;
    }

    public int getBottomLineThickness() {
        return 1;
    }

    public float getScale() {
        return this.scale;
    }

    public MenuBar setScale(float scale) {
        if (this.forceUIScale) LOGGER.error("[FANCYMENU] Unable to set scale of MenuBar while MenuBar#isForceUIScale()!");
        this.scale = scale;
        return this;
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

    public boolean isForceUIScale() {
        return this.forceUIScale;
    }

    public MenuBar setForceUIScale(boolean forceUIScale) {
        this.forceUIScale = forceUIScale;
        return this;
    }

    public boolean isEntryContextMenuOpen() {
        for (MenuBarEntry e : this.getEntries()) {
            if (e instanceof ContextMenuBarEntry c) {
                if (c.contextMenu.isOpen()) return true;
            }
        }
        return false;
    }

    public MenuBar closeAllContextMenus() {
        for (MenuBarEntry e : this.getEntries()) {
            if (e instanceof ContextMenuBarEntry c) {
                c.contextMenu.closeMenu();
            }
        }
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
        float scale = UIBase.calculateFixedScale(this.scale);
        int scaledMouseX = (int) ((float)mouseX / scale);
        int scaledMouseY = (int) ((float)mouseY / scale);
        for (MenuBarEntry e : ListUtils.mergeLists(this.leftEntries, this.rightEntries)) {
            if (e instanceof ContextMenuBarEntry c) {
                c.contextMenu.mouseClicked(mouseX, mouseY, button);
            }
            if (e.isVisible()) e.mouseClicked(scaledMouseX, scaledMouseY, button);
        }
        if (this.isUserNavigatingInMenuBar()) return true;
        return GuiEventListener.super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        float scale = UIBase.calculateFixedScale(this.scale);
        int width = ScreenUtils.getScreenWidth();
        int scaledHeight = (this.getHeight() != 0) ? (int)((float)this.getHeight() * scale) : 0;
        return UIBase.isXYInArea((int)mouseX, (int)mouseY, 0, 0, width, scaledHeight);
    }

    public static abstract class MenuBarEntry extends GuiComponent implements Renderable, GuiEventListener {

        protected final String identifier;
        @NotNull
        protected MenuBar parent;
        protected int x;
        protected int y;
        protected int height;
        protected boolean hovered = false;
        protected MenuBarEntryBooleanSupplier activeSupplier;
        protected MenuBarEntryBooleanSupplier visibleSupplier;

        public MenuBarEntry(@NotNull String identifier, @NotNull MenuBar parent) {
            this.identifier = identifier;
            this.parent = parent;
        }

        @Override
        public abstract void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial);

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
            return UIBase.isXYInArea((int) mouseX, (int) mouseY, this.x, this.y, this.getWidth(), this.height);
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
        protected Supplier<DrawableColor> iconTextureColor = () -> UIBase.getUIColorScheme().ui_texture_color;
        @NotNull
        protected ClickAction clickAction;
        protected Font font = Minecraft.getInstance().font;

        public ClickableMenuBarEntry(@NotNull String identifier, @NotNull MenuBar menuBar, @NotNull Component label, @NotNull ClickAction clickAction) {
            super(identifier, menuBar);
            this.labelSupplier = (bar, entry) -> label;
            this.clickAction = clickAction;
        }

        @Override
        public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
            this.renderBackground(pose);
            this.renderLabelOrIcon(pose);
        }

        protected void renderBackground(PoseStack pose) {
            UIBase.resetShaderColor();
            fill(pose, this.x, this.y, this.x + this.getWidth(), this.y + this.height, this.getBackgroundColor().getColorInt());
            UIBase.resetShaderColor();
        }

        protected void renderLabelOrIcon(PoseStack pose) {
            RenderSystem.enableBlend();
            Component label = this.getLabel();
            ITexture iconTexture = this.getIconTexture();
            if (iconTexture != null) {
                int[] size = iconTexture.getAspectRatio().getAspectRatioSizeByMaximumSize(this.getWidth(), this.height);
                UIBase.resetShaderColor();
                DrawableColor iconColor = (this.iconTextureColor != null) ? this.iconTextureColor.get() : null;
                if (iconColor != null) UIBase.setShaderColor(iconColor);
                RenderUtils.bindTexture((iconTexture.getResourceLocation() != null) ? iconTexture.getResourceLocation() : ITexture.MISSING_TEXTURE_LOCATION);
                blit(pose, this.x, this.y, 0.0F, 0.0F, size[0], size[1], size[0], size[1]);
            } else {
                UIBase.drawElementLabel(pose, this.font, label, this.x + 5, this.y + (this.height / 2) - (this.font.lineHeight / 2), this.isActive() ? UIBase.getUIColorScheme().element_label_color_normal.getColorInt() : UIBase.getUIColorScheme().element_label_color_inactive.getColorInt());
            }
            UIBase.resetShaderColor();
        }

        @Override
        protected int getWidth() {
            Component label = this.getLabel();
            ITexture iconTexture = this.getIconTexture();
            if (iconTexture != null) {
                return iconTexture.getAspectRatio().getAspectRatioWidth(this.height);
            }
            return this.font.width(label) + 10;
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

        @NotNull
        protected DrawableColor getBackgroundColor() {
            if (this.isHovered() && this.isActive()) return UIBase.getUIColorScheme().element_background_color_hover;
            return UIBase.getUIColorScheme().element_background_color_normal;
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

        public void openContextMenu() {
            this.contextMenu.setScale(this.parent.scale);
            float scale = UIBase.calculateFixedScale(this.parent.scale);
            float scaledX = (float)this.x * scale;
            float scaledY = (float)this.y * scale;
            float scaledHeight = (float)this.height * scale;
            this.contextMenu.openMenuAt(scaledX, scaledY + scaledHeight - this.contextMenu.getScaledBorderThickness());
        }

        @Override
        public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
            this.contextMenu.setScale(this.parent.scale);
            this.handleOpenOnHover();
            super.render(pose, mouseX, mouseY, partial);
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
        protected @NotNull DrawableColor getBackgroundColor() {
            if (this.contextMenu.isOpen()) return UIBase.getUIColorScheme().element_background_color_hover;
            return super.getBackgroundColor();
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if ((!this.isHovered() || !this.isActive() || !this.isVisible()) && !this.contextMenu.isUserNavigatingInMenu()) {
                this.contextMenu.closeMenu();
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

    }

    public static class SpacerMenuBarEntry extends MenuBarEntry {

        protected int width = 10;

        public SpacerMenuBarEntry(@NotNull String identifier, @NotNull MenuBar menuBar) {
            super(identifier, menuBar);
        }

        @Override
        public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
            RenderSystem.enableBlend();
            UIBase.resetShaderColor();
            this.renderBackground(pose);
        }

        protected void renderBackground(PoseStack pose) {
            fill(pose, this.x, this.y, this.x + this.getWidth(), this.y + this.height, UIBase.getUIColorScheme().element_background_color_normal.getColorInt());
            UIBase.resetShaderColor();
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
        protected DrawableColor color = UIBase.getUIColorScheme().element_border_color_normal;

        public SeparatorMenuBarEntry(@NotNull String identifier, @NotNull MenuBar parent) {
            super(identifier, parent);
        }

        @Override
        public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
            RenderSystem.enableBlend();
            UIBase.resetShaderColor();
            fill(pose, this.x, this.y, this.x + this.getWidth(), this.y + this.height, color.getColorInt());
            UIBase.resetShaderColor();
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
            return this.color;
        }

        public SeparatorMenuBarEntry setColor(@NotNull DrawableColor color) {
            this.color = color;
            return this;
        }

    }

    public enum Side {
        LEFT,
        RIGHT
    }

}
