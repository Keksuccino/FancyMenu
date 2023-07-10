package de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.util.cycle.ILocalizedValueCycle;
import de.keksuccino.fancymenu.util.properties.RuntimePropertyContainer;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.TooltipHandler;
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
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
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

@SuppressWarnings("all")
public class ContextMenu extends GuiComponent implements Renderable, GuiEventListener, NarratableEntry {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final ResourceLocation SUB_CONTEXT_MENU_ARROW_ICON = new ResourceLocation("fancymenu", "textures/contextmenu/context_menu_sub_arrow.png");
    private static final ResourceLocation CONTEXT_MENU_TOOLTIP_ICON = new ResourceLocation("fancymenu", "textures/contextmenu/context_menu_tooltip.png");
    private static final DrawableColor SHADOW_COLOR = DrawableColor.of(new Color(43, 43, 43, 100));

    protected final List<ContextMenuEntry<?>> entries = new ArrayList<>();
    protected float scale = UIBase.getUIScale();
    protected Float overriddenRenderScale = null;
    protected Float overriddenTooltipScale = null;
    protected boolean forceUIScale = true;
    protected boolean open = false;
    protected int rawX; // without border
    protected int rawY; // without border
    protected int rawWidth; // without border
    protected int rawHeight; // without border
    protected SubMenuContextMenuEntry parentEntry = null;
    protected SubMenuOpeningSide subMenuOpeningSide = SubMenuOpeningSide.RIGHT;
    protected boolean shadow = true;
    protected boolean keepDistanceToEdges = true;
    protected boolean forceDefaultTooltipStyle = true;
    protected boolean forceRawXY = false;
    protected boolean forceSide = false;

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

        if (!this.isOpen()) return;

        if (this.forceUIScale) this.scale = UIBase.getUIScale();
        float cachedScale = this.scale;
        if (this.overriddenRenderScale != null) this.scale = this.overriddenRenderScale;

        float scale = UIBase.calculateFixedScale(this.getScale());

        RenderSystem.enableBlend();
        UIBase.resetShaderColor();
        pose.pushPose();
        pose.scale(scale, scale, scale);
        pose.translate(0.0F, 0.0F, 400.0F);

        List<ContextMenuEntry<?>> renderEntries = new ArrayList<>();
        renderEntries.add(new SpacerContextMenuEntry("unregistered_spacer_top", this));

        //Check if icon space should get added to entries
        boolean addIconSpace = false;
        for (ContextMenuEntry<?> e : this.entries) {
            if (e instanceof ClickableContextMenuEntry<?> c) {
                if (c.icon != null) {
                    addIconSpace = true;
                    break;
                }
            }
        }

        this.rawWidth = 20;
        this.rawHeight = 0;
        ContextMenuEntry<?> prev = null;
        for (ContextMenuEntry<?> e : this.entries) {
            e.addSpaceForIcon = addIconSpace;
            //Don't render separator entries at the start and end of the menu OR if the previous entry was also a separator entry
            if ((e instanceof SeparatorContextMenuEntry) && ((prev instanceof SeparatorContextMenuEntry) || ((e == this.entries.get(0)) || (e == this.entries.get(this.entries.size()-1))))) {
                prev = e;
                continue;
            }
            //Don't render if hidden
            if (!e.isVisible()) {
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
        int scaledX = (int)((float)x/scale) + this.getBorderThickness();
        int scaledY = (int)((float)y/scale) + this.getBorderThickness();
        int scaledMouseX = (int) ((float)mouseX / scale);
        int scaledMouseY = (int) ((float)mouseY / scale);
        boolean navigatingInSub = this.isUserNavigatingInSubMenu();

        //Render shadow
        if (this.hasShadow()) {
            fill(pose, scaledX + 4, scaledY + 4, scaledX + this.getWidth() + 4, scaledY + this.getHeight() + 4, SHADOW_COLOR.getColorInt());
            UIBase.resetShaderColor();
        }
        //Render background
        fill(pose, scaledX, scaledY, scaledX + this.getWidth(), scaledY + this.getHeight(), UIBase.getUIColorScheme().element_background_color_normal.getColorInt());
        UIBase.resetShaderColor();
        //Update + render entries
        int entryY = scaledY;
        for (ContextMenuEntry<?> e : renderEntries) {
            e.x = scaledX;
            e.y = entryY; //already scaled
            e.width = this.getWidth(); //don't scale, because already scaled via pose.scale()
            boolean hover = e.isHovered();
            e.setHovered(!navigatingInSub && UIBase.isXYInArea(scaledMouseX, scaledMouseY, e.x, e.y, e.width, e.getHeight()));
            //Run hover action of element if its hover state changed to hovered
            if (!hover && e.isHovered() && (e.hoverAction != null)) {
                e.hoverAction.run(this, e, false);
            }
            RenderSystem.enableBlend();
            UIBase.resetShaderColor();
            e.render(pose, scaledMouseX, scaledMouseY, partial);
            entryY += e.getHeight(); //don't scale this, because already scaled via pose.scale()
        }
        UIBase.resetShaderColor();
        //Render border
        UIBase.renderBorder(pose, scaledX - this.getBorderThickness(), scaledY - this.getBorderThickness(), scaledX + this.getWidth() + this.getBorderThickness(), scaledY + this.getHeight() + this.getBorderThickness(), this.getBorderThickness(), UIBase.getUIColorScheme().context_menu_border_color.getColor(), true, true, true, true);

        //Post-tick
        for (ContextMenuEntry<?> e : renderEntries) {
            if (e.tickAction != null) {
                e.tickAction.run(this, e, true);
            }
        }

        pose.popPose();

        //Render sub context menus
        for (ContextMenuEntry<?> e : renderEntries) {
            if (e instanceof SubMenuContextMenuEntry s) {
                s.subContextMenu.forceSide = this.forceSide;
                s.subContextMenu.forceRawXY = this.forceRawXY;
                s.subContextMenu.shadow = this.shadow;
                s.subContextMenu.scale = this.scale;
                s.subContextMenu.forceUIScale = this.forceUIScale;
                s.subContextMenu.overriddenRenderScale = this.overriddenRenderScale;
                s.subContextMenu.overriddenTooltipScale = this.overriddenTooltipScale;
                s.subContextMenu.render(pose, mouseX, mouseY, partial);
            }
        }

        this.scale = cachedScale;

        UIBase.resetShaderColor();

    }

    @NotNull
    public SubMenuContextMenuEntry addSubMenuEntryAt(int index, @NotNull String identifier, @NotNull Component label, @NotNull ContextMenu subContextMenu) {
        SubMenuContextMenuEntry e = new SubMenuContextMenuEntry(identifier, this, label, subContextMenu);
        return this.addEntryAt(index, e);
    }

    @NotNull
    public SubMenuContextMenuEntry addSubMenuEntryBefore(@NotNull String addBeforeIdentifier, @NotNull String identifier, @NotNull Component label, @NotNull ContextMenu subContextMenu) {
        SubMenuContextMenuEntry e = new SubMenuContextMenuEntry(identifier, this, label, subContextMenu);
        return this.addEntryBefore(addBeforeIdentifier, e);
    }

    @NotNull
    public SubMenuContextMenuEntry addSubMenuEntryAfter(@NotNull String addAfterIdentifier, @NotNull String identifier, @NotNull Component label, @NotNull ContextMenu subContextMenu) {
        SubMenuContextMenuEntry e = new SubMenuContextMenuEntry(identifier, this, label, subContextMenu);
        return this.addEntryAfter(addAfterIdentifier, e);
    }

    @NotNull
    public SubMenuContextMenuEntry addSubMenuEntry(@NotNull String identifier, @NotNull Component label, @NotNull ContextMenu subContextMenu) {
        SubMenuContextMenuEntry e = new SubMenuContextMenuEntry(identifier, this, label, subContextMenu);
        return this.addEntry(e);
    }

    @NotNull
    public SeparatorContextMenuEntry addSeparatorEntryAt(int index, @NotNull String identifier) {
        SeparatorContextMenuEntry e = new SeparatorContextMenuEntry(identifier, this);
        return this.addEntryAt(index, e);
    }

    @NotNull
    public SeparatorContextMenuEntry addSeparatorEntryBefore(@NotNull String addBeforeIdentifier, @NotNull String identifier) {
        SeparatorContextMenuEntry e = new SeparatorContextMenuEntry(identifier, this);
        return this.addEntryBefore(addBeforeIdentifier, e);
    }

    @NotNull
    public SeparatorContextMenuEntry addSeparatorEntryAfter(@NotNull String addAfterIdentifier, @NotNull String identifier) {
        SeparatorContextMenuEntry e = new SeparatorContextMenuEntry(identifier, this);
        return this.addEntryAfter(addAfterIdentifier, e);
    }

    @NotNull
    public SeparatorContextMenuEntry addSeparatorEntry(@NotNull String identifier) {
        SeparatorContextMenuEntry e = new SeparatorContextMenuEntry(identifier, this);
        return this.addEntry(e);
    }

    @NotNull
    public <T> ValueCycleContextMenuEntry<T> addValueCycleEntryAt(int index, @NotNull String identifier, @NotNull ILocalizedValueCycle<T> valueCycle) {
        ValueCycleContextMenuEntry<T> e = new ValueCycleContextMenuEntry<>(identifier, this, valueCycle);
        return this.addEntryAt(index, e);
    }

    @NotNull
    public <T> ValueCycleContextMenuEntry<T> addValueCycleEntryAfter(@NotNull String addAfterIdentifier, @NotNull String identifier, @NotNull ILocalizedValueCycle<T> valueCycle) {
        ValueCycleContextMenuEntry<T> e = new ValueCycleContextMenuEntry<>(identifier, this, valueCycle);
        return this.addEntryAfter(addAfterIdentifier, e);
    }

    @NotNull
    public <T> ValueCycleContextMenuEntry<T> addValueCycleEntryBefore(@NotNull String addBeforeIdentifier, @NotNull String identifier, @NotNull ILocalizedValueCycle<T> valueCycle) {
        ValueCycleContextMenuEntry<T> e = new ValueCycleContextMenuEntry<>(identifier, this, valueCycle);
        return this.addEntryBefore(addBeforeIdentifier, e);
    }

    @NotNull
    public <T> ValueCycleContextMenuEntry<T> addValueCycleEntry(@NotNull String identifier, @NotNull ILocalizedValueCycle<T> valueCycle) {
        ValueCycleContextMenuEntry<T> e = new ValueCycleContextMenuEntry<>(identifier, this, valueCycle);
        return this.addEntry(e);
    }

    @NotNull
    public ClickableContextMenuEntry<?> addClickableEntryAt(int index, @NotNull String identifier, @NotNull Component label, @NotNull ClickableContextMenuEntry.ClickAction clickAction) {
        ClickableContextMenuEntry<?> e = new ClickableContextMenuEntry<>(identifier, this, label, clickAction);
        return this.addEntryAt(index, e);
    }

    @NotNull
    public ClickableContextMenuEntry<?> addClickableEntryAfter(@NotNull String addAfterIdentifier, @NotNull String identifier, @NotNull Component label, @NotNull ClickableContextMenuEntry.ClickAction clickAction) {
        ClickableContextMenuEntry<?> e = new ClickableContextMenuEntry<>(identifier, this, label, clickAction);
        return this.addEntryAfter(addAfterIdentifier, e);
    }

    @NotNull
    public ClickableContextMenuEntry<?> addClickableEntryBefore(@NotNull String addBeforeIdentifier, @NotNull String identifier, @NotNull Component label, @NotNull ClickableContextMenuEntry.ClickAction clickAction) {
        ClickableContextMenuEntry<?> e = new ClickableContextMenuEntry<>(identifier, this, label, clickAction);
        return this.addEntryBefore(addBeforeIdentifier, e);
    }

    @NotNull
    public ClickableContextMenuEntry<?> addClickableEntry(@NotNull String identifier, @NotNull Component label, @NotNull ClickableContextMenuEntry.ClickAction clickAction) {
        ClickableContextMenuEntry<?> e = new ClickableContextMenuEntry<>(identifier, this, label, clickAction);
        return this.addEntry(e);
    }

    @NotNull
    public <T extends ContextMenuEntry<?>> T addEntryAfter(@NotNull String identifier, @NotNull T entry) {
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
    public <T extends ContextMenuEntry<?>> T addEntryBefore(@NotNull String identifier, @NotNull T entry) {
        int index = this.getEntryIndex(identifier);
        if (index < 0) {
            LOGGER.error("[FANCYMENU] Failed to add ContextMenu entry (" + entry.identifier + ") before other entry (" + identifier + ")! Target entry not found! Will add the entry at the end instead!");
            index = this.entries.size();
        }
        return this.addEntryAt(index, entry);
    }

    @NotNull
    public <T extends ContextMenuEntry<?>> T addEntry(@NotNull T entry) {
        return this.addEntryAt(this.entries.size(), entry);
    }

    @NotNull
    public <T extends ContextMenuEntry<?>> T addEntryAt(int index, @NotNull T entry) {
        if (this.hasEntry(entry.identifier)) {
            LOGGER.error("[FANCYMENU] Failed to add ContextMenu entry! Identifier already in use: " + entry.identifier);
        } else {
            this.entries.add(index, entry);
        }
        return entry;
    }

    public ContextMenu removeEntry(String identifier) {
        ContextMenuEntry<?> e = this.getEntry(identifier);
        if (e != null) {
            this.entries.remove(e);
            e.onRemoved();
        }
        return this;
    }

    public ContextMenu clearEntries() {
        this.closeMenu();
        for (ContextMenuEntry<?> e : this.entries) {
            e.onRemoved();
        }
        this.entries.clear();
        return this;
    }

    @Nullable
    public ContextMenuEntry<?> getEntry(String identifier) {
        for (ContextMenuEntry<?> e : this.entries) {
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
        ContextMenuEntry<?> e = this.getEntry(identifier);
        if (e != null) {
            return this.entries.indexOf(e);
        }
        return -1;
    }

    /**
     * Returns a COPY of the entry list. Changes made to the list will not get reflected in the menu.
     */
    @NotNull
    public List<ContextMenuEntry<?>> getEntries() {
        return new ArrayList<>(this.entries);
    }

    public boolean hasEntry(String identifier) {
        return this.getEntry(identifier) != null;
    }

    public float getScale() {
        return this.scale;
    }

    public ContextMenu setScale(float scale) {
        if (this.forceUIScale) LOGGER.error("[FANCYMENU] Unable to set scale of ContextMenu while ContextMenu#isForceUIScale()!");
        this.scale = scale;
        return this;
    }

    @Nullable
    public Float getOverriddenRenderScale() {
        return this.overriddenRenderScale;
    }

    public ContextMenu setOverriddenRenderScale(@Nullable Float overriddenRenderScale) {
        this.overriddenRenderScale = overriddenRenderScale;
        return this;
    }

    @Nullable
    public Float getOverriddenTooltipScale() {
        return this.overriddenTooltipScale;
    }

    public ContextMenu setOverriddenTooltipScale(@Nullable Float overriddenTooltipScale) {
        this.overriddenTooltipScale = overriddenTooltipScale;
        return this;
    }

    public boolean isForceUIScale() {
        return this.forceUIScale;
    }

    public ContextMenu setForceUIScale(boolean forceUIScale) {
        this.forceUIScale = forceUIScale;
        return this;
    }

    protected int getMinDistanceToScreenEdge() {
        if (!this.keepDistanceToEdges) return 0;
        return 5;
    }

    protected int getActualX() {
        if (this.isSubMenu()) {
            float cachedScale = this.parentEntry.parent.scale;
            if (this.parentEntry.parent.overriddenRenderScale != null) this.parentEntry.parent.scale = this.parentEntry.parent.overriddenRenderScale;
            float scale = UIBase.calculateFixedScale(this.scale);
            int scaledOffsetX = (int) (5.0F * scale);
            SubMenuOpeningSide side = this.getPossibleSubMenuOpeningSide();
            if (side == SubMenuOpeningSide.LEFT) {
                int actualX = this.parentEntry.parent.getActualX() - this.getScaledWidth() + scaledOffsetX;
                this.parentEntry.parent.scale = cachedScale;
                return actualX;
            }
            if (side == SubMenuOpeningSide.RIGHT) {
                int actualX = this.parentEntry.parent.getActualX() + this.parentEntry.parent.getScaledWidth() - scaledOffsetX;
                this.parentEntry.parent.scale = cachedScale;
                return actualX;
            }
        }
        if (this.forceRawXY) {
            return this.getX();
        }
        if ((this.getX() + this.getScaledWidthWithBorder()) >= (getScreenWidth() - this.getMinDistanceToScreenEdge())) {
            return getScreenWidth() - this.getScaledWidthWithBorder() - this.getMinDistanceToScreenEdge() - 1;
        }
        return Math.max(this.getX(), this.getMinDistanceToScreenEdge());
    }

    protected int getActualY() {
        int y = this.getY();
        if (this.isSubMenu()) {
            float scale = UIBase.calculateFixedScale((this.overriddenRenderScale != null) ? this.overriddenRenderScale : this.scale);
            int scaledOffsetY = (int) (10.0F * scale);
            y = (int) ((float)this.parentEntry.y * scale);
            y += scaledOffsetY;
        }
        if (this.forceRawXY) {
            return y;
        }
        if ((y + this.getScaledHeightWithBorder()) >= (getScreenHeight() - this.getMinDistanceToScreenEdge())) {
            return getScreenHeight() - this.getScaledHeightWithBorder() - this.getMinDistanceToScreenEdge() - 1;
        }
        return Math.max(y, this.getMinDistanceToScreenEdge());
    }

    @NotNull
    protected SubMenuOpeningSide getPossibleSubMenuOpeningSide() {
        if (this.forceSide) {
            return this.subMenuOpeningSide;
        }
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

    public int getBorderThickness() {
        return 1;
    }

    public int getScaledBorderThickness() {
        float scale = UIBase.calculateFixedScale(this.scale);
        return (int)((float)this.getBorderThickness() * scale);
    }

    public int getX() {
        return this.rawX;
    }

    public int getY() {
        return this.rawY;
    }

    public int getWidth() {
        return this.rawWidth;
    }

    public int getWidthWithBorder() {
        return this.getWidth() + (this.getBorderThickness() * 2);
    }

    public int getScaledWidth() {
        float scale = UIBase.calculateFixedScale(this.scale);
        return (int) ((float)this.getWidth() * scale);
    }

    public int getScaledWidthWithBorder() {
        return this.getScaledWidth() + (this.getScaledBorderThickness() * 2);
    }

    public int getHeight() {
        return this.rawHeight;
    }

    public int getHeightWithBorder() {
        return this.getHeight() + (this.getBorderThickness() * 2);
    }

    public int getScaledHeight() {
        float scale = UIBase.calculateFixedScale(this.scale);
        return (int) ((float)this.getHeight() * scale);
    }

    public int getScaledHeightWithBorder() {
        return this.getScaledHeight() + (this.getScaledBorderThickness() * 2);
    }

    public boolean isHovered() {
        if (!this.isOpen()) return false;
        for (ContextMenuEntry<?> e : this.entries) {
            if (e.isHovered()) return true;
        }
        return false;
    }

    protected ContextMenu unhoverAllEntries() {
        for (ContextMenuEntry<?> e : this.entries) {
            e.setHovered(false);
        }
        return this;
    }

    public boolean hasShadow() {
        return this.shadow;
    }

    public ContextMenu setShadow(boolean shadow) {
        this.shadow = shadow;
        return this;
    }

    public boolean isForceDefaultTooltipStyle() {
        return this.forceDefaultTooltipStyle;
    }

    public ContextMenu setForceDefaultTooltipStyle(boolean forceDefaultTooltipStyle) {
        this.forceDefaultTooltipStyle = forceDefaultTooltipStyle;
        return this;
    }

    public boolean isKeepDistanceToEdges() {
        return this.keepDistanceToEdges;
    }

    public ContextMenu setKeepDistanceToEdges(boolean keepDistanceToEdges) {
        this.keepDistanceToEdges = keepDistanceToEdges;
        return this;
    }

    public boolean isForceRawXY() {
        return this.forceRawXY;
    }

    public ContextMenu setForceRawXY(boolean forceRawXY) {
        this.forceRawXY = forceRawXY;
        return this;
    }

    public boolean isForceSide() {
        return this.forceSide;
    }

    public ContextMenu setForceSide(boolean forceSide) {
        this.forceSide = forceSide;
        return this;
    }

    @NotNull
    public SubMenuOpeningSide getSubMenuOpeningSide() {
        return this.subMenuOpeningSide;
    }

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
        for (ContextMenuEntry<?> e : this.entries) {
            if (e instanceof SubMenuContextMenuEntry s) {
                if (s.subContextMenu.isHovered()) return true;
            }
        }
        return false;
    }

    public boolean isSubMenuOpen() {
        if (!this.isOpen()) return false;
        for (ContextMenuEntry<?> e : this.entries) {
            if (e instanceof SubMenuContextMenuEntry s) {
                if (s.subContextMenu.isOpen()) return true;
            }
        }
        return false;
    }

    public ContextMenu openMenuAt(int x, int y) {
        this.closeSubMenus();
        this.unhoverAllEntries();
        this.rawX = x;
        this.rawY = y;
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
        for (ContextMenuEntry<?> e : this.entries) {
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
        for (ContextMenuEntry<?> e : this.entries) {
            if (e instanceof SubMenuContextMenuEntry s) {
                if (s.subContextMenu.isUserNavigatingInMenu()) return true;
            }
        }
        return false;
    }

    @NotNull
    protected List<ContextMenuEntry<?>> getStackableEntries() {
        List<ContextMenuEntry<?>> l = new ArrayList<>();
        for (ContextMenuEntry<?> e : this.entries) {
            if (e.isStackable()) l.add(e);
        }
        return l;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.isUserNavigatingInMenu()) {
            float scale = UIBase.calculateFixedScale(this.scale);
            int scaledMouseX = (int) ((float)mouseX / scale);
            int scaledMouseY = (int) ((float)mouseY / scale);
            for (ContextMenuEntry<?> entry : this.entries) {
                entry.mouseClicked(scaledMouseX, scaledMouseY, button);
            }
            //Handle click for sub context menus
            for (ContextMenuEntry<?> e : this.entries) {
                if (e instanceof SubMenuContextMenuEntry s) {
                    s.subContextMenu.mouseClicked(mouseX, mouseY, button);
                }
            }
            return true;
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
            stacked.forceDefaultTooltipStyle = menusToStack[0].forceDefaultTooltipStyle;
            stacked.forceUIScale = menusToStack[0].forceUIScale;
            stacked.overriddenRenderScale = menusToStack[0].overriddenRenderScale;
            stacked.overriddenTooltipScale = menusToStack[0].overriddenTooltipScale;
            stacked.keepDistanceToEdges = menusToStack[0].keepDistanceToEdges;
            stacked.forceRawXY = menusToStack[0].forceRawXY;
            stacked.forceSide = menusToStack[0].forceSide;

            for (ContextMenuEntry<?> ignoredEntry : menusToStack[0].getStackableEntries()) {

                RuntimePropertyContainer stackProperties = new RuntimePropertyContainer();
                List<ContextMenuEntry<?>> entryStack = collectInstancesOfStackableEntryInMenus(ignoredEntry.identifier, menusToStack);
                if (!entryStack.isEmpty() && (entryStack.size() == menusToStack.length)) { // only stack entries if all menus have a stackable instance of it

                    ContextMenuEntry<?> firstOriginal = entryStack.get(0);
                    List<ContextMenuEntry<?>> entryStackCopyWithoutFirst = new ArrayList<>();
                    entryStack.forEach((entry) ->  {
                        if (entry != firstOriginal) entryStackCopyWithoutFirst.add(entry.copy());
                    });

                    ContextMenuEntry<?> first = firstOriginal.copy();
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
                        ContextMenuEntry<?> prev = first;
                        for (ContextMenuEntry<?> e2 : entryStackCopyWithoutFirst) {
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

    protected static List<ContextMenuEntry<?>> collectInstancesOfStackableEntryInMenus(String entryIdentifier, ContextMenu[] menus) {
        List<ContextMenuEntry<?>> entries = new ArrayList<>();
        for (ContextMenu m : menus) {
            ContextMenuEntry<?> e = m.getEntry(entryIdentifier);
            if ((e != null) && e.isStackable() && e.isActive()) {
                entries.add(e);
            }
        }
        return entries;
    }

    protected static List<ContextMenu> getSubContextMenusOfSubMenuEntries(List<ContextMenuEntry<?>> entries) {
        List<ContextMenu> l = new ArrayList<>();
        for (ContextMenuEntry<?> e : entries) {
            if (e instanceof SubMenuContextMenuEntry s) {
                l.add(s.subContextMenu);
            }
        }
        return l;
    }

    public static abstract class ContextMenuEntry<T extends ContextMenuEntry<T>> extends GuiComponent implements Renderable, GuiEventListener {

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
        protected BooleanSupplier visibleStateSupplier;
        @Nullable
        protected Supplier<Tooltip> tooltipSupplier;
        protected Font font = Minecraft.getInstance().font;
        protected boolean addSpaceForIcon = false;

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

        public T setHeight(int height) {
            this.height = height;
            return (T) this;
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

        public T setIsActiveSupplier(@Nullable BooleanSupplier activeStateSupplier) {
            this.activeStateSupplier = activeStateSupplier;
            return (T) this;
        }

        public boolean isVisible() {
            return (this.visibleStateSupplier == null) || this.visibleStateSupplier.getBoolean(this.parent, this);
        }

        public T setIsVisibleSupplier(@Nullable BooleanSupplier visibleStateSupplier) {
            this.visibleStateSupplier = visibleStateSupplier;
            return (T) this;
        }

        public T setTickAction(@Nullable EntryTask tickAction) {
            this.tickAction = tickAction;
            return (T) this;
        }

        public T setHoverAction(@Nullable EntryTask hoverAction) {
            this.hoverAction = hoverAction;
            return (T) this;
        }

        public T setTooltipSupplier(@Nullable Supplier<Tooltip> tooltipSupplier) {
            this.tooltipSupplier = tooltipSupplier;
            return (T) this;
        }

        @Nullable
        public Tooltip getTooltip() {
            return (this.tooltipSupplier != null) ? this.tooltipSupplier.get(this.parent, this) : null;
        }

        public T setStackable(boolean stackable) {
            this.getStackMeta().setStackable(stackable);
            return (T) this;
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

        public abstract ContextMenuEntry<?> copy();

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
            void run(ContextMenu menu, ContextMenuEntry<?> entry, boolean isPost);
        }

    }

    public static class ClickableContextMenuEntry<T extends ClickableContextMenuEntry<T>> extends ContextMenuEntry<T> {

        protected static final int ICON_WIDTH_HEIGHT = 10;

        @NotNull
        protected ClickAction clickAction;
        @NotNull
        protected Supplier<Component> labelSupplier;
        @Nullable
        protected Supplier<Component> shortcutTextSupplier;
        @Nullable
        protected ResourceLocation icon;
        protected boolean tooltipIconHovered = false;
        protected boolean tooltipActive = false;
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
            if ((this.icon != null) || this.addSpaceForIcon) labelX += 20;
            int labelY = this.y + (this.height / 2) - (this.font.lineHeight / 2);
            UIBase.drawElementLabel(pose, this.font, this.getLabel(), labelX, labelY, this.isActive() ? UIBase.getUIColorScheme().element_label_color_normal.getColorInt() : UIBase.getUIColorScheme().element_label_color_inactive.getColorInt());

            int shortcutTextWidth = 0;
            Component shortcutText = this.getShortcutText();
            if (shortcutText != null) {
                shortcutTextWidth = this.font.width(shortcutText);
                int shortcutX = this.x + this.width - 10 - shortcutTextWidth;
                UIBase.drawElementLabel(pose, this.font, shortcutText, shortcutX, labelY, this.isActive() ? UIBase.getUIColorScheme().element_label_color_normal.getColorInt() : UIBase.getUIColorScheme().element_label_color_inactive.getColorInt());
            }

            this.renderIcon(pose);

            this.renderTooltipIconAndRegisterTooltip(pose, mouseX, mouseY, (shortcutTextWidth > 0) ? -(shortcutTextWidth + 8) : 0);

        }

        protected void renderIcon(PoseStack pose) {
            if (this.icon != null) {
                RenderSystem.enableBlend();
                UIBase.getUIColorScheme().setUITextureShaderColor(1.0F);
                RenderUtils.bindTexture(this.icon);
                blit(pose, this.x + 10, this.y + (this.getHeight() / 2) - (ICON_WIDTH_HEIGHT / 2), 0.0F, 0.0F, ICON_WIDTH_HEIGHT, ICON_WIDTH_HEIGHT, ICON_WIDTH_HEIGHT, ICON_WIDTH_HEIGHT);
                UIBase.resetShaderColor();
            }
        }

        protected void renderTooltipIconAndRegisterTooltip(PoseStack pose, int mouseX, int mouseY, int offsetX) {

            Tooltip tooltip = this.getTooltip();

            if (tooltip != null) {

                this.tooltipIconHovered = this.isTooltipIconHovered(mouseX, mouseY, offsetX);
                if (this.tooltipIconHovered) {
                    if (this.tooltipIconHoverStart == -1) {
                        this.tooltipIconHoverStart = System.currentTimeMillis();
                    }
                } else {
                    this.tooltipIconHoverStart = -1;
                }
                this.tooltipActive = (this.tooltipIconHoverStart != -1) && ((this.tooltipIconHoverStart + 200) < System.currentTimeMillis());

                RenderSystem.enableBlend();
                UIBase.getUIColorScheme().setUITextureShaderColor(this.tooltipIconHovered ? 1.0F : 0.2F);
                RenderUtils.bindTexture(CONTEXT_MENU_TOOLTIP_ICON);
                blit(pose, this.getTooltipIconX() + offsetX, this.getTooltipIconY(), 0.0F, 0.0F, 10, 10, 10, 10);
                UIBase.resetShaderColor();

                if (this.tooltipActive) {
                    if (this.parent.isForceDefaultTooltipStyle()) {
                        tooltip.setDefaultStyle();
                    }
                    float tooltipScale = (this.parent.overriddenTooltipScale != null) ? this.parent.overriddenTooltipScale : this.parent.scale;
                    tooltip.setScale(tooltipScale);
                    TooltipHandler.INSTANCE.addTooltip(tooltip, () ->this.tooltipActive, false, true);
                }

            } else {
                this.tooltipIconHovered = false;
                this.tooltipActive = false;
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
                fill(pose, this.x, this.y, this.x + this.width, this.y + this.height, UIBase.getUIColorScheme().element_background_color_hover.getColorInt());
            }
        }

        @Nullable
        public ResourceLocation getIcon() {
            return this.icon;
        }

        public T setIcon(@Nullable ResourceLocation icon) {
            this.icon = icon;
            return (T) this;
        }

        @NotNull
        public T setLabelSupplier(@NotNull Supplier<Component> labelSupplier) {
            Objects.requireNonNull(labelSupplier);
            this.labelSupplier = labelSupplier;
            return (T) this;
        }

        @NotNull
        public Component getLabel() {
            Component c = this.labelSupplier.get(this.parent, this);
            Objects.requireNonNull(c);
            return c;
        }

        @NotNull
        public T setClickAction(@NotNull ClickAction clickAction) {
            Objects.requireNonNull(clickAction);
            this.clickAction = clickAction;
            return (T) this;
        }

        @Nullable
        public Component getShortcutText() {
            return (this.shortcutTextSupplier != null) ? this.shortcutTextSupplier.get(this.parent, this) : null;
        }

        @NotNull
        public T setShortcutTextSupplier(@Nullable Supplier<Component> shortcutTextSupplier) {
            this.shortcutTextSupplier = shortcutTextSupplier;
            return (T) this;
        }

        @Override
        public ClickableContextMenuEntry<T> copy() {
            ClickableContextMenuEntry<T> copy = new ClickableContextMenuEntry<>(this.identifier, this.parent, Component.literal(""), this.clickAction);
            copy.shortcutTextSupplier = this.shortcutTextSupplier;
            copy.labelSupplier = this.labelSupplier;
            copy.height = this.height;
            copy.tickAction = this.tickAction;
            copy.tooltipSupplier = this.tooltipSupplier;
            copy.activeStateSupplier = this.activeStateSupplier;
            copy.icon = this.icon;
            return copy;
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
            if ((this.icon != null) || this.addSpaceForIcon) {
                i += 20;
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
            if ((button == 0) && this.isHovered() && this.isActive() && !this.parent.isSubMenuHovered() && !this.tooltipIconHovered) {
                if (FancyMenu.getOptions().playUiClickSounds.getValue()) {
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                }
                this.clickAction.onClick(this.parent, this);
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @FunctionalInterface
        public interface ClickAction {
            void onClick(ContextMenu menu, ClickableContextMenuEntry<?> entry);
        }

    }

    public static class ValueCycleContextMenuEntry<V> extends ClickableContextMenuEntry<ValueCycleContextMenuEntry<V>> {

        protected final ILocalizedValueCycle<V> valueCycle;

        public ValueCycleContextMenuEntry(@NotNull String identifier, @NotNull ContextMenu parent, @NotNull ILocalizedValueCycle<V> valueCycle) {
            super(identifier, parent, Component.empty(), (menu, entry) -> valueCycle.next());
            this.valueCycle = valueCycle;
            this.labelSupplier = (menu, entry) -> this.valueCycle.getCycleComponent();
        }

        @NotNull
        public ILocalizedValueCycle<V> getValueCycle() {
            return this.valueCycle;
        }

        @Override
        public @NotNull ValueCycleContextMenuEntry<V> setLabelSupplier(@NotNull Supplier<Component> labelSupplier) {
            LOGGER.error("[FANCYMENU] You can't set the label of ValueCycleContextMenuEntries!");
            return this;
        }

        @Override
        public @NotNull ValueCycleContextMenuEntry<V> setClickAction(@NotNull ClickAction clickAction) {
            LOGGER.error("[FANCYMENU] You can't set the click action of ValueCycleContextMenuEntries!");
            return this;
        }

        @Override
        public ValueCycleContextMenuEntry<V> copy() {
            ValueCycleContextMenuEntry<V> copy = new ValueCycleContextMenuEntry<>(this.identifier, this.parent, this.valueCycle);
            copy.shortcutTextSupplier = this.shortcutTextSupplier;
            copy.labelSupplier = this.labelSupplier;
            copy.height = this.height;
            copy.tickAction = this.tickAction;
            copy.tooltipSupplier = this.tooltipSupplier;
            copy.activeStateSupplier = this.activeStateSupplier;
            copy.icon = this.icon;
            return copy;
        }

    }

    public static class SubMenuContextMenuEntry extends ClickableContextMenuEntry<SubMenuContextMenuEntry> {

        @NotNull
        protected ContextMenu subContextMenu;
        protected boolean subMenuHoverTicked = false;
        protected boolean subMenuHoveredAfterOpen = false;
        protected long parentMenuHoverStartTime = -1;
        protected long entryHoverStartTime = -1;
        protected long entryNotHoveredStartTime = -1;

        public SubMenuContextMenuEntry(@NotNull String identifier, @NotNull ContextMenu parent, @NotNull Component label, @NotNull ContextMenu subContextMenu) {
            super(identifier, parent, label, ((menu, entry) -> {}));
            this.subContextMenu = subContextMenu;
            this.subContextMenu.parentEntry = this;
            this.subContextMenu.forceDefaultTooltipStyle = parent.forceDefaultTooltipStyle;
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
            UIBase.getUIColorScheme().setUITextureShaderColor(1.0F);
            RenderUtils.bindTexture(SUB_CONTEXT_MENU_ARROW_ICON);
            blit(pose, this.x + this.width - 20, this.y + 5, 0.0F, 0.0F, 10, 10, 10, 10);
            UIBase.resetShaderColor();
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
                if (this.parentMenuHoverStartTime == -1) {
                    this.parentMenuHoverStartTime = System.currentTimeMillis();
                }
                if ((this.parentMenuHoverStartTime + 400) < System.currentTimeMillis()) {
                    this.subContextMenu.closeMenu();
                }
            } else {
                this.parentMenuHoverStartTime = -1;
            }
            //Open sub menu on entry hover
            if (this.isActive() && this.isHovered() && !this.parent.isSubMenuHovered() && !this.tooltipIconHovered) {
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
            //Close sub menu if tooltip is active
            if (this.tooltipActive && this.subContextMenu.isOpen()) {
                this.subContextMenu.closeMenu();
            }
        }

        public void openSubMenu() {
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
            this.subContextMenu.forceDefaultTooltipStyle = this.parent.forceDefaultTooltipStyle;
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
            copy.icon = this.icon;
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

    public static class SeparatorContextMenuEntry extends ContextMenuEntry<SeparatorContextMenuEntry> {

        public SeparatorContextMenuEntry(@NotNull String identifier, @NotNull ContextMenu parent) {
            super(identifier, parent);
            this.height = 9;
        }

        @Override
        public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
            fill(pose, this.x + 10, this.y + 4, this.x + this.width - 10, this.y + 5, UIBase.getUIColorScheme().context_menu_border_color.getColorInt());
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
            int i = 20;
            if (this.addSpaceForIcon) i += 20;
            return i;
        }

        @Override
        public void setFocused(boolean var1) {
        }

        @Override
        public boolean isFocused() {
            return false;
        }

    }

    public static class SpacerContextMenuEntry extends ContextMenuEntry<SpacerContextMenuEntry> {

        public SpacerContextMenuEntry(@NotNull String identifier, @NotNull ContextMenu parent) {
            super(identifier, parent);
            this.height = 4;
        }

        @Override
        public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
        }

        @Override
        public int getMinWidth() {
            int i = 20;
            if (this.addSpaceForIcon) i += 20;
            return i;
        }

        @Override
        public SpacerContextMenuEntry copy() {
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
        protected ContextMenuEntry<?> nextInStack;

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
        T get(ContextMenu menu, ContextMenuEntry<?> entry);
    }

    @FunctionalInterface
    public interface BooleanSupplier extends Supplier<Boolean> {

        default boolean getBoolean(ContextMenu menu, ContextMenuEntry<?> entry) {
            Boolean b = this.get(menu, entry);
            if (b != null) {
                return b;
            }
            return false;
        }

    }

    public static class IconFactory {
        @NotNull
        public static ResourceLocation getIcon(@NotNull String iconName) {
            return new ResourceLocation("fancymenu", "textures/contextmenu/icons/" + iconName + ".png");
        }
    }

}