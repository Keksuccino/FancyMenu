package de.keksuccino.fancymenu.rendering.ui.contextmenu.v2;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.properties.RuntimePropertyContainer;
import de.keksuccino.fancymenu.rendering.ui.UIBase;
import de.keksuccino.fancymenu.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.rendering.ui.tooltip.TooltipHandler;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.rendering.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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

    private static final Logger LOGGER = LogManager.getLogger();

    private static final ResourceLocation SUB_CONTEXT_MENU_ARROW_ICON = new ResourceLocation("fancymenu", "textures/context_menu_sub_arrow.png");
    private static final ResourceLocation CONTEXT_MENU_TOOLTIP_ICON = new ResourceLocation("fancymenu", "textures/context_menu_tooltip.png");
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
    protected boolean shadow = true;
    protected boolean applyDefaultTooltipStyle = true;

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

        if (!this.isOpen()) return;

        float scale = UIBase.calculateFixedScale(this.getScale());

        RenderSystem.enableBlend();
        pose.pushPose();
        pose.scale(scale, scale, scale);
        pose.translate(0.0F, 0.0F, 400.0F);

        List<ContextMenuEntry> renderEntries = new ArrayList<>();
        renderEntries.add(new SpacerContextMenuEntry("unregistered_spacer_top", this));

        this.rawWidth = 20;
        this.rawHeight = 0;
        ContextMenuEntry prev = null;
        for (ContextMenuEntry e : this.entries) {
            //Don't render separator entries at the start and end of the menu OR if the previous entry was also a separator entry
            if ((e instanceof SeparatorContextMenuEntry) && ((prev instanceof SeparatorContextMenuEntry) || ((e == this.entries.get(0)) || (e == this.entries.get(this.entries.size()-1))))) {
                prev = e;
                continue;
            }
            //Pre-tick
            if (e.tickAction != null) {
                e.tickAction.run(this, e, false);
            }
            //Update width + height
            int w = e.getMinWidth();
            if (w > this.rawWidth) {
                this.rawWidth = w;
            }
            this.rawHeight += e.getHeight();
            renderEntries.add(e);
            prev = e;
        }
        this.rawHeight += 8; //add top and bottom spacer to total height

        renderEntries.add(new SpacerContextMenuEntry("unregistered_spacer_bottom", this));

        int x = this.getActualX();
        int y = this.getActualY();
        int scaledX = (int)((float)x/scale);
        int scaledY = (int)((float)y/scale);
        int scaledMouseX = (int) ((float)mouseX / scale);
        int scaledMouseY = (int) ((float)mouseY / scale);
        boolean navigatingInSub = this.isUserNavigatingInSubMenu();

        //Render shadow
        if (this.hasShadow()) {
            fill(pose, scaledX + 4, scaledY + 4, scaledX + this.getWidth() + 4, scaledY + this.getHeight() + 4, MENU_SHADOW_COLOR.getRGB());
        }
        //Render background
        fill(pose, scaledX, scaledY, scaledX + this.getWidth(), scaledY + this.getHeight(), MENU_BACKGROUND_COLOR.getRGB());
        //Update + render entries
        int entryY = scaledY;
        for (ContextMenuEntry e : renderEntries) {
            e.x = scaledX;
            e.y = entryY; //already scaled
            e.width = this.rawWidth; //don't scale, because already scaled via pose.scale()
            boolean hover = e.isHovered();
            e.setHovered(!navigatingInSub && UIBase.isXYInArea(scaledMouseX, scaledMouseY, e.x, e.y, e.width, e.getHeight()));
            //Run hover action of element if its hover state changed to hovered
            if (!hover && e.isHovered() && (e.hoverAction != null)) {
                e.hoverAction.run(this, e, false);
            }
            e.render(pose, scaledMouseX, scaledMouseY, partial);
            entryY += e.getHeight(); //don't scale this, because already scaled via pose.scale()
        }
        //Render border
        UIBase.renderBorder(pose, scaledX - 1, scaledY - 1, scaledX + this.rawWidth + 1, scaledY + this.rawHeight + 1, 1, UIBase.ELEMENT_BORDER_COLOR_IDLE, true, true, true, true);

        //Post-tick
        for (ContextMenuEntry e : renderEntries) {
            if (e.tickAction != null) {
                e.tickAction.run(this, e, true);
            }
        }

        pose.popPose();

        //Render sub context menus
        for (ContextMenuEntry e : renderEntries) {
            if (e instanceof SubMenuContextMenuEntry s) {
                s.subContextMenu.render(pose, mouseX, mouseY, partial);
            }
        }

    }

    @NotNull
    public SubMenuContextMenuEntry addSubMenuEntryAt(int index, @NotNull String identifier, @NotNull Component label, @NotNull ContextMenu subContextMenu) {
        SubMenuContextMenuEntry e = new SubMenuContextMenuEntry(identifier, this, label, subContextMenu);
        return (SubMenuContextMenuEntry) this.addEntryAt(index, e);
    }

    @NotNull
    public SubMenuContextMenuEntry addSubMenuEntryBefore(@NotNull String addBeforeIdentifier, @NotNull String identifier, @NotNull Component label, @NotNull ContextMenu subContextMenu) {
        SubMenuContextMenuEntry e = new SubMenuContextMenuEntry(identifier, this, label, subContextMenu);
        return (SubMenuContextMenuEntry) this.addEntryBefore(addBeforeIdentifier, e);
    }

    @NotNull
    public SubMenuContextMenuEntry addSubMenuEntryAfter(@NotNull String addAfterIdentifier, @NotNull String identifier, @NotNull Component label, @NotNull ContextMenu subContextMenu) {
        SubMenuContextMenuEntry e = new SubMenuContextMenuEntry(identifier, this, label, subContextMenu);
        return (SubMenuContextMenuEntry) this.addEntryAfter(addAfterIdentifier, e);
    }

    @NotNull
    public SubMenuContextMenuEntry addSubMenuEntry(@NotNull String identifier, @NotNull Component label, @NotNull ContextMenu subContextMenu) {
        SubMenuContextMenuEntry e = new SubMenuContextMenuEntry(identifier, this, label, subContextMenu);
        return (SubMenuContextMenuEntry) this.addEntry(e);
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

    @NotNull
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

    @NotNull
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

    @NotNull
    public ContextMenu clearEntries() {
        this.closeMenu();
        for (ContextMenuEntry e : this.entries) {
            e.onRemoved();
        }
        this.entries.clear();
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

    @NotNull
    public ContextMenu setScale(float scale) {
        this.scale = scale;
        return this;
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

    @NotNull
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

    @NotNull
    protected ContextMenu unhoverAllEntries() {
        for (ContextMenuEntry e : this.entries) {
            e.setHovered(false);
        }
        return this;
    }

    public boolean hasShadow() {
        return this.shadow;
    }

    @NotNull
    public ContextMenu setShadow(boolean shadow) {
        this.shadow = shadow;
        return this;
    }

    public boolean isApplyDefaultTooltipStyle() {
        return this.applyDefaultTooltipStyle;
    }

    public void setApplyDefaultTooltipStyle(boolean applyDefaultTooltipStyle) {
        this.applyDefaultTooltipStyle = applyDefaultTooltipStyle;
    }

    @NotNull
    public SubMenuOpeningSide getSubMenuOpeningSide() {
        return this.subMenuOpeningSide;
    }

    @NotNull
    public ContextMenu setSubMenuOpeningSide(@NotNull SubMenuOpeningSide subMenuOpeningSide) {
        Objects.requireNonNull(subMenuOpeningSide);
        this.subMenuOpeningSide = subMenuOpeningSide;
        return this;
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

    @NotNull
    public ContextMenu openMenuAt(int x, int y) {
        this.closeSubMenus();
        this.unhoverAllEntries();
        this.rawX = Math.max(5, x);
        this.rawY = Math.max(5, y);
        this.open = true;
        return this;
    }

    @NotNull
    public ContextMenu openMenuAtMouse() {
        return this.openMenuAt(MouseInput.getMouseX(), MouseInput.getMouseY());
    }

    @NotNull
    public ContextMenu closeMenu() {
        this.closeSubMenus();
        this.unhoverAllEntries();
        this.open = false;
        return this;
    }

    @NotNull
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

    @NotNull
    protected List<ContextMenuEntry> getStackableEntries() {
        List<ContextMenuEntry> l = new ArrayList<>();
        for (ContextMenuEntry e : this.entries) {
            if (e.isStackable()) l.add(e);
        }
        return l;
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

    /**
     * Will stack all stackable settings and all stackable ACTIVE {@link ContextMenuEntry}s of the given {@link ContextMenu}s and returns them as a new (stacked) instance.
     */
    @NotNull
    public static ContextMenu stackContextMenus(@NotNull List<ContextMenu> menusToStack) {
        return stackContextMenus(menusToStack.toArray(new ContextMenu[0]));
    }

    /**
     * Will stack all stackable settings and all stackable ACTIVE {@link ContextMenuEntry}s of the given {@link ContextMenu}s and returns them as a new (stacked) instance.
     */
    @NotNull
    public static ContextMenu stackContextMenus(@NotNull ContextMenu... menusToStack) {

        ContextMenu stacked = new ContextMenu();

        if (menusToStack.length > 0) {

            stacked.scale = menusToStack[0].scale;
            stacked.subMenuOpeningSide = menusToStack[0].subMenuOpeningSide;
            stacked.shadow = menusToStack[0].shadow;

            for (ContextMenuEntry ignoredEntry : menusToStack[0].getStackableEntries()) {

                RuntimePropertyContainer stackProperties = new RuntimePropertyContainer();
                List<ContextMenuEntry> entryStack = collectInstancesOfStackableEntryInMenus(ignoredEntry.identifier, menusToStack);
                if (!entryStack.isEmpty()) {

                    ContextMenuEntry firstOriginal = entryStack.get(0);
                    List<ContextMenuEntry> entryStackCopyWithoutFirst = new ArrayList<>();
                    entryStack.forEach((entry) ->  {
                        if (entry != firstOriginal) entryStackCopyWithoutFirst.add(entry.copy());
                    });

                    ContextMenuEntry first = firstOriginal.copy();
                    first.stackMeta.firstInStack = true;
                    first.stackMeta.lastInStack = false;
                    first.stackMeta.partOfStack = true;
                    first.stackMeta.properties = stackProperties;
                    first.parent = stacked;
                    stacked.addEntry(first);
                    if (first instanceof SubMenuContextMenuEntry s) {
                        s.setSubContextMenu(stackContextMenus(getSubContextMenusOfSubMenuEntries(entryStack)));
                        s.stackMeta.lastInStack = true;
                    } else {
                        ContextMenuEntry prev = first;
                        for (ContextMenuEntry e2 : entryStackCopyWithoutFirst) {
                            prev.stackMeta.nextInStack = e2;
                            prev = e2;
                            e2.stackMeta.properties = stackProperties;
                            e2.stackMeta.partOfStack = true;
                            e2.stackMeta.firstInStack = false;
                            e2.stackMeta.lastInStack = false;
                            e2.parent = stacked;
                        }
                        prev.stackMeta.lastInStack = true;
                    }

                }

            }

        }

        return stacked;

    }

    protected static List<ContextMenuEntry> collectInstancesOfStackableEntryInMenus(String entryIdentifier, ContextMenu[] menus) {
        List<ContextMenuEntry> entries = new ArrayList<>();
        for (ContextMenu m : menus) {
            ContextMenuEntry e = m.getEntry(entryIdentifier);
            if ((e != null) && e.isStackable() && e.isActive()) {
                entries.add(e);
            }
        }
        return entries;
    }

    protected static List<ContextMenu> getSubContextMenusOfSubMenuEntries(List<ContextMenuEntry> entries) {
        List<ContextMenu> l = new ArrayList<>();
        for (ContextMenuEntry e : entries) {
            if (e instanceof SubMenuContextMenuEntry s) {
                l.add(s.subContextMenu);
            }
        }
        return l;
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
        protected EntryTask tickAction;
        protected EntryTask hoverAction;
        protected boolean hovered = false;
        protected ContextMenuStackMeta stackMeta = new ContextMenuStackMeta();
        @Nullable
        protected BooleanSupplier activeStateSupplier;
        @Nullable
        protected Supplier<Tooltip> tooltipSupplier;
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
            return (this.activeStateSupplier == null) || this.activeStateSupplier.getBoolean(this.parent, this);
        }

        public ContextMenuEntry setIsActiveSupplier(@Nullable BooleanSupplier activeStateSupplier) {
            this.activeStateSupplier = activeStateSupplier;
            return this;
        }

        public ContextMenuEntry setTickAction(@Nullable EntryTask tickAction) {
            this.tickAction = tickAction;
            return this;
        }

        public ContextMenuEntry setHoverAction(@Nullable EntryTask hoverAction) {
            this.hoverAction = hoverAction;
            return this;
        }

        public ContextMenuEntry setTooltipSupplier(@Nullable Supplier<Tooltip> tooltipSupplier) {
            this.tooltipSupplier = tooltipSupplier;
            return this;
        }

        @Nullable
        public Tooltip getTooltip() {
            return (this.tooltipSupplier != null) ? this.tooltipSupplier.get(this.parent, this) : null;
        }

        public ContextMenuEntry setStackable(boolean stackable) {
            this.getStackMeta().setStackable(stackable);
            return this;
        }

        public boolean isStackable() {
            return this.getStackMeta().isStackable();
        }

        @NotNull
        public ContextMenuStackMeta getStackMeta() {
            return this.stackMeta;
        }

        protected void onRemoved() {
        }

        public abstract ContextMenuEntry copy();

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return GuiEventListener.super.mouseClicked(mouseX, mouseY, button);
        }

        @FunctionalInterface
        public interface EntryTask {
            /**
             * @param menu The {@link ContextMenu} this {@link EntryTask}'s {@link ContextMenuEntry} is part of.
             * @param entry The {@link ContextMenuEntry} this {@link EntryTask} is part of.
             * @param isPost Only used for the {@link ContextMenuEntry#tickAction}.
             */
            void run(ContextMenu menu, ContextMenuEntry entry, boolean isPost);
        }

    }

    public static class ClickableContextMenuEntry extends ContextMenuEntry {

        @NotNull
        protected ClickAction clickAction;
        @NotNull
        protected Supplier<Component> labelSupplier;
        @Nullable
        protected Supplier<Component> shortcutTextSupplier;
        protected long tooltipIconHoverStart = -1;

        public ClickableContextMenuEntry(@NotNull String identifier, @NotNull ContextMenu parent, @NotNull Component label, @NotNull ClickAction clickAction) {
            super(identifier, parent);
            this.clickAction = clickAction;
            this.labelSupplier = (menu, entry) -> label;
        }

        @Override
        public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

            this.renderBackground(pose);

            int labelX = this.x + 10;
            int labelY = this.y + (this.height / 2) - (this.font.lineHeight / 2);
            this.font.draw(pose, this.getLabel(), labelX, labelY, this.isActive() ? UIBase.TEXT_COLOR_GREY_4.getRGB() : UIBase.TEXT_COLOR_GREY_3.getRGB());

            int shortcutTextWidth = 0;
            Component shortcutText = this.getShortcutText();
            if (shortcutText != null) {
                shortcutTextWidth = this.font.width(shortcutText);
                int shortcutX = this.x + this.width - 10 - shortcutTextWidth;
                this.font.draw(pose, shortcutText, shortcutX, labelY, this.isActive() ? UIBase.TEXT_COLOR_GREY_4.getRGB() : UIBase.TEXT_COLOR_GREY_3.getRGB());
            }

            this.renderTooltipIconAndRegisterTooltip(pose, mouseX, mouseY, (shortcutTextWidth > 0) ? -(shortcutTextWidth + 8) : 0);

        }

        protected void renderTooltipIconAndRegisterTooltip(PoseStack pose, int mouseX, int mouseY, int offsetX) {
            if (this.tooltipSupplier != null) {

                boolean iconHovered = this.isTooltipIconHovered(mouseX, mouseY, offsetX);
                if (iconHovered) {
                    if (this.tooltipIconHoverStart == -1) {
                        this.tooltipIconHoverStart = System.currentTimeMillis();
                    }
                } else {
                    this.tooltipIconHoverStart = -1;
                }
                boolean showTooltip = (this.tooltipIconHoverStart != -1) && ((this.tooltipIconHoverStart + 200) < System.currentTimeMillis());

                RenderSystem.enableBlend();
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, iconHovered ? 1.0F : 0.2F);
                RenderUtils.bindTexture(CONTEXT_MENU_TOOLTIP_ICON);
                blit(pose, this.getTooltipIconX() + offsetX, this.getTooltipIconY(), 0.0F, 0.0F, 10, 10, 10, 10);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

                Tooltip tooltip = this.getTooltip();
                if (tooltip != null) {
                    if (this.parent.isApplyDefaultTooltipStyle()) {
                        tooltip.setDefaultBackgroundColor();
                    }
                    TooltipHandler.INSTANCE.addTooltip(tooltip, () -> showTooltip, false, true);
                }

            }
        }

        protected boolean isTooltipIconHovered(int mouseX, int mouseY, int offsetX) {
            return UIBase.isXYInArea(mouseX, mouseY, this.getTooltipIconX() + offsetX, this.getTooltipIconY(), 10, 10);
        }

        protected int getTooltipIconX() {
            return this.x + this.width - 20;
        }

        protected int getTooltipIconY() {
            return this.y + 5;
        }

        protected void renderBackground(@NotNull PoseStack pose) {
            if (this.isHovered() && this.isActive()) {
                fill(pose, this.x, this.y, this.x + this.width, this.y + this.height, UIBase.ELEMENT_BACKGROUND_COLOR_HOVER.getRGB());
            }
        }

        @NotNull
        public ClickableContextMenuEntry setLabelSupplier(@NotNull Supplier<Component> labelSupplier) {
            Objects.requireNonNull(labelSupplier);
            this.labelSupplier = labelSupplier;
            return this;
        }

        @NotNull
        public Component getLabel() {
            Component c = this.labelSupplier.get(this.parent, this);
            Objects.requireNonNull(c);
            return c;
        }

        @NotNull
        public ClickableContextMenuEntry setClickAction(@NotNull ClickAction clickAction) {
            Objects.requireNonNull(clickAction);
            this.clickAction = clickAction;
            return this;
        }

        @Nullable
        public Component getShortcutText() {
            return (this.shortcutTextSupplier != null) ? this.shortcutTextSupplier.get(this.parent, this) : null;
        }

        @NotNull
        public ClickableContextMenuEntry setShortcutTextSupplier(@Nullable Supplier<Component> shortcutTextSupplier) {
            this.shortcutTextSupplier = shortcutTextSupplier;
            return this;
        }

        @Override
        public ClickableContextMenuEntry copy() {
            ClickableContextMenuEntry copy = new ClickableContextMenuEntry(this.identifier, this.parent, Component.literal(""), this.clickAction);
            copy.shortcutTextSupplier = this.shortcutTextSupplier;
            copy.labelSupplier = this.labelSupplier;
            copy.height = this.height;
            copy.tickAction = this.tickAction;
            copy.tooltipSupplier = this.tooltipSupplier;
            copy.activeStateSupplier = this.activeStateSupplier;
            return copy;
        }

        @Override
        public ClickableContextMenuEntry setTickAction(@Nullable EntryTask tickAction) {
            return (ClickableContextMenuEntry) super.setTickAction(tickAction);
        }

        @Override
        public ClickableContextMenuEntry setHoverAction(@Nullable EntryTask hoverAction) {
            return (ClickableContextMenuEntry) super.setHoverAction(hoverAction);
        }

        @Override
        public ClickableContextMenuEntry setTooltipSupplier(@Nullable Supplier<Tooltip> tooltipSupplier) {
            return (ClickableContextMenuEntry) super.setTooltipSupplier(tooltipSupplier);
        }

        @Override
        public ClickableContextMenuEntry setHeight(int height) {
            return (ClickableContextMenuEntry) super.setHeight(height);
        }

        @Override
        public ClickableContextMenuEntry setIsActiveSupplier(@Nullable BooleanSupplier activeStateSupplier) {
            return (ClickableContextMenuEntry) super.setIsActiveSupplier(activeStateSupplier);
        }

        @Override
        public ClickableContextMenuEntry setStackable(boolean stackable) {
            return (ClickableContextMenuEntry) super.setStackable(stackable);
        }

        @Override
        public int getMinWidth() {
            int i = Minecraft.getInstance().font.width(this.getLabel()) + 20;
            if (this.tooltipSupplier != null) {
                i += 30;
            }
            Component shortcutText = this.getShortcutText();
            if (shortcutText != null) {
                i += Minecraft.getInstance().font.width(shortcutText) + 30;
            }
            return i;
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
            this.clickAction = (menu, entry) -> this.openSubMenu();
        }

        @Override
        public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

            this.tickEntry();

            super.render(pose, mouseX, mouseY, partial);

            this.renderSubMenuArrow(pose);

        }

        protected void renderSubMenuArrow(PoseStack pose) {
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderUtils.bindTexture(SUB_CONTEXT_MENU_ARROW_ICON);
            blit(pose, this.x + this.width - 20, this.y + 5, 0.0F, 0.0F, 10, 10, 10, 10);
        }

        @Override
        protected int getTooltipIconX() {
            return super.getTooltipIconX() - 15;
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

        public SubMenuContextMenuEntry setSubMenuOpeningSide(@NotNull SubMenuOpeningSide subMenuOpeningSide) {
            this.subContextMenu.subMenuOpeningSide = subMenuOpeningSide;
            return this;
        }

        @Override
        public SubMenuContextMenuEntry copy() {
            SubMenuContextMenuEntry copy = new SubMenuContextMenuEntry(this.identifier, this.parent, Component.literal(""), new ContextMenu());
            copy.height = this.height;
            copy.tickAction = this.tickAction;
            copy.tooltipSupplier = this.tooltipSupplier;
            copy.activeStateSupplier = this.activeStateSupplier;
            copy.labelSupplier = this.labelSupplier;
            return copy;
        }

        @Override
        protected void onRemoved() {
            this.subContextMenu.closeMenu();
            this.subContextMenu.parentEntry = null;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            //Close sub menu when left-clicking outside the menu
            if ((button == 0) && this.subContextMenu.isOpen() && !this.subContextMenu.isUserNavigatingInMenu()) {
                this.subContextMenu.closeMenu();
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public int getMinWidth() {
            int i = super.getMinWidth();
            if (this.tooltipSupplier == null) {
                i += 30;
            } else {
                i += 15;
            }
            return i;
        }

        @Override
        public @NotNull SubMenuContextMenuEntry setLabelSupplier(@NotNull Supplier<Component> labelSupplier) {
            return (SubMenuContextMenuEntry) super.setLabelSupplier(labelSupplier);
        }

        @Override
        public SubMenuContextMenuEntry setTooltipSupplier(@Nullable Supplier<Tooltip> tooltipSupplier) {
            return (SubMenuContextMenuEntry) super.setTooltipSupplier(tooltipSupplier);
        }

        @Override
        public SubMenuContextMenuEntry setTickAction(@Nullable EntryTask tickAction) {
            return (SubMenuContextMenuEntry) super.setTickAction(tickAction);
        }

        @Override
        public SubMenuContextMenuEntry setHoverAction(@Nullable EntryTask hoverAction) {
            return (SubMenuContextMenuEntry) super.setHoverAction(hoverAction);
        }

        @Override
        public SubMenuContextMenuEntry setIsActiveSupplier(@Nullable BooleanSupplier activeStateSupplier) {
            return (SubMenuContextMenuEntry) super.setIsActiveSupplier(activeStateSupplier);
        }

        @Override
        public SubMenuContextMenuEntry setHeight(int height) {
            return (SubMenuContextMenuEntry) super.setHeight(height);
        }

        @Override
        public SubMenuContextMenuEntry setStackable(boolean stackable) {
            return (SubMenuContextMenuEntry) super.setStackable(stackable);
        }

        @Override
        public @NotNull SubMenuContextMenuEntry setClickAction(@NotNull ClickAction clickAction) {
            LOGGER.error("[FANCYMENU] You can't set the click action of SubMenuContextMenuEntries.");
            return this;
        }

        @Override
        public @NotNull SubMenuContextMenuEntry setShortcutTextSupplier(@Nullable Supplier<Component> shortcutTextSupplier) {
            LOGGER.error("[FANCYMENU] You can't set a shortcut text for SubMenuContextMenuEntries.");
            return this;
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
        public SeparatorContextMenuEntry setTickAction(@Nullable EntryTask tickAction) {
            return (SeparatorContextMenuEntry) super.setTickAction(tickAction);
        }

        @Override
        public SeparatorContextMenuEntry setHoverAction(@Nullable EntryTask hoverAction) {
            return (SeparatorContextMenuEntry) super.setHoverAction(hoverAction);
        }

        @Override
        public SeparatorContextMenuEntry setTooltipSupplier(@Nullable Supplier<Tooltip> tooltipSupplier) {
            return (SeparatorContextMenuEntry) super.setTooltipSupplier(tooltipSupplier);
        }

        @Override
        public SeparatorContextMenuEntry setHeight(int height) {
            return (SeparatorContextMenuEntry) super.setHeight(height);
        }

        @Override
        public SeparatorContextMenuEntry setIsActiveSupplier(@Nullable BooleanSupplier activeStateSupplier) {
            return (SeparatorContextMenuEntry) super.setIsActiveSupplier(activeStateSupplier);
        }

        @Override
        public SeparatorContextMenuEntry setStackable(boolean stackable) {
            return (SeparatorContextMenuEntry) super.setStackable(stackable);
        }

        @Override
        public SeparatorContextMenuEntry copy() {
            SeparatorContextMenuEntry copy = new SeparatorContextMenuEntry(this.identifier, this.parent);
            copy.height = this.height;
            copy.tickAction = this.tickAction;
            copy.tooltipSupplier = this.tooltipSupplier;
            copy.activeStateSupplier = this.activeStateSupplier;
            return copy;
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

    public static class SpacerContextMenuEntry extends ContextMenuEntry {

        public SpacerContextMenuEntry(@NotNull String identifier, @NotNull ContextMenu parent) {
            super(identifier, parent);
            this.height = 4;
        }

        @Override
        public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
        }

        @Override
        public int getMinWidth() {
            return 20;
        }

        @Override
        public ContextMenuEntry copy() {
            SpacerContextMenuEntry e = new SpacerContextMenuEntry(this.identifier, this.parent);
            e.height = this.height;
            return e;
        }

        @Override
        public void setFocused(boolean var1) {
        }

        @Override
        public boolean isFocused() {
            return false;
        }

    }

    public static class ContextMenuStackMeta {

        protected RuntimePropertyContainer properties = new RuntimePropertyContainer();
        protected boolean stackable = false;
        protected boolean partOfStack = false;
        protected boolean firstInStack = true;
        protected boolean lastInStack = true;
        protected ContextMenuEntry nextInStack;

        /**
         * This is a shared instance. Every entry in the stack has access to the same {@link RuntimePropertyContainer} instance.
         */
        @NotNull
        public RuntimePropertyContainer getProperties() {
            return this.properties;
        }

        public boolean isPartOfStack() {
            return this.partOfStack;
        }

        public boolean isFirstInStack() {
            return this.firstInStack;
        }

        public boolean isLastInStack() {
            return this.lastInStack;
        }

        public boolean isStackable() {
            return this.stackable;
        }

        public void setStackable(boolean stackable) {
            this.stackable = stackable;
        }

    }

    public enum SubMenuOpeningSide {
        LEFT,
        RIGHT
    }

    @FunctionalInterface
    public interface Supplier<T> {
        T get(ContextMenu menu, ContextMenuEntry entry);
    }

    @FunctionalInterface
    public interface BooleanSupplier extends Supplier<Boolean> {

        default boolean getBoolean(ContextMenu menu, ContextMenuEntry entry) {
            Boolean b = this.get(menu, entry);
            if (b != null) {
                return b;
            }
            return false;
        }

    }

}
