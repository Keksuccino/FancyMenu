package de.keksuccino.fancymenu.rendering.ui.menubar.v2;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.rendering.DrawableColor;
import de.keksuccino.fancymenu.rendering.ui.UIBase;
import de.keksuccino.fancymenu.rendering.ui.contextmenu.v2.ContextMenu;
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

//TODO hier weiter machen !!!!!!!!!!
//TODO hier weiter machen !!!!!!!!!!
//TODO hier weiter machen !!!!!!!!!!
//TODO hier weiter machen !!!!!!!!!!
//TODO hier weiter machen !!!!!!!!!!

@SuppressWarnings("unused")
public class MenuBar extends GuiComponent implements Renderable, GuiEventListener, NarratableEntry {

    private static final Logger LOGGER = LogManager.getLogger();

    protected final List<MenuBarEntry> leftEntries = new ArrayList<>();
    protected final List<MenuBarEntry> rightEntries = new ArrayList<>();

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

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
        for (MenuBarEntry e : this.leftEntries) {
            e.mouseClicked(mouseX, mouseY, button);
        }
        for (MenuBarEntry e : this.rightEntries) {
            e.mouseClicked(mouseX, mouseY, button);
        }
        return GuiEventListener.super.mouseClicked(mouseX, mouseY, button);
    }

    public static abstract class MenuBarEntry extends GuiComponent implements Renderable, GuiEventListener {

        @NotNull
        protected MenuBar parent;
        protected int x;
        protected int y;
        protected boolean hovered = false;
        protected boolean active = true;
        protected boolean visible = true;

        public MenuBarEntry(@NotNull MenuBar parent) {
            this.parent = parent;
        }

        @Override
        public abstract void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial);

        protected int getWidth() {
            return 20;
        }

        protected int getHeight() {
            return 20;
        }

        public boolean isHovered() {
            return this.hovered;
        }

        public boolean isActive() {
            return this.active;
        }

        public MenuBarEntry setActive(boolean active) {
            this.active = active;
            return this;
        }

        public boolean isVisible() {
            return this.visible;
        }

        public MenuBarEntry setVisible(boolean visible) {
            this.visible = visible;
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
        public boolean isMouseOver(double mouseX, double mouseY) {
            return UIBase.isXYInArea((int) mouseX, (int) mouseY, this.x, this.y, this.getWidth(), this.getHeight());
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return GuiEventListener.super.mouseClicked(mouseX, mouseY, button);
        }

    }

    public static class ClickableMenuBarEntry extends MenuBarEntry {

        @NotNull
        protected Component label;
        @NotNull
        protected ClickAction clickAction;
        protected Font font = Minecraft.getInstance().font;

        public ClickableMenuBarEntry(@NotNull MenuBar menuBar, @NotNull Component label, @NotNull ClickAction clickAction) {
            super(menuBar);
            this.label = label;
            this.clickAction = clickAction;
        }

        @Override
        public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
            this.hovered = this.isMouseOver(mouseX, mouseY);
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            this.renderBackground(pose);
            this.renderLabel(pose);
        }

        protected void renderBackground(PoseStack pose) {
            fill(pose, this.x, this.y, this.x + this.getWidth(), this.y + this.getHeight(), this.getBackgroundColor().getColorInt());
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }

        protected void renderLabel(PoseStack pose) {
            this.font.draw(pose, this.label, this.x + 5, this.y + (this.font.width(this.label) / 2F) - (this.font.lineHeight / 2F), this.isActive() ? UIBase.getUIColorScheme().elementLabelColorNormal.getColorInt() : UIBase.getUIColorScheme().elementLabelColorInactive.getColorInt());
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
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

        public ContextMenuBarEntry(@NotNull MenuBar menuBar, @NotNull Component label, ContextMenu contextMenu) {
            super(menuBar, label, (bar, entry) -> {});
            this.contextMenu = contextMenu;
            this.clickAction = (bar, entry) -> {
                this.contextMenu.openMenuAt(this.x, this.y + this.getHeight() - 1);
            };
        }

        @Override
        public ClickableMenuBarEntry setClickAction(@NotNull ClickAction clickAction) {
            LOGGER.error("[FANCYMENU] You can't change the click action of ContextMenuBarEntries!");
            return this;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if ((button == 0) && (!this.isHovered() || !this.isActive() || !this.isVisible())) {
                this.contextMenu.closeMenu();
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

    }

    public static class SeparatorMenuBarEntry extends MenuBarEntry {

        @NotNull
        protected DrawableColor color = UIBase.getUIColorScheme().elementBorderColorNormal;

        public SeparatorMenuBarEntry(@NotNull MenuBar parent) {
            super(parent);
        }

        @Override
        public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            fill(pose, this.x, this.y, this.x + this.getWidth(), this.y + this.getHeight(), color.getColorInt());
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
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

}
