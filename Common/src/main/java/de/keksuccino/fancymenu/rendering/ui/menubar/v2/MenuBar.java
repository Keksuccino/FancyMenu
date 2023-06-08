package de.keksuccino.fancymenu.rendering.ui.menubar.v2;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.rendering.DrawableColor;
import de.keksuccino.fancymenu.rendering.ui.UIBase;
import de.keksuccino.fancymenu.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.utils.ListUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("unused")
public class MenuBar extends GuiComponent implements Renderable, GuiEventListener, NarratableEntry {

    private static final Logger LOGGER = LogManager.getLogger();

    protected final List<MenuBarEntry> leftEntries = new ArrayList<>();
    protected final List<MenuBarEntry> rightEntries = new ArrayList<>();

    protected int height = 20;
    protected float scale = UIBase.getUIScale();
    protected boolean forceUIScale = true;
    protected int bottomLineY;

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

        if (this.forceUIScale) this.scale = UIBase.getUIScale();

        float scale = UIBase.calculateFixedScale(this.scale);
        int scaledMouseX = (int) ((float)mouseX / scale);
        int scaledMouseY = (int) ((float)mouseY / scale);
        int y = 0;
        int width = (Minecraft.getInstance().screen != null) ? Minecraft.getInstance().screen.width : 0;
        int scaledWidth = (width != 0) ? (int)((float)width / scale) : 0;

        RenderSystem.enableBlend();
        UIBase.resetShaderColor();

        pose.pushPose();
        pose.scale(scale, scale, scale);

        this.renderBackground(pose, scaledWidth, this.height);

        //Render all visible entries
        int leftX = 0;
        for (MenuBarEntry e : this.leftEntries) {
            e.x = (int)((float)leftX / scale);
            e.y = y;
            e.height = this.height;
            e.hovered = e.isMouseOver(scaledMouseX, scaledMouseY);
            if (e.isVisible()) {
                e.render(pose, scaledMouseX, scaledMouseY, partial);
            }
            leftX += (int)((float)e.getWidth() * scale);
        }
        int rightX = scaledWidth;
        for (MenuBarEntry e : this.rightEntries) {
            e.x = (int)((float)rightX / scale) - e.getWidth();
            e.y = y;
            e.height = this.height;
            e.hovered = e.isMouseOver(scaledMouseX, scaledMouseY);
            if (e.isVisible()) {
                e.render(pose, scaledMouseX, scaledMouseY, partial);
            }
            rightX -= (int)((float)e.getWidth() / scale);
        }

        this.renderBottomLine(pose, scaledWidth, this.height);

        //Render context menus of ContextMenuBarEntries
        for (MenuBarEntry e : ListUtils.mergeLists(this.leftEntries, this.rightEntries)) {
            if (e instanceof ContextMenuBarEntry c) {
                c.contextMenu.setOverriddenRenderScale((float) Minecraft.getInstance().getWindow().getGuiScale());
                c.contextMenu.render(pose, scaledMouseX, scaledMouseY, partial);
            }
        }

        pose.popPose();

        UIBase.resetShaderColor();

    }

    protected void renderBackground(PoseStack pose, int width, int height) {
        fill(pose, 0, 0, width, height, UIBase.getUIColorScheme().elementBackgroundColorNormal.getColorInt());
        UIBase.resetShaderColor();
    }

    protected void renderBottomLine(PoseStack pose, int width, int height) {
        this.bottomLineY = height - this.getBottomLineThickness();
        fill(pose, 0, this.bottomLineY, width, height, UIBase.getUIColorScheme().elementBorderColorNormal.getColorInt());
        UIBase.resetShaderColor();
    }

    @NotNull
    public SeparatorMenuBarEntry addSeparatorEntry(@NotNull Side side, @NotNull String identifier) {
        return (SeparatorMenuBarEntry) this.addEntry(side, new SeparatorMenuBarEntry(identifier, this));
    }

    @NotNull
    public ContextMenuBarEntry addContextMenuEntry(@NotNull Side side, @NotNull String identifier, @NotNull Component label, @NotNull ContextMenu contextMenu) {
        return (ContextMenuBarEntry) this.addEntry(side, new ContextMenuBarEntry(identifier, this, label, contextMenu));
    }

    @NotNull
    public ClickableMenuBarEntry addClickableEntry(@NotNull Side side, @NotNull String identifier, @NotNull Component label, @NotNull ClickableMenuBarEntry.ClickAction clickAction) {
        return (ClickableMenuBarEntry) this.addEntry(side, new ClickableMenuBarEntry(identifier, this, label, clickAction));
    }

    @NotNull
    public MenuBarEntry addEntry(@NotNull Side side, @NotNull MenuBarEntry entry) {
        Objects.requireNonNull(side);
        Objects.requireNonNull(entry);
        Objects.requireNonNull(entry.identifier);
        if (side == Side.LEFT) {
            this.leftEntries.add(entry);
        }
        if (side == Side.RIGHT) {
            this.rightEntries.add(entry);
        }
        return entry;
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

    public boolean isForceUIScale() {
        return this.forceUIScale;
    }

    public MenuBar setForceUIScale(boolean forceUIScale) {
        this.forceUIScale = forceUIScale;
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
        return GuiEventListener.super.mouseClicked(mouseX, mouseY, button);
    }

    public static abstract class MenuBarEntry extends GuiComponent implements Renderable, GuiEventListener {

        protected final String identifier;
        @NotNull
        protected MenuBar parent;
        protected int x;
        protected int y;
        protected int height;
        protected boolean hovered = false;
        protected BooleanSupplier activeSupplier;
        protected BooleanSupplier visibleSupplier;

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

        public MenuBarEntry setActiveSupplier(BooleanSupplier activeSupplier) {
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

        public MenuBarEntry setVisibleSupplier(BooleanSupplier visibleSupplier) {
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
        public interface BooleanSupplier {
            boolean get(MenuBar bar, MenuBarEntry entry);
        }

    }

    public static class ClickableMenuBarEntry extends MenuBarEntry {

        @NotNull
        protected Component label;
        @NotNull
        protected ClickAction clickAction;
        protected Font font = Minecraft.getInstance().font;

        public ClickableMenuBarEntry(@NotNull String identifier, @NotNull MenuBar menuBar, @NotNull Component label, @NotNull ClickAction clickAction) {
            super(identifier, menuBar);
            this.label = label;
            this.clickAction = clickAction;
        }

        @Override
        public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
            RenderSystem.enableBlend();
            UIBase.resetShaderColor();
            this.renderBackground(pose);
            this.renderLabel(pose);
        }

        protected void renderBackground(PoseStack pose) {
            fill(pose, this.x, this.y, this.x + this.getWidth(), this.y + this.height, this.getBackgroundColor().getColorInt());
            UIBase.resetShaderColor();
        }

        protected void renderLabel(PoseStack pose) {
            UIBase.drawText(pose, this.font, this.label, this.x + 5, this.y + (this.height / 2) - (this.font.lineHeight / 2), this.isActive() ? UIBase.getUIColorScheme().elementLabelColorNormal.getColorInt() : UIBase.getUIColorScheme().elementLabelColorInactive.getColorInt());
            UIBase.resetShaderColor();
        }

        @Override
        protected int getWidth() {
            return this.font.width(this.label) + 10;
        }

        @NotNull
        protected DrawableColor getBackgroundColor() {
            if (this.isHovered() && this.isActive()) return UIBase.getUIColorScheme().elementBackgroundColorHover;
            return UIBase.getUIColorScheme().elementBackgroundColorNormal;
        }

        @NotNull
        public Component getLabel() {
            return this.label;
        }

        public ClickableMenuBarEntry setLabel(@NotNull Component label) {
            this.label = label;
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
            this.contextMenu.setMinXYEnabled(false);
            this.contextMenu.setForceUIScale(false);
            this.clickAction = (bar, entry) -> this.openContextMenu();
        }

        protected void openContextMenu() {
            this.contextMenu.setScale(this.parent.scale);
            this.contextMenu.openMenuAt(this.x, this.y + this.height - 1);
        }

        @Override
        public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
            this.contextMenu.setScale(this.parent.scale);
            super.render(pose, mouseX, mouseY, partial);
        }

        @Override
        public ClickableMenuBarEntry setClickAction(@NotNull ClickAction clickAction) {
            LOGGER.error("[FANCYMENU] You can't change the click action of ContextMenuBarEntries!");
            return this;
        }

        @Override
        protected @NotNull DrawableColor getBackgroundColor() {
            if (this.contextMenu.isOpen()) return UIBase.getUIColorScheme().elementBackgroundColorHover;
            return super.getBackgroundColor();
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if ((button == 0) && (!this.isHovered() || !this.isActive() || !this.isVisible()) && !this.contextMenu.isUserNavigatingInMenu()) {
                this.contextMenu.closeMenu();
            }
//            if ((button == 0) && (this.isActive() && this.isVisible() && this.isHovered())) {
//                this.contextMenu.openMenuAt()
//                return true;
//            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

    }

    public static class SeparatorMenuBarEntry extends MenuBarEntry {

        @NotNull
        protected DrawableColor color = UIBase.getUIColorScheme().elementBorderColorNormal;

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

        @NotNull
        public DrawableColor getColor() {
            return this.color;
        }

        public void setColor(@NotNull DrawableColor color) {
            this.color = color;
        }

    }

    public enum Side {
        LEFT,
        RIGHT
    }

}
