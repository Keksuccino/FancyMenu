package de.keksuccino.fancymenu.rendering.ui.contextmenu.v2;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.rendering.ui.UIBase;
import de.keksuccino.fancymenu.rendering.ui.tooltip.Tooltip;
import de.keksuccino.konkrete.input.MouseInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("unused")
public class ContextMenu extends GuiComponent implements Renderable, GuiEventListener, NarratableEntry {


    //TODO Pfeil an rechter Seite von sub menu entry


    private static final Logger LOGGER = LogManager.getLogger();

    private static final Color MENU_SHADOW_COLOR = new Color(43, 43, 43, 100);
    private static final Color MENU_BACKGROUND_COLOR = UIBase.ELEMENT_BACKGROUND_COLOR_IDLE;

    protected final List<ContextMenuEntry> entries = new ArrayList<>();
    protected float scale = UIBase.getUiScale();
    protected boolean open = false;
    protected int rawX; // without border
    protected int rawY; // without border
    protected int rawWidth; // without border
    protected int rawHeight; // without border
    protected SubMenuContextMenuEntry parentEntry = null;
    protected SubMenuOpeningSide subMenuOpeningSide = SubMenuOpeningSide.RIGHT;

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

        if (!this.isOpen()) return;

        float scale = UIBase.calculateFixedScale(this.getScale());

        RenderSystem.enableBlend();
        pose.pushPose();
        pose.scale(scale, scale, scale);
        pose.translate(0.0F, 0.0F, 400.0F);

        this.rawWidth = 20;
        this.rawHeight = 0;
        for (ContextMenuEntry e : this.entries) {
            //Pre-tick
            if (e.ticker != null) {
                e.ticker.onTick(this, e);
            }
            //Update width + height
            int w = e.getMinWidth();
            if (w > this.rawWidth) {
                this.rawWidth = w;
            }
            this.rawHeight += e.getHeight();
        }

        int x = this.getActualX();
        int y = this.getActualY();
        int scaledX = (int)((float)x/scale);
        int scaledY = (int)((float)y/scale);
        int scaledMouseX = (int) ((float)mouseX / scale);
        int scaledMouseY = (int) ((float)mouseY / scale);
        boolean navigatingInSub = this.isUserNavigatingInSubMenu();

        //Render shadow
        fill(pose, scaledX + 4, scaledY + 4, scaledX + this.getWidth() + 4, scaledY + this.getHeight() + 4, MENU_SHADOW_COLOR.getRGB());
        //Render background
        fill(pose, scaledX, scaledY, scaledX + this.getWidth(), scaledY + this.getHeight(), MENU_BACKGROUND_COLOR.getRGB());
        //Update + render entries
        int entryY = scaledY;
        for (ContextMenuEntry e : this.entries) {
            e.x = scaledX;
            e.y = entryY; //already scaled
            e.width = this.rawWidth; //don't scale, because already scaled via pose.scale()
            e.setHovered(!navigatingInSub && UIBase.isMouseInArea(scaledMouseX, scaledMouseY, e.x, e.y, e.width, e.getHeight()));
            e.render(pose, scaledMouseX, scaledMouseY, partial);
            entryY += e.getHeight(); //don't scale this, because already scaled via pose.scale()
        }
        //Render border
        UIBase.renderBorder(pose, scaledX - 1, scaledY - 1, scaledX + this.rawWidth + 1, scaledY + this.rawHeight + 1, 1, UIBase.ELEMENT_BORDER_COLOR_IDLE, true, true, true, true);

        //Post-tick
        for (ContextMenuEntry e : this.entries) {
            if (e.ticker != null) {
                e.ticker.onTick(this, e);
            }
        }

        pose.popPose();

        //Render sub context menus
        for (ContextMenuEntry e : this.entries) {
            if (e instanceof SubMenuContextMenuEntry s) {
                s.subContextMenu.render(pose, mouseX, mouseY, partial);
            }
        }

    }

    @NotNull
    public SeparatorContextMenuEntry addSeparatorEntryAt(int index, @NotNull String identifier) {
        SeparatorContextMenuEntry e = new SeparatorContextMenuEntry(identifier, this);
        return (SeparatorContextMenuEntry) this.addEntryAt(index, e);
    }

    @NotNull
    public SeparatorContextMenuEntry addSeparatorEntryBefore(@NotNull String addBeforeIdentifier, @NotNull String identifier) {
        SeparatorContextMenuEntry e = new SeparatorContextMenuEntry(identifier, this);
        return (SeparatorContextMenuEntry) this.addEntryBefore(addBeforeIdentifier, e);
    }

    @NotNull
    public SeparatorContextMenuEntry addSeparatorEntryAfter(@NotNull String addAfterIdentifier, @NotNull String identifier) {
        SeparatorContextMenuEntry e = new SeparatorContextMenuEntry(identifier, this);
        return (SeparatorContextMenuEntry) this.addEntryAfter(addAfterIdentifier, e);
    }

    @NotNull
    public SeparatorContextMenuEntry addSeparatorEntry(@NotNull String identifier) {
        SeparatorContextMenuEntry e = new SeparatorContextMenuEntry(identifier, this);
        return (SeparatorContextMenuEntry) this.addEntry(e);
    }

    @NotNull
    public ClickableContextMenuEntry addClickableEntryAt(int index, @NotNull String identifier, @NotNull Component label, @NotNull ClickableContextMenuEntry.ClickAction clickAction) {
        ClickableContextMenuEntry e = new ClickableContextMenuEntry(identifier, this, label, clickAction);
        return (ClickableContextMenuEntry) this.addEntryAt(index, e);
    }

    @NotNull
    public ClickableContextMenuEntry addClickableEntryAfter(@NotNull String addAfterIdentifier, @NotNull String identifier, @NotNull Component label, @NotNull ClickableContextMenuEntry.ClickAction clickAction) {
        ClickableContextMenuEntry e = new ClickableContextMenuEntry(identifier, this, label, clickAction);
        return (ClickableContextMenuEntry) this.addEntryAfter(addAfterIdentifier, e);
    }

    @NotNull
    public ClickableContextMenuEntry addClickableEntryBefore(@NotNull String addBeforeIdentifier, @NotNull String identifier, @NotNull Component label, @NotNull ClickableContextMenuEntry.ClickAction clickAction) {
        ClickableContextMenuEntry e = new ClickableContextMenuEntry(identifier, this, label, clickAction);
        return (ClickableContextMenuEntry) this.addEntryBefore(addBeforeIdentifier, e);
    }

    @NotNull
    public ClickableContextMenuEntry addClickableEntry(@NotNull String identifier, @NotNull Component label, @NotNull ClickableContextMenuEntry.ClickAction clickAction) {
        ClickableContextMenuEntry e = new ClickableContextMenuEntry(identifier, this, label, clickAction);
        return (ClickableContextMenuEntry) this.addEntry(e);
    }

    public ContextMenuEntry addEntryAfter(@NotNull String identifier, @NotNull ContextMenuEntry entry) {
        int index = this.getEntryIndex(identifier);
        if (index >= 0) {
            index++;
        } else {
            LOGGER.error("[FANCYMENU] Failed to add ContextMenu entry (" + entry.identifier + ") after other entry (" + identifier + ")! Target entry not found! Will add the entry at the end instead!");
            index = this.entries.size();
        }
        return this.addEntryAt(index, entry);
    }

    public ContextMenuEntry addEntryBefore(@NotNull String identifier, @NotNull ContextMenuEntry entry) {
        int index = this.getEntryIndex(identifier);
        if (index < 0) {
            LOGGER.error("[FANCYMENU] Failed to add ContextMenu entry (" + entry.identifier + ") before other entry (" + identifier + ")! Target entry not found! Will add the entry at the end instead!");
            index = this.entries.size();
        }
        return this.addEntryAt(index, entry);
    }

    @NotNull
    public ContextMenuEntry addEntry(@NotNull ContextMenuEntry entry) {
        return this.addEntryAt(this.entries.size(), entry);
    }

    @NotNull
    public ContextMenuEntry addEntryAt(int index, @NotNull ContextMenuEntry entry) {
        if (this.hasEntry(entry.identifier)) {
            LOGGER.error("[FANCYMENU] Failed to add ContextMenu entry! Identifier already in use: " + entry.identifier);
        } else {
            this.entries.add(index, entry);
        }
        return entry;
    }

    @NotNull
    public ContextMenu removeEntry(String identifier) {
        ContextMenuEntry e = this.getEntry(identifier);
        if (e != null) {
            this.entries.remove(e);
            e.onRemoved();
        }
        return this;
    }

    @Nullable
    public ContextMenuEntry getEntry(String identifier) {
        for (ContextMenuEntry e : this.entries) {
            if (e.identifier.equals(identifier)) {
                return e;
            }
        }
        return null;
    }

    /**
     * Returns the entry index or -1 if the entry was not found.
     */
    public int getEntryIndex(String identifier) {
        ContextMenuEntry e = this.getEntry(identifier);
        if (e != null) {
            return this.entries.indexOf(e);
        }
        return -1;
    }

    /**
     * Returns a COPY of the entry list. Changes made to the list will not get reflected in the menu.
     */
    @NotNull
    public List<ContextMenuEntry> getEntries() {
        return new ArrayList<>(this.entries);
    }

    public boolean hasEntry(String identifier) {
        return this.getEntry(identifier) != null;
    }

    public float getScale() {
        return this.scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    protected int getActualX() {
        if (this.isSubMenu()) {
            SubMenuOpeningSide side = this.getPossibleSubMenuOpeningSide();
            if (side == SubMenuOpeningSide.LEFT) {
                return this.parentEntry.parent.getActualX() - this.getScaledWidth() + 5;
            }
            if (side == SubMenuOpeningSide.RIGHT) {
                return this.parentEntry.parent.getActualX() + this.parentEntry.parent.getScaledWidth() - 5;
            }
        }
        if ((this.getX() + this.getScaledWidth()) > (getScreenWidth() - 5)) {
            int offset = (this.getX() + this.getScaledWidth()) - (getScreenWidth() - 5);
            return this.getX() - offset;
        }
        return this.getX();
    }

    protected int getActualY() {
        int y = this.getY();
        if (this.isSubMenu()) {
            float parentMenuScale = UIBase.calculateFixedScale(this.parentEntry.parent.scale);
            y = (int) ((float)this.parentEntry.y * parentMenuScale);
            y += 5;
        }
        if ((y + this.getScaledHeight()) >= (getScreenHeight() - 5)) {
            int offset = (y + this.getScaledHeight()) - (getScreenHeight() - 5);
            return y - offset;
        }
        return y;
    }

    protected SubMenuOpeningSide getPossibleSubMenuOpeningSide() {
        if (this.isSubMenu()) {
            if ((this.subMenuOpeningSide == SubMenuOpeningSide.LEFT) && ((this.parentEntry.parent.getActualX() - this.getScaledWidth() + 5) < 5)) {
                return SubMenuOpeningSide.RIGHT;
            }
            if ((this.subMenuOpeningSide == SubMenuOpeningSide.RIGHT) && ((this.parentEntry.parent.getActualX() + this.parentEntry.parent.getScaledWidth() - 5 + this.getScaledWidth()) > (getScreenWidth() - 5))) {
                return SubMenuOpeningSide.LEFT;
            }
        }
        return this.subMenuOpeningSide;
    }

    public int getX() {
        return this.rawX - 1; // - border
    }

    public int getY() {
        return this.rawY - 1; // - border
    }

    public int getWidth() {
        return this.rawWidth + 2; // + border
    }

    public int getScaledWidth() {
        float scale = UIBase.calculateFixedScale(this.scale);
        return (int) ((float)this.getWidth() * scale);
    }

    public int getHeight() {
        return this.rawHeight + 2; // + border
    }

    public int getScaledHeight() {
        float scale = UIBase.calculateFixedScale(this.scale);
        return (int) ((float)this.getHeight() * scale);
    }

    public boolean isHovered() {
        if (!this.isOpen()) return false;
        for (ContextMenuEntry e : this.entries) {
            if (e.isHovered()) return true;
        }
        return false;
    }

    protected void unhoverAllEntries() {
        for (ContextMenuEntry e : this.entries) {
            e.setHovered(false);
        }
    }

    public SubMenuOpeningSide getSubMenuOpeningSide() {
        return this.subMenuOpeningSide;
    }

    public void setSubMenuOpeningSide(SubMenuOpeningSide subMenuOpeningSide) {
        this.subMenuOpeningSide = subMenuOpeningSide;
    }

    public boolean isSubMenu() {
        return this.parentEntry != null;
    }

    @Nullable
    public SubMenuContextMenuEntry getParentEntry() {
        return this.parentEntry;
    }

    public boolean isSubMenuHovered() {
        if (!this.isOpen()) return false;
        for (ContextMenuEntry e : this.entries) {
            if (e instanceof SubMenuContextMenuEntry s) {
                if (s.subContextMenu.isHovered()) return true;
            }
        }
        return false;
    }

    public boolean isSubMenuOpen() {
        if (!this.isOpen()) return false;
        for (ContextMenuEntry e : this.entries) {
            if (e instanceof SubMenuContextMenuEntry s) {
                if (s.subContextMenu.isOpen()) return true;
            }
        }
        return false;
    }

    public ContextMenu openMenuAt(int x, int y) {
        this.closeSubMenus();
        this.unhoverAllEntries();
        this.rawX = Math.max(5, x);
        this.rawY = Math.max(5, y);
        this.open = true;
        return this;
    }

    public ContextMenu openMenuAtMouse() {
        return this.openMenuAt(MouseInput.getMouseX(), MouseInput.getMouseY());
    }

    public ContextMenu closeMenu() {
        this.closeSubMenus();
        this.unhoverAllEntries();
        this.open = false;
        return this;
    }

    public ContextMenu closeSubMenus() {
        for (ContextMenuEntry e : this.entries) {
            if (e instanceof SubMenuContextMenuEntry s) {
                s.subContextMenu.closeMenu();
            }
        }
        return this;
    }

    public boolean isOpen() {
        return this.open;
    }

    public boolean isUserNavigatingInMenu() {
        return this.isHovered() || this.isUserNavigatingInSubMenu();
    }

    public boolean isUserNavigatingInSubMenu() {
        for (ContextMenuEntry e : this.entries) {
            if (e instanceof SubMenuContextMenuEntry s) {
                if (s.subContextMenu.isUserNavigatingInMenu()) return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float scale = UIBase.calculateFixedScale(this.scale);
        int scaledMouseX = (int) ((float)mouseX / scale);
        int scaledMouseY = (int) ((float)mouseY / scale);
        for (ContextMenuEntry entry : this.entries) {
            entry.mouseClicked(scaledMouseX, scaledMouseY, button);
        }
        //Handle click for sub context menus
        for (ContextMenuEntry e : this.entries) {
            if (e instanceof SubMenuContextMenuEntry s) {
                s.subContextMenu.mouseClicked(mouseX, mouseY, button);
            }
        }
        return GuiEventListener.super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void setFocused(boolean var1) {
    }

    @Override
    public boolean isFocused() {
        return false;
    }

    @Override
    public @NotNull NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(@NotNull NarrationElementOutput var1) {
    }

    protected static int getScreenWidth() {
        Screen s = Minecraft.getInstance().screen;
        if (s != null) {
            return s.width;
        }
        return 1;
    }

    protected static int getScreenHeight() {
        Screen s = Minecraft.getInstance().screen;
        if (s != null) {
            return s.height;
        }
        return 1;
    }

    public static abstract class ContextMenuEntry extends GuiComponent implements Renderable, GuiEventListener {

        protected String identifier;
        protected ContextMenu parent;
        /** Only for internal use. This gets set by the parent {@link ContextMenu}. **/
        protected int x;
        /** Only for internal use. This gets set by the parent {@link ContextMenu}. **/
        protected int y;
        /** Only for internal use. This gets set by the parent {@link ContextMenu}. **/
        protected int width;
        protected int height = 20;
        @Nullable
        protected Ticker ticker;
        @Nullable
        protected Tooltip tooltip;
        protected boolean hovered = false;
        protected boolean active = true;
        protected Font font = Minecraft.getInstance().font;

        public ContextMenuEntry(@NotNull String identifier, @NotNull ContextMenu parent) {
            this.identifier = identifier;
            this.parent = parent;
        }

        public abstract void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial);

        @NotNull
        public String getIdentifier() {
            return identifier;
        }

        @NotNull
        public ContextMenu getParent() {
            return parent;
        }

        public int getHeight() {
            return this.height;
        }

        public ContextMenuEntry setHeight(int height) {
            this.height = height;
            return this;
        }

        public abstract int getMinWidth();

        protected void setHovered(boolean hovered) {
            this.hovered = hovered;
        }

        public boolean isHovered() {
            return this.hovered;
        }

        public boolean isActive() {
            return this.active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public ContextMenuEntry setTicker(@Nullable Ticker ticker) {
            this.ticker = ticker;
            return this;
        }

        public ContextMenuEntry setTooltip(@Nullable Tooltip tooltip) {
            this.tooltip = tooltip;
            return this;
        }

        @Nullable
        public Tooltip getTooltip() {
            return this.tooltip;
        }

        protected void onRemoved() {
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return GuiEventListener.super.mouseClicked(mouseX, mouseY, button);
        }

        @FunctionalInterface
        public interface Ticker {
            void onTick(ContextMenu menu, ContextMenuEntry entry);
        }

    }

    public static class ClickableContextMenuEntry extends ContextMenuEntry {

        @NotNull
        protected ClickAction clickAction;
        @NotNull
        protected Component label;

        public ClickableContextMenuEntry(@NotNull String identifier, @NotNull ContextMenu parent, @NotNull Component label, @NotNull ClickAction clickAction) {
            super(identifier, parent);
            this.clickAction = clickAction;
            this.label = label;
        }

        @Override
        public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

            this.renderBackground(pose);

            int labelX = this.x + 10;
            int labelY = this.y + (this.height / 2) - (this.font.lineHeight / 2);
            this.font.drawShadow(pose, this.label, labelX, labelY, this.isActive() ? UIBase.TEXT_COLOR_GREY_4.getRGB() : UIBase.TEXT_COLOR_GREY_3.getRGB());

        }

        protected void renderBackground(@NotNull PoseStack pose) {
            if (this.isHovered() && this.isActive()) {
                fill(pose, this.x, this.y, this.x + this.width, this.y + this.height, UIBase.ELEMENT_BACKGROUND_COLOR_HOVER.getRGB());
            }
        }

        public ClickableContextMenuEntry setLabel(@NotNull Component label) {
            this.label = label;
            return this;
        }

        public ClickableContextMenuEntry setClickAction(@NotNull ClickAction clickAction) {
            Objects.requireNonNull(clickAction);
            this.clickAction = clickAction;
            return this;
        }

        @Override
        public ClickableContextMenuEntry setTicker(@Nullable Ticker ticker) {
            return (ClickableContextMenuEntry) super.setTicker(ticker);
        }

        @Override
        public ClickableContextMenuEntry setTooltip(@Nullable Tooltip tooltip) {
            return (ClickableContextMenuEntry) super.setTooltip(tooltip);
        }

        @Override
        public ClickableContextMenuEntry setHeight(int height) {
            return (ClickableContextMenuEntry) super.setHeight(height);
        }

        @Override
        public int getMinWidth() {
            return Minecraft.getInstance().font.width(this.label) + 20;
        }

        @Override
        public void setFocused(boolean var1) {
        }

        @Override
        public boolean isFocused() {
            return false;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if ((button == 0) && this.isHovered() && this.isActive() && !this.parent.isSubMenuHovered()) {
                this.clickAction.onClick(this.parent, this);
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @FunctionalInterface
        public interface ClickAction {
            void onClick(ContextMenu menu, ClickableContextMenuEntry entry);
        }

    }

    public static class SubMenuContextMenuEntry extends ClickableContextMenuEntry {

        @NotNull
        protected ContextMenu subContextMenu;
        protected boolean subMenuHoverTicked = false;
        protected boolean subMenuHoveredAfterOpen = false;
        protected long entryHoverStartTime = -1;
        protected long entryNotHoveredStartTime = -1;

        public SubMenuContextMenuEntry(@NotNull String identifier, @NotNull ContextMenu parent, @NotNull Component label, @NotNull ContextMenu subContextMenu) {
            super(identifier, parent, label, ((menu, entry) -> {}));
            this.subContextMenu = subContextMenu;
            this.subContextMenu.parentEntry = this;
            this.setClickAction((menu, entry) -> this.openSubMenu());
        }

        @Override
        public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
            this.tickEntry();
            super.render(pose, mouseX, mouseY, partial);
        }

        @Override
        protected void renderBackground(@NotNull PoseStack pose) {
            boolean hover = this.hovered;
            this.hovered = this.hovered || this.subContextMenu.isOpen();
            super.renderBackground(pose);
            this.hovered = hover;
        }

        protected void tickEntry() {
            //Close sub menu when hovering over parent menu AFTER sub menu was hovered
            if (!this.subContextMenu.isOpen()) {
                this.subMenuHoveredAfterOpen = false;
                this.subMenuHoverTicked = false;
            }
            if (this.subContextMenu.isHovered()) {
                if (!this.subMenuHoverTicked) {
                    this.subMenuHoverTicked = true;
                } else {
                    this.subMenuHoveredAfterOpen = true;
                }
            }
            if (this.parent.isHovered() && !this.isHovered() && this.subContextMenu.isOpen() && !this.subContextMenu.isUserNavigatingInMenu() && this.subMenuHoveredAfterOpen) {
                this.subContextMenu.closeMenu();
            }
            //Open sub menu on entry hover
            if (this.isHovered() && !this.parent.isSubMenuHovered()) {
                long now = System.currentTimeMillis();
                if (this.entryHoverStartTime == -1) {
                    this.entryHoverStartTime = now;
                }
                if (((this.entryHoverStartTime + 400) < now) && !this.subContextMenu.isOpen()) {
                    this.parent.closeSubMenus();
                    this.openSubMenu();
                }
            } else {
                this.entryHoverStartTime = -1;
            }
            //Close sub menu if not hovered
            if (!this.isHovered() && this.parent.isHovered() && !this.parent.isSubMenuHovered()) {
                long now = System.currentTimeMillis();
                if (this.entryNotHoveredStartTime == -1) {
                    this.entryNotHoveredStartTime = now;
                }
                if (((this.entryNotHoveredStartTime + 400) < now) && this.subContextMenu.isOpen()) {
                    this.subContextMenu.closeMenu();
                }
            } else {
                this.entryNotHoveredStartTime = -1;
            }
        }

        protected void openSubMenu() {
            this.subContextMenu.openMenuAt(0,0);
        }

        @NotNull
        public ContextMenu getSubContextMenu() {
            return this.subContextMenu;
        }

        public void setSubContextMenu(@NotNull ContextMenu subContextMenu) {
            this.subContextMenu.closeMenu();
            this.subContextMenu.parentEntry = null;
            this.subContextMenu = subContextMenu;
            this.subContextMenu.parentEntry = this;
        }

        @NotNull
        public SubMenuOpeningSide getSubMenuOpeningSide() {
            return this.subContextMenu.subMenuOpeningSide;
        }

        public void setSubMenuOpeningSide(@NotNull SubMenuOpeningSide subMenuOpeningSide) {
            this.subContextMenu.subMenuOpeningSide = subMenuOpeningSide;
        }

        @Override
        protected void onRemoved() {
            this.subContextMenu.parentEntry = null;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            //Close sub menu when left-clicking outside the menu
            if ((button == 0) && this.subContextMenu.isOpen() && !this.subContextMenu.isUserNavigatingInMenu()) {
                this.subContextMenu.closeMenu();
                LOGGER.info("CLOSED SUB IN MOUSE_CLICKED " + this.identifier);
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

    }

    public static class SeparatorContextMenuEntry extends ContextMenuEntry {

        public SeparatorContextMenuEntry(@NotNull String identifier, @NotNull ContextMenu parent) {
            super(identifier, parent);
            this.height = 9;
        }

        @Override
        public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
            fill(pose, this.x + 10, this.y + 4, this.x + this.width - 10, this.y + 5, UIBase.ELEMENT_BORDER_COLOR_IDLE.getRGB());
        }

        @Override
        public SeparatorContextMenuEntry setTicker(@Nullable Ticker ticker) {
            return (SeparatorContextMenuEntry) super.setTicker(ticker);
        }

        @Override
        public SeparatorContextMenuEntry setTooltip(@Nullable Tooltip tooltip) {
            return (SeparatorContextMenuEntry) super.setTooltip(tooltip);
        }

        @Override
        public SeparatorContextMenuEntry setHeight(int height) {
            return (SeparatorContextMenuEntry) super.setHeight(height);
        }

        @Override
        public int getMinWidth() {
            return 20;
        }

        @Override
        public void setFocused(boolean var1) {
        }

        @Override
        public boolean isFocused() {
            return false;
        }

    }

    public enum SubMenuOpeningSide {
        LEFT,
        RIGHT
    }

}
