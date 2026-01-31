package de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.math.Axis;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.util.cycle.ILocalizedValueCycle;
import de.keksuccino.fancymenu.util.properties.RuntimePropertyContainer;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.GuiBlurRenderer;
import de.keksuccino.fancymenu.util.rendering.IconAnimation;
import de.keksuccino.fancymenu.util.rendering.IconAnimations;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.SmoothRectangleRenderer;
import de.keksuccino.fancymenu.util.rendering.ui.FancyMenuUiComponent;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcon;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.TooltipHandler;
import de.keksuccino.fancymenu.util.rendering.ui.widget.NavigatableWidget;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.ExtendedEditBox;
import de.keksuccino.konkrete.input.MouseInput;
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
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@SuppressWarnings("all")
public class ContextMenu implements Renderable, GuiEventListener, NarratableEntry, NavigatableWidget, FancyMenuUiComponent {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final MaterialIcon SUB_CONTEXT_MENU_ARROW_ICON = MaterialIcons.CHEVRON_RIGHT;
    private static final ResourceLocation SCROLL_UP_ARROW = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/contextmenu/scroll_up_arrow.png");
    private static final ResourceLocation SCROLL_DOWN_ARROW = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/contextmenu/scroll_down_arrow.png");
    private static final MaterialIcon CONTEXT_MENU_TOOLTIP_ICON = MaterialIcons.INFO;
    private static final DrawableColor SHADOW_COLOR = DrawableColor.of(new Color(43, 43, 43, 100));
    private static final int SCROLL_INDICATOR_HEIGHT = 12; // Space reserved for arrows
    private static final String SEARCH_ENTRY_IDENTIFIER = "context_menu_search";
    private static final String SEARCH_SEPARATOR_IDENTIFIER = "context_menu_search_separator";

    protected final List<ContextMenuEntry<?>> entries = new ArrayList<>();
    protected float scale = UIBase.getUIScale();
    protected boolean forceUIScale = true;
    protected boolean open = false;
    protected float rawX; // without border
    protected float rawY; // without border
    protected float rawWidth; // without border
    protected float rawHeight; // without border
    protected SubMenuContextMenuEntry parentEntry = null;
    protected SubMenuOpeningSide subMenuOpeningSide = SubMenuOpeningSide.RIGHT;
    protected boolean shadow = false;
    protected boolean keepDistanceToEdges = true;
    protected boolean forceRawXY = false;
    protected boolean forceSide = false;
    protected boolean forceSideSubMenus = true;
    protected boolean roundTopLeftCorner = true;
    protected boolean roundTopRightCorner = true;
    protected boolean roundBottomLeftCorner = true;
    protected boolean roundBottomRightCorner = true;
    protected boolean openAnimationEnabled = true;
    protected float scrollPosition = 0.0f; // Current scroll position
    private boolean needsScrolling = false; // Flag to track if menu is scrollable
    private float displayHeight = 0; // Adjusted height when scrollable
    private static final float OPEN_ANIMATION_GROW_TIME_MS = 120.0F;
    private static final float OPEN_ANIMATION_MIN_SCALE = 0.78F;
    private long openAnimationStartMs = 0L;
    private boolean openAnimationActive = false;
    protected int renderMouseX = 0;
    protected int renderMouseY = 0;
    private final SearchContextMenuEntry searchEntry;
    private final SeparatorContextMenuEntry searchSeparator;
    private boolean searchEntryRequested = false;
    private boolean searchEntryVisibleLast = false;
    private boolean alwaysShowSearchBar = false;
    private ContextMenu cachedSearchMenu = null;

    public ContextMenu() {
        this.searchEntry = new SearchContextMenuEntry(SEARCH_ENTRY_IDENTIFIER, this);
        this.searchSeparator = new SeparatorContextMenuEntry(SEARCH_SEPARATOR_IDENTIFIER, this);
        this.entries.add(this.searchEntry);
        this.entries.add(this.searchSeparator);
        this.searchEntry.addIsVisibleSupplier((menu, entry) -> this.isSearchEntryVisible());
        this.searchSeparator.addIsVisibleSupplier((menu, entry) -> this.isSearchEntryVisible());
        this.searchEntryVisibleLast = this.isSearchEntryVisible();
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        this.renderMouseX = mouseX;
        this.renderMouseY = mouseY;

        if (!this.isOpen()) return;

        this.updateSearchVisibilityState(false);

        if (this.forceUIScale) this.scale = UIBase.getUIScale();

        boolean animationsEnabled = UIBase.shouldPlayAnimations() && this.openAnimationEnabled;
        boolean openingAnimation = animationsEnabled && this.isTopLevelOpenAnimationRunning();
        float uiScale = UIBase.calculateFixedScale(this.getScale());
        float animationScale = animationsEnabled ? this.getOpenAnimationScale(partial) : 1.0F;
        float renderScale = uiScale * animationScale;

        RenderSystem.disableDepthTest();
        RenderingUtils.setDepthTestLocked(true);

        RenderSystem.enableBlend();
        graphics.pose().pushPose();
        graphics.pose().scale(renderScale, renderScale, renderScale);

        List<ContextMenuEntry<?>> renderEntries = new ArrayList<>();
        renderEntries.add(new SpacerContextMenuEntry("unregistered_spacer_top", this));

        //Check if icon space should get added to entries
        boolean addIconSpace = this.shouldAddIconSpaceForEntries();

        this.rawWidth = 20;
        this.rawHeight = 0;

        String searchText = this.getActiveSearchText();
        String searchLower = (searchText != null) ? searchText.toLowerCase(Locale.ROOT) : null;
        boolean filterActive = (searchLower != null) && !searchLower.isBlank();
        List<ContextMenuEntry<?>> visibleEntries = new ArrayList<>();
        for (ContextMenuEntry<?> e : this.entries) {
            e.addSpaceForIcon = addIconSpace;
            if (e.isVisible() && (!filterActive || this.matchesSearchFilter(e, searchLower))) {
                visibleEntries.add(e);
            }
        }
        for (ContextMenuEntry<?> e : this.entries) {
            if (!visibleEntries.contains(e)) {
                e.setHovered(false);
            }
        }

        int startIndex = 0;
        int endIndex = visibleEntries.size() - 1;
        while (startIndex <= endIndex && visibleEntries.get(startIndex) instanceof SeparatorContextMenuEntry) {
            startIndex++;
        }
        while (endIndex >= startIndex && visibleEntries.get(endIndex) instanceof SeparatorContextMenuEntry && visibleEntries.get(endIndex) != this.searchSeparator) {
            endIndex--;
        }

        ContextMenuEntry<?> prev = null;
        for (int i = startIndex; i <= endIndex; i++) {
            ContextMenuEntry<?> e = visibleEntries.get(i);
            //Merge separator entries when they would render in a row
            if (e instanceof SeparatorContextMenuEntry && prev instanceof SeparatorContextMenuEntry) {
                continue;
            }
            //Pre-tick
            if (e.tickAction != null) {
                e.tickAction.run(this, e, false);
            }
            //Update width + height
            float w = e.getMinWidth();
            if (w > this.rawWidth) {
                this.rawWidth = w;
            }
            this.rawHeight += e.getHeight();
            renderEntries.add(e);
            prev = e;
        }
        this.rawHeight += 8; //add top and bottom spacer to total height

        renderEntries.add(new SpacerContextMenuEntry("unregistered_spacer_bottom", this));

        // Calculate max height considering both menu scale and GUI scale
        double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
        float menuScale = UIBase.calculateFixedScale(this.scale);
        float maxMenuHeight = (getScreenHeight() / menuScale) * 0.7f;

        this.needsScrolling = this.rawHeight > maxMenuHeight;

        // If scrollable, adjust displayed height
        this.displayHeight = needsScrolling ? maxMenuHeight : this.rawHeight;
        boolean renderContent = !openingAnimation;

        float x = this.getActualX();
        float y = this.getActualY();
        float scaledX = (float)((float)x/ renderScale) + this.getBorderThickness();
        float scaledY = (float)((float)y/ renderScale) + this.getBorderThickness();
        float scaledMouseX = (float) ((float)mouseX / renderScale);
        float scaledMouseY = (float) ((float)mouseY / renderScale);
        boolean navigatingInSub = this.isUserNavigatingInSubMenu();
        float normalRoundingRadius = UIBase.getInterfaceCornerRoundingRadius();
        float normalCornerTopLeft = this.roundTopLeftCorner ? normalRoundingRadius : 0.0F;
        float normalCornerTopRight = this.roundTopRightCorner ? normalRoundingRadius : 0.0F;
        float normalCornerBottomLeft = this.roundBottomLeftCorner ? normalRoundingRadius : 0.0F;
        float normalCornerBottomRight = this.roundBottomRightCorner ? normalRoundingRadius : 0.0F;
        float smoothScale = renderScale;
        float smoothX = scaledX * smoothScale;
        float smoothY = scaledY * smoothScale;
        float smoothWidth = this.getWidth() * smoothScale;
        float smoothHeight = displayHeight * smoothScale;
        float smoothCornerTopLeft = normalCornerTopLeft * smoothScale;
        float smoothCornerTopRight = normalCornerTopRight * smoothScale;
        float smoothCornerBottomLeft = normalCornerBottomLeft * smoothScale;
        float smoothCornerBottomRight = normalCornerBottomRight * smoothScale;

        //Render shadow
        if (this.hasShadow()) {
            SmoothRectangleRenderer.renderSmoothRectRoundAllCorners(
                    graphics,
                    (scaledX + 4.0F) * smoothScale,
                    (scaledY + 4.0F) * smoothScale,
                    smoothWidth,
                    smoothHeight,
                    smoothCornerTopLeft,
                    smoothCornerTopRight,
                    smoothCornerBottomRight,
                    smoothCornerBottomLeft,
                    SHADOW_COLOR.getColorInt(),
                    partial
            );
        }

        if (UIBase.shouldBlur()) {
            // Render blur background
            float blurX = smoothX;
            float blurY = smoothY;
            float blurWidth = smoothWidth;
            float blurHeight = smoothHeight;
            if (blurWidth > 0.0F && blurHeight > 0.0F) {
                GuiBlurRenderer.renderBlurAreaWithIntensityRoundAllCorners(
                        graphics,
                        blurX,
                        blurY,
                        blurWidth,
                        blurHeight,
                        UIBase.getBlurRadius(),
                        smoothCornerTopLeft,
                        smoothCornerTopRight,
                        smoothCornerBottomRight,
                        smoothCornerBottomLeft,
                        UIBase.getUITheme().ui_blur_overlay_background_tint,
                        partial
                );
            }
        } else {
            //Render normal background
            SmoothRectangleRenderer.renderSmoothRectRoundAllCorners(
                    graphics,
                    smoothX,
                    smoothY,
                    smoothWidth,
                    smoothHeight,
                    smoothCornerTopLeft,
                    smoothCornerTopRight,
                    smoothCornerBottomRight,
                    smoothCornerBottomLeft,
                    UIBase.getUITheme().ui_overlay_background_color.getColorInt(),
                    partial
            );
        }

        // Enable scissoring if scrollable
        if (needsScrolling && renderContent) {
            // Calculate scissor boundaries IN THE SCALED CONTEXT
            float scissorTopInScaledContext = scaledY + SCROLL_INDICATOR_HEIGHT;
            float scissorBottomInScaledContext = scaledY + displayHeight - SCROLL_INDICATOR_HEIGHT;
            float scissorLeftInScaledContext = scaledX;
            float scissorRightInScaledContext = scaledX + this.getWidth();

            // Convert coordinates from the scaled context to the UNscaled logical GUI space
            // The 'renderScale' variable is the one used in graphics.pose().scale()
            float logicalMinX = scissorLeftInScaledContext * renderScale;
            float logicalMinY = scissorTopInScaledContext * renderScale;
            float logicalMaxX = scissorRightInScaledContext * renderScale;
            float logicalMaxY = scissorBottomInScaledContext * renderScale;

            // enableScissor expects unscaled logical GUI coordinates
            graphics.enableScissor((int)logicalMinX, (int)logicalMinY, (int)logicalMaxX, (int)logicalMaxY);
        }

        //Update + render entries
        if (renderContent) {
            float entryY = scaledY;
            if (needsScrolling) {
                // Add space for scroll indicator and apply scroll position
                entryY += SCROLL_INDICATOR_HEIGHT - scrollPosition;
            }

            for (ContextMenuEntry<?> e : renderEntries) {
                e.x = scaledX;
                e.y = entryY; //already scaled
                e.width = this.getWidth(); //don't scale, because already scaled via graphics.pose().scale()

                boolean isVisible = true;
                if (needsScrolling) {
                    // Check if entry is visible in the scrollable area
                    float entryBottom = entryY + e.getHeight();
                    float visibleTop = scaledY + SCROLL_INDICATOR_HEIGHT;
                    float visibleBottom = scaledY + displayHeight - SCROLL_INDICATOR_HEIGHT;

                    // Entry is visible if it's at least partially within the visible area
                    isVisible = (entryY < visibleBottom && entryBottom > visibleTop);
                }

                boolean hover = e.isHovered();
                // Only set hover if the entry is visible in the scroll area
                e.setHovered(isVisible && !navigatingInSub && UIBase.isXYInArea(scaledMouseX, scaledMouseY, e.x, e.y, e.width, e.getHeight()));

                //Run hover action of element if its hover state changed to hovered
                if (!hover && e.isHovered() && (e.hoverAction != null)) {
                    e.hoverAction.run(this, e, false);
                }

                // Only render if visible
                if (isVisible) {
                    RenderSystem.enableBlend();
                    e.render(graphics, (int) scaledMouseX, (int) scaledMouseY, partial);
                }

                entryY += e.getHeight(); //don't scale this, because already scaled via graphics.pose().scale()
            }
        } else {
            this.unhoverAllEntries();
        }

        // Disable scissoring and render arrow indicators if needed
        if (needsScrolling && renderContent) {
            graphics.disableScissor();

            // Calculate max scroll position
            float maxScrollPosition = this.rawHeight - (displayHeight - SCROLL_INDICATOR_HEIGHT * 2);

            // Create a darker version of the background color (about 10% darker)
            Color bgColor = UIBase.shouldBlur() ? UIBase.getUITheme().ui_blur_overlay_background_tint.getColor() : UIBase.getUITheme().ui_overlay_border_color.getColor();
            Color darkerBgColor = new Color(
                    Math.max(0, (int)(bgColor.getRed() * 0.9)),
                    Math.max(0, (int)(bgColor.getGreen() * 0.9)),
                    Math.max(0, (int)(bgColor.getBlue() * 0.9)),
                    bgColor.getAlpha()
            );
            int darkerBackgroundColor = darkerBgColor.getRGB();

            // Render up arrow background and arrow if scrolled down
            if (scrollPosition > 0) {
                // Fill background with rounded top corners
                SmoothRectangleRenderer.renderSmoothRectRoundAllCorners(
                        graphics,
                        smoothX,
                        smoothY,
                        smoothWidth,
                        SCROLL_INDICATOR_HEIGHT * smoothScale,
                        smoothCornerTopLeft,
                        smoothCornerTopRight,
                        0.0F,
                        0.0F,
                        darkerBackgroundColor,
                        partial
                );

                // Render arrow centered
                RenderSystem.enableBlend();
                UIBase.getUITheme().setUITextureShaderColor(graphics, 1.0F);
                graphics.blit(
                        SCROLL_UP_ARROW,
                        (int)(scaledX + this.getWidth()/2 - 5),
                        (int)(scaledY + (SCROLL_INDICATOR_HEIGHT - 10) / 2), // Center vertically
                        0.0F, 0.0F, 10, 10, 10, 10
                );
                RenderingUtils.resetShaderColor(graphics);
            }

            // Render down arrow background and arrow if can scroll further
            if (scrollPosition < maxScrollPosition) {
                // Fill background with rounded bottom corners
                SmoothRectangleRenderer.renderSmoothRectRoundAllCorners(
                        graphics,
                        smoothX,
                        (scaledY + displayHeight - SCROLL_INDICATOR_HEIGHT) * smoothScale,
                        smoothWidth,
                        SCROLL_INDICATOR_HEIGHT * smoothScale,
                        0.0F,
                        0.0F,
                        smoothCornerBottomRight,
                        smoothCornerBottomLeft,
                        darkerBackgroundColor,
                        partial
                );

                // Render arrow centered (with fixed position)
                RenderSystem.enableBlend();
                UIBase.getUITheme().setUITextureShaderColor(graphics, 1.0F);
                graphics.blit(
                        SCROLL_DOWN_ARROW,
                        (int)(scaledX + this.getWidth()/2 - 5),
                        (int)(scaledY + displayHeight - SCROLL_INDICATOR_HEIGHT + (SCROLL_INDICATOR_HEIGHT - 10) / 2), // Centered in area
                        0.0F, 0.0F, 10, 10, 10, 10
                );
                RenderingUtils.resetShaderColor(graphics);
            }
        }

        //Render border
        float smoothBorderThickness = this.getBorderThickness() * smoothScale;
        float smoothBorderCornerTopLeft = normalCornerTopLeft > 0.0F ? (normalCornerTopLeft + this.getBorderThickness()) * smoothScale : 0.0F;
        float smoothBorderCornerTopRight = normalCornerTopRight > 0.0F ? (normalCornerTopRight + this.getBorderThickness()) * smoothScale : 0.0F;
        float smoothBorderCornerBottomRight = normalCornerBottomRight > 0.0F ? (normalCornerBottomRight + this.getBorderThickness()) * smoothScale : 0.0F;
        float smoothBorderCornerBottomLeft = normalCornerBottomLeft > 0.0F ? (normalCornerBottomLeft + this.getBorderThickness()) * smoothScale : 0.0F;
        SmoothRectangleRenderer.renderSmoothBorderRoundAllCorners(
                graphics,
                (scaledX - this.getBorderThickness()) * smoothScale,
                (scaledY - this.getBorderThickness()) * smoothScale,
                (this.getWidth() + (this.getBorderThickness() * 2.0F)) * smoothScale,
                (displayHeight + (this.getBorderThickness() * 2.0F)) * smoothScale,
                smoothBorderThickness,
                smoothBorderCornerTopLeft,
                smoothBorderCornerTopRight,
                smoothBorderCornerBottomRight,
                smoothBorderCornerBottomLeft,
                UIBase.shouldBlur() ? UIBase.getUITheme().ui_blur_overlay_border_color.getColorInt() : UIBase.getUITheme().ui_overlay_border_color.getColorInt(),
                partial
        );

        //Post-tick
        for (ContextMenuEntry<?> e : renderEntries) {
            if (e.tickAction != null) {
                e.tickAction.run(this, e, true);
            }
        }

        graphics.pose().popPose();

        RenderingUtils.setDepthTestLocked(false);

        //Render sub context menus
        for (ContextMenuEntry<?> e : renderEntries) {
            if (e instanceof SubMenuContextMenuEntry s) {
                if (this.forceSideSubMenus) s.subContextMenu.forceSide = this.forceSide;
                s.subContextMenu.forceRawXY = this.forceRawXY;
                s.subContextMenu.shadow = this.shadow;
                s.subContextMenu.scale = this.scale;
                s.subContextMenu.forceUIScale = this.forceUIScale;
                s.subContextMenu.render(graphics, mouseX, mouseY, partial);
            }
        }

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
        return this.addEntry(e).setStackable(true);
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
        if ((entry instanceof SearchContextMenuEntry) && (entry != this.searchEntry)) {
            LOGGER.error("[FANCYMENU] Failed to add ContextMenu search entry! Only one search entry is allowed per menu.");
            return entry;
        }
        if (this.hasEntry(entry.identifier)) {
            LOGGER.error("[FANCYMENU] Failed to add ContextMenu entry! Identifier already in use: " + entry.identifier);
        } else {
            if (!this.isProtectedEntry(entry)) {
                int minIndex = this.getProtectedEntriesCount();
                if (index < minIndex) index = minIndex;
            }
            this.entries.add(index, entry);
        }
        return entry;
    }

    public ContextMenu removeEntry(String identifier) {
        if (this.isProtectedEntryIdentifier(identifier)) {
            LOGGER.error("[FANCYMENU] Failed to remove ContextMenu entry! Entry is protected: " + identifier);
            return this;
        }
        ContextMenuEntry<?> e = this.getEntry(identifier);
        if (e != null) {
            this.entries.remove(e);
            e.onRemoved();
        }
        return this;
    }

    public ContextMenu clearEntries() {
        this.closeMenu();
        List<ContextMenuEntry<?>> entriesToRemove = new ArrayList<>(this.entries);
        for (ContextMenuEntry<?> e : entriesToRemove) {
            if (this.isProtectedEntry(e)) continue;
            this.entries.remove(e);
            e.onRemoved();
        }
        this.resetSearchState();
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

    public boolean isOpenAnimationEnabled() {
        return this.openAnimationEnabled;
    }

    public ContextMenu setOpenAnimationEnabled(boolean openAnimationEnabled) {
        this.openAnimationEnabled = openAnimationEnabled;
        return this;
    }

    public ContextMenu setScale(float scale) {
        if (this.forceUIScale) LOGGER.error("[FANCYMENU] Unable to set scale of ContextMenu while ContextMenu#isForceUIScale()!");
        this.scale = scale;
        return this;
    }

    public boolean isForceUIScale() {
        return this.forceUIScale;
    }

    public ContextMenu setForceUIScale(boolean forceUIScale) {
        this.forceUIScale = forceUIScale;
        return this;
    }

    public boolean isAlwaysShowSearchBar() {
        return this.alwaysShowSearchBar;
    }

    public ContextMenu setAlwaysShowSearchBar(boolean alwaysShowSearchBar) {
        this.alwaysShowSearchBar = alwaysShowSearchBar;
        this.updateSearchVisibilityState(false);
        return this;
    }

    private boolean isTopLevelOpenAnimationRunning() {
        if (!UIBase.shouldPlayAnimations() || !this.openAnimationEnabled) return false;
        if (this.isSubMenu() || !this.openAnimationActive) return false;
        float elapsedMs = (float) (net.minecraft.Util.getMillis() - this.openAnimationStartMs);
        if (elapsedMs >= OPEN_ANIMATION_GROW_TIME_MS) {
            this.openAnimationActive = false;
            return false;
        }
        return true;
    }

    private float getOpenAnimationScale(float partial) {
        if (!UIBase.shouldPlayAnimations() || !this.openAnimationEnabled) return 1.0F;
        if (this.isSubMenu() || !this.isOpen()) return 1.0F;
        if (!this.isTopLevelOpenAnimationRunning()) return 1.0F;

        float elapsedMs = (float) (net.minecraft.Util.getMillis() - this.openAnimationStartMs);
        float growT = Math.min(elapsedMs / OPEN_ANIMATION_GROW_TIME_MS, 1.0F);
        // Ease-out cubic for a quick pop
        float easedGrow = 1.0F - (float) Math.pow(1.0F - growT, 3);
        float baseScale = OPEN_ANIMATION_MIN_SCALE + (1.0F - OPEN_ANIMATION_MIN_SCALE) * easedGrow;
        return Math.max(baseScale, 0.01F);
    }

    protected float getMinDistanceToScreenEdge() {
        if (!this.keepDistanceToEdges) return 0;
        return 5;
    }

    protected float getActualX() {
        if (this.isSubMenu()) {
            float cachedScale = this.parentEntry.parent.scale;
            float scale = UIBase.calculateFixedScale(this.scale);
            float scaledOffsetX = (float) (5.0F * scale);
            SubMenuOpeningSide side = this.getPossibleSubMenuOpeningSide();
            if (side == SubMenuOpeningSide.LEFT) {
                float actualX = this.parentEntry.parent.getActualX() - this.getScaledWidth() + scaledOffsetX;
                this.parentEntry.parent.scale = cachedScale;
                return actualX;
            }
            if (side == SubMenuOpeningSide.RIGHT) {
                float actualX = this.parentEntry.parent.getActualX() + this.parentEntry.parent.getScaledWidth() - scaledOffsetX;
                this.parentEntry.parent.scale = cachedScale;
                return actualX;
            }
        }
        if (this.forceRawXY) {
            return this.getX();
        }
        //Force the menu to stay on screen
        if ((this.getX() + this.getScaledWidthWithBorder()) >= (getScreenWidth() - this.getMinDistanceToScreenEdge())) {
            return getScreenWidth() - this.getScaledWidthWithBorder() - this.getMinDistanceToScreenEdge() - 1;
        }
        return Math.max(this.getX(), this.getMinDistanceToScreenEdge());
    }

    protected float getActualY() {
        float y = this.getY();
        if (this.isSubMenu()) {
            float scale = UIBase.calculateFixedScale(this.scale);
            int scaledOffsetY = (int) (10.0F * scale);
            y = (float) ((float)this.parentEntry.y * scale);
            y += scaledOffsetY;
        }
        if (this.forceRawXY) {
            return y;
        }

        // Calculate the actual height to use for positioning considering scaling
        float menuScale = UIBase.calculateFixedScale(this.scale);
        float heightToUse;

        if (this.needsScrolling) {
            // Use the actual display height that's determined during rendering
            // This is already capped at 70% of screen height in logical coordinates
            heightToUse = this.displayHeight * menuScale + this.getBorderThickness() * 2 * menuScale;
        } else {
            heightToUse = this.getScaledHeightWithBorder();
        }

        // Make sure the menu stays fully on screen
        if ((y + heightToUse) >= (getScreenHeight() - this.getMinDistanceToScreenEdge())) {
            return getScreenHeight() - heightToUse - this.getMinDistanceToScreenEdge() - 1;
        }

        return Math.max(y, this.getMinDistanceToScreenEdge());
    }

    @NotNull
    protected SubMenuOpeningSide getPossibleSubMenuOpeningSide() {
        if (this.forceSide) {
            return this.subMenuOpeningSide;
        }
        if (this.isSubMenu()) {
            float potentialX = this.parentEntry.parent.getActualX() - this.getScaledWidth() + 5;
            if ((this.subMenuOpeningSide == SubMenuOpeningSide.LEFT) && (potentialX < 5)) {
                return SubMenuOpeningSide.RIGHT;
            }
            potentialX = this.parentEntry.parent.getActualX() + this.parentEntry.parent.getScaledWidth() - 5 + this.getScaledWidth();
            if ((this.subMenuOpeningSide == SubMenuOpeningSide.RIGHT) && (potentialX > (getScreenWidth() - 5))) {
                return SubMenuOpeningSide.LEFT;
            }
        }
        return this.subMenuOpeningSide;
    }

    public float getBorderThickness() {
        return 1;
    }

    public float getScaledBorderThickness() {
        float scale = UIBase.calculateFixedScale(this.scale);
        return (float)((float)this.getBorderThickness() * scale);
    }

    public float getX() {
        return this.rawX;
    }

    public float getY() {
        return this.rawY;
    }

    public float getWidth() {
        return this.rawWidth;
    }

    public float getWidthWithBorder() {
        return this.getWidth() + (this.getBorderThickness() * 2);
    }

    public float getScaledWidth() {
        float scale = UIBase.calculateFixedScale(this.scale);
        return (float) ((float)this.getWidth() * scale);
    }

    public float getScaledWidthWithBorder() {
        return this.getScaledWidth() + (this.getScaledBorderThickness() * 2);
    }

    public float getHeight() {
        return this.rawHeight;
    }

    public float getHeightWithBorder() {
        return this.getHeight() + (this.getBorderThickness() * 2);
    }

    public float getScaledHeight() {
        float scale = UIBase.calculateFixedScale(this.scale);
        if (this.needsScrolling) {
            return (float)((float)this.displayHeight * scale);
        }
        return (float)((float)this.getHeight() * scale);
    }

    public float getScaledHeightWithBorder() {
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

    public ContextMenu setRoundedCorners(boolean topLeft, boolean topRight, boolean bottomLeft, boolean bottomRight) {
        this.roundTopLeftCorner = topLeft;
        this.roundTopRightCorner = topRight;
        this.roundBottomLeftCorner = bottomLeft;
        this.roundBottomRightCorner = bottomRight;
        return this;
    }

    public boolean isForceSide() {
        return this.forceSide;
    }

    public ContextMenu setForceSide(boolean forceSide) {
        this.forceSide = forceSide;
        return this;
    }

    public boolean isForceSideSubMenus() {
        return this.forceSideSubMenus;
    }

    public ContextMenu setForceSideSubMenus(boolean forceSideSubMenus) {
        this.forceSideSubMenus = forceSideSubMenus;
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

    /**
     * Opens the {@link ContextMenu} at the given X and Y coordinates.
     *
     * @param x The X coordinate.
     * @param y The Y coordinate.
     * @param entryPath The {@link SubMenuContextMenuEntry} path of menus to open.
     */
    public ContextMenu openMenuAt(float x, float y, @Nullable List<String> entryPath) {
        this.closeSubMenus();
        this.unhoverAllEntries();
        this.resetSearchState();
        this.rawX = x;
        this.rawY = y;
        this.open = true;
        if (!this.isSubMenu() && UIBase.shouldPlayAnimations() && this.openAnimationEnabled) {
            this.openAnimationStartMs = net.minecraft.Util.getMillis();
            this.openAnimationActive = true;
        } else {
            this.openAnimationActive = false;
        }
        this.scrollPosition = 0.0f; // Reset scroll position when opening menu
        if ((entryPath != null) && !entryPath.isEmpty()) {
            String firstId = entryPath.get(0);
            ContextMenuEntry<?> entry = this.getEntry(firstId);
            if (entry instanceof SubMenuContextMenuEntry sub) {
                sub.openSubMenu((entryPath.size() > 1) ? entryPath.subList(1, entryPath.size()) : null);
            }
        }
        return this;
    }

    /**
     * Opens the {@link ContextMenu} at the given X and Y coordinates.
     *
     * @param x The X coordinate.
     * @param y The Y coordinate.
     */
    public ContextMenu openMenuAt(float x, float y) {
        return this.openMenuAt(x, y, null);
    }

    /**
     * Opens the {@link ContextMenu} at the mouse position.
     *
     * @param entryPath The {@link SubMenuContextMenuEntry} path of menus to open.
     */
    public ContextMenu openMenuAtMouse(@Nullable List<String> entryPath) {
        return this.openMenuAt(MouseInput.getMouseX(), MouseInput.getMouseY(), entryPath);
    }

    /**
     * Opens the {@link ContextMenu} at the mouse position.
     */
    public ContextMenu openMenuAtMouse() {
        return this.openMenuAtMouse(null);
    }

    /**
     * Closes this menu, clears hover state, and closes any open sub-menus attached to it.
     */
    public ContextMenu closeMenu() {
        this.closeSubMenus();
        this.unhoverAllEntries();
        this.open = false;
        this.openAnimationActive = false;
        this.resetSearchState();
        ContextMenu root = this.getRootMenu();
        if (root.cachedSearchMenu == this) {
            root.cachedSearchMenu = null;
        }
        return this;
    }

    /**
     * Closes this menu and every parent menu in the chain up to the root.
     * This also closes any open sub-menus on each menu in the chain.
     */
    public ContextMenu closeMenuChain() {
        ContextMenu current = this;
        while (current != null) {
            current.closeMenu();
            SubMenuContextMenuEntry parent = current.parentEntry;
            current = (parent != null) ? parent.parent : null;
        }
        return this;
    }

    /**
     * Closes all sub-menus that belong to this menu without closing the menu itself.
     */
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
        if (UIBase.shouldPlayAnimations() && this.openAnimationEnabled && this.isTopLevelOpenAnimationRunning()) {
            return true;
        }
        // If the menu is scrollable and the mouse is over it, consider it as navigating
        if (this.needsScrolling && this.isOpen() && this.isMouseOverMenu(this.renderMouseX, this.renderMouseY)) {
            return true;
        }
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

    // Helper to check if an entry is currently visible in the scrollable area
    private boolean isEntryVisible(ContextMenuEntry<?> entry) {
        if (!this.needsScrolling) return true;

        float scale = UIBase.calculateFixedScale(this.getScale());
        float scaledY = (float)((float)this.getActualY()/scale) + this.getBorderThickness();

        float entryTop = entry.y;
        float entryBottom = entry.y + entry.getHeight();
        float visibleTop = scaledY + SCROLL_INDICATOR_HEIGHT;
        float visibleBottom = scaledY + this.displayHeight - SCROLL_INDICATOR_HEIGHT;

        return entryTop < visibleBottom && entryBottom > visibleTop;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.isUserNavigatingInMenu()) {
            float scale = UIBase.calculateFixedScale(this.scale) * this.getOpenAnimationScale(0.0F);
            int scaledMouseX = (int) ((float)mouseX / scale);
            int scaledMouseY = (int) ((float)mouseY / scale);
            String searchText = this.getActiveSearchText();
            String searchLower = (searchText != null) ? searchText.toLowerCase(Locale.ROOT) : null;
            boolean filterActive = (searchLower != null) && !searchLower.isBlank();

            // Check if click is on scroll arrow areas first
            if (button == 0 && this.needsScrolling && this.isMouseOverMenu(mouseX, mouseY)) {
                float scaledX = (float)((float)this.getActualX()/scale) + this.getBorderThickness();
                float scaledY = (float)((float)this.getActualY()/scale) + this.getBorderThickness();
                
                // Calculate max scroll position
                float maxScrollPosition = this.rawHeight - (this.displayHeight - SCROLL_INDICATOR_HEIGHT * 2);
                
                // Check if clicking in up arrow area
                if (scaledMouseY >= scaledY && scaledMouseY <= scaledY + SCROLL_INDICATOR_HEIGHT) {
                    // Only scroll if the arrow is actually visible (can scroll up)
                    if (this.scrollPosition > 0) {
                        // Scroll up by a fixed amount (e.g., 3 entries worth)
                        this.scrollPosition = Math.max(0, this.scrollPosition - 60);
                        // Close sub-menus when scrolling
                        this.closeSubMenus();
                    }
                    // Always consume the click in the arrow area
                    return true;
                }
                
                // Check if clicking in down arrow area
                if (scaledMouseY >= scaledY + this.displayHeight - SCROLL_INDICATOR_HEIGHT && 
                    scaledMouseY <= scaledY + this.displayHeight) {
                    // Only scroll if the arrow is actually visible (can scroll down)
                    if (this.scrollPosition < maxScrollPosition) {
                        // Scroll down by a fixed amount (e.g., 3 entries worth)
                        this.scrollPosition = Math.min(maxScrollPosition, this.scrollPosition + 60);
                        // Close sub-menus when scrolling
                        this.closeSubMenus();
                    }
                    // Always consume the click in the arrow area
                    return true;
                }
            }

            // Process entries only if they're visible in the scroll area
            for (ContextMenuEntry<?> entry : this.entries) {
                if (!entry.isVisible()) continue;
                if (filterActive && !this.matchesSearchFilter(entry, searchLower)) continue;
                if (!this.needsScrolling || isEntryVisible(entry)) {
                    entry.mouseClicked(scaledMouseX, scaledMouseY, button);
                }
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
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.isUserNavigatingInMenu()) {
            float scale = UIBase.calculateFixedScale(this.scale) * this.getOpenAnimationScale(0.0F);
            int scaledMouseX = (int) ((float)mouseX / scale);
            int scaledMouseY = (int) ((float)mouseY / scale);
            String searchText = this.getActiveSearchText();
            String searchLower = (searchText != null) ? searchText.toLowerCase(Locale.ROOT) : null;
            boolean filterActive = (searchLower != null) && !searchLower.isBlank();

            for (ContextMenuEntry<?> entry : this.entries) {
                if (!entry.isVisible()) continue;
                if (filterActive && !this.matchesSearchFilter(entry, searchLower)) continue;
                if (!this.needsScrolling || isEntryVisible(entry)) {
                    entry.mouseReleased(scaledMouseX, scaledMouseY, button);
                }
            }
            for (ContextMenuEntry<?> e : this.entries) {
                if (e instanceof SubMenuContextMenuEntry s) {
                    s.subContextMenu.mouseReleased(mouseX, mouseY, button);
                }
            }
            return true;
        }
        return GuiEventListener.super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.isUserNavigatingInMenu()) {
            float scale = UIBase.calculateFixedScale(this.scale) * this.getOpenAnimationScale(0.0F);
            int scaledMouseX = (int) ((float)mouseX / scale);
            int scaledMouseY = (int) ((float)mouseY / scale);
            double scaledDragX = dragX / scale;
            double scaledDragY = dragY / scale;
            String searchText = this.getActiveSearchText();
            String searchLower = (searchText != null) ? searchText.toLowerCase(Locale.ROOT) : null;
            boolean filterActive = (searchLower != null) && !searchLower.isBlank();

            for (ContextMenuEntry<?> entry : this.entries) {
                if (!entry.isVisible()) continue;
                if (filterActive && !this.matchesSearchFilter(entry, searchLower)) continue;
                if (!this.needsScrolling || isEntryVisible(entry)) {
                    entry.mouseDragged(scaledMouseX, scaledMouseY, button, scaledDragX, scaledDragY);
                }
            }
            for (ContextMenuEntry<?> e : this.entries) {
                if (e instanceof SubMenuContextMenuEntry s) {
                    s.subContextMenu.mouseDragged(mouseX, mouseY, button, dragX, dragY);
                }
            }
            return true;
        }
        return GuiEventListener.super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {

        if (this.isOpen()) {

            if (this.needsScrolling && this.isMouseOverMenu(mouseX, mouseY)) {
                // Close all sub-menus when scrolling in the parent menu
                this.closeSubMenus();

                // Update scroll position (scrollDeltaY is negative when scrolling down)
                this.scrollPosition -= scrollDeltaY * 30.0; // Adjust scroll speed

                // Clamp scroll position
                float maxScrollPosition = this.rawHeight - (this.displayHeight - SCROLL_INDICATOR_HEIGHT * 2);
                this.scrollPosition = Math.max(0, Math.min(this.scrollPosition, maxScrollPosition));

                return true; // We handled the scroll
            }

            // Check if any submenu can handle the scroll
            for (ContextMenuEntry<?> e : this.entries) {
                if (e instanceof SubMenuContextMenuEntry s) {
                    if (s.subContextMenu.mouseScrolled(mouseX, mouseY, scrollDeltaX, scrollDeltaY)) {
                        return true;
                    }
                }
            }

        }

        return GuiEventListener.super.mouseScrolled(mouseX, mouseY, scrollDeltaX, scrollDeltaY);

    }

    // It's important to not use the real isMouseOver() method, because that would break FM's GUIs
    public boolean isMouseOverMenu(double mouseX, double mouseY) {
        float scale = UIBase.calculateFixedScale(this.getScale()) * this.getOpenAnimationScale(0.0F);
        float actualX = this.getActualX() / scale + this.getBorderThickness();
        float actualY = this.getActualY() / scale + this.getBorderThickness();
        float width = this.getWidth();
        float height = this.needsScrolling ? this.displayHeight : this.getHeight();

        return mouseX >= actualX * scale &&
                mouseX <= (actualX + width) * scale &&
                mouseY >= actualY * scale &&
                mouseY <= (actualY + height) * scale;
    }

    // Always return false here to not break FM's GUIs
    @Override
    public boolean isMouseOver(double $$0, double $$1) {
        return false;
    }

    @Override
    public void setFocused(boolean var1) {
    }

    @Override
    public boolean isFocused() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.isOpen()) return false;
        ContextMenu root = this.getRootMenu();
        root.clearCachedSearchMenuIfClosed();
        ContextMenu hoverMenu = root.getMenuUnderCursor(root.renderMouseX, root.renderMouseY);
        ContextMenu targetMenu = (hoverMenu != null) ? hoverMenu : ((root.cachedSearchMenu != null) ? root.cachedSearchMenu : root);
        if (targetMenu.isCtrlF(keyCode)) {
            if (hoverMenu != null) {
                root.cachedSearchMenu = hoverMenu;
            }
            if (targetMenu.isAlwaysShowSearchBar()) {
                targetMenu.showSearchEntry(true);
            } else if (targetMenu.isSearchEntryVisible()) {
                targetMenu.hideSearchEntry();
            } else {
                targetMenu.showSearchEntry(true);
            }
            return true;
        }
        if (targetMenu.searchEntry.isVisible() && targetMenu.searchEntry.keyPressed(keyCode, scanCode, modifiers)) {
            if (hoverMenu != null) {
                root.cachedSearchMenu = hoverMenu;
            }
            return true;
        }
        if (targetMenu != root && root.searchEntry.isVisible() && root.searchEntry.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return GuiEventListener.super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (!this.isOpen()) return false;
        ContextMenu root = this.getRootMenu();
        root.clearCachedSearchMenuIfClosed();
        ContextMenu hoverMenu = root.getMenuUnderCursor(root.renderMouseX, root.renderMouseY);
        ContextMenu targetMenu = (hoverMenu != null) ? hoverMenu : ((root.cachedSearchMenu != null) ? root.cachedSearchMenu : root);
        if (targetMenu.searchEntry.isVisible() && targetMenu.searchEntry.keyReleased(keyCode, scanCode, modifiers)) {
            if (hoverMenu != null) {
                root.cachedSearchMenu = hoverMenu;
            }
            return true;
        }
        if (targetMenu != root && root.searchEntry.isVisible() && root.searchEntry.keyReleased(keyCode, scanCode, modifiers)) {
            return true;
        }
        return GuiEventListener.super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (!this.isOpen()) return false;
        ContextMenu root = this.getRootMenu();
        root.clearCachedSearchMenuIfClosed();
        ContextMenu hoverMenu = root.getMenuUnderCursor(root.renderMouseX, root.renderMouseY);
        ContextMenu targetMenu = (hoverMenu != null) ? hoverMenu : ((root.cachedSearchMenu != null) ? root.cachedSearchMenu : root);
        if (!targetMenu.searchEntry.isVisible() && !Character.isISOControl(codePoint)) {
            if (hoverMenu != null) {
                root.cachedSearchMenu = hoverMenu;
            }
            targetMenu.showSearchEntry(true);
        }
        if (targetMenu.searchEntry.isVisible() && targetMenu.searchEntry.charTyped(codePoint, modifiers)) {
            if (hoverMenu != null) {
                root.cachedSearchMenu = hoverMenu;
            }
            return true;
        }
        if (targetMenu != root && root.searchEntry.isVisible() && root.searchEntry.charTyped(codePoint, modifiers)) {
            return true;
        }
        return GuiEventListener.super.charTyped(codePoint, modifiers);
    }

    @Override
    public @NotNull NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(@NotNull NarrationElementOutput var1) {
    }

    @Override
    public boolean isFocusable() {
        return false;
    }

    @Override
    public void setFocusable(boolean focusable) {
        throw new RuntimeException("ContextMenus are not focusable!");
    }

    @Override
    public boolean isNavigatable() {
        return false;
    }

    @Override
    public void setNavigatable(boolean navigatable) {
        throw new RuntimeException("ContextMenus are not navigatable!");
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

    protected boolean isSearchEntryVisible() {
        return this.alwaysShowSearchBar || this.searchEntryRequested;
    }

    protected void showSearchEntry(boolean focus) {
        this.searchEntryRequested = true;
        this.scrollPosition = 0.0F;
        this.searchEntry.prepareForImmediateInput(this.shouldAddIconSpaceForEntries());
        this.updateSearchVisibilityState(focus);
    }

    protected void hideSearchEntry() {
        this.searchEntryRequested = false;
        this.updateSearchVisibilityState(false);
    }

    protected void resetSearchState() {
        this.searchEntryRequested = false;
        this.searchEntry.resetSearchValue();
        this.searchEntryVisibleLast = this.isSearchEntryVisible();
    }

    protected void updateSearchVisibilityState(boolean focusOnShow) {
        boolean visible = this.isSearchEntryVisible();
        if (visible != this.searchEntryVisibleLast) {
            if (!visible) {
                this.searchEntry.resetSearchValue();
            } else if (focusOnShow) {
                this.searchEntry.focusAndSelectAll();
            }
        } else if (visible && focusOnShow) {
            this.searchEntry.focusAndSelectAll();
        }
        this.searchEntryVisibleLast = visible;
    }

    @Nullable
    protected String getActiveSearchText() {
        if (!this.isSearchEntryVisible()) return null;
        String value = this.searchEntry.getSearchValue();
        return value.isBlank() ? null : value;
    }

    protected boolean shouldAddIconSpaceForEntries() {
        for (ContextMenuEntry<?> e : this.entries) {
            if (e instanceof ClickableContextMenuEntry<?> c) {
                if (c.hasIconAssigned()) {
                    return true;
                }
            }
            if (e instanceof SearchContextMenuEntry s) {
                if (s.hasIconAssigned() && s.isVisible()) {
                    return true;
                }
            }
        }
        return false;
    }

    protected boolean matchesSearchFilter(@NotNull ContextMenuEntry<?> entry, @NotNull String searchLower) {
        if ((entry == this.searchEntry) || (entry == this.searchSeparator)) {
            return true;
        }
        if (entry instanceof SeparatorContextMenuEntry || entry instanceof SpacerContextMenuEntry) {
            return true;
        }
        if (entry instanceof ClickableContextMenuEntry<?> clickable) {
            Component label = clickable.getLabel();
            String labelLower = label.getString().toLowerCase(Locale.ROOT);
            if (labelLower.contains(searchLower)) {
                return true;
            }
            String trimmedSearch = searchLower.trim();
            if (trimmedSearch.isEmpty()) {
                return true;
            }
            String[] parts = trimmedSearch.split("\\s+");
            if (parts.length <= 1) {
                return false;
            }
            for (String part : parts) {
                if (part.isEmpty()) {
                    continue;
                }
                if (!labelLower.contains(part)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    protected boolean isCtrlF(int keyCode) {
        return keyCode == InputConstants.KEY_F && Screen.hasControlDown();
    }

    protected void clearCachedSearchMenuIfClosed() {
        if (this.cachedSearchMenu != null && !this.cachedSearchMenu.isOpen()) {
            this.cachedSearchMenu = null;
        }
    }

    @Nullable
    protected ContextMenu getMenuUnderCursor(int mouseX, int mouseY) {
        if (!this.isOpen()) return null;
        ContextMenu hovered = this.isMouseOverMenu(mouseX, mouseY) ? this : null;
        for (ContextMenuEntry<?> e : this.entries) {
            if (e instanceof SubMenuContextMenuEntry s && s.subContextMenu.isOpen()) {
                ContextMenu subHovered = s.subContextMenu.getMenuUnderCursor(mouseX, mouseY);
                if (subHovered != null) {
                    hovered = subHovered;
                }
            }
        }
        return hovered;
    }

    @NotNull
    protected ContextMenu getRootMenu() {
        ContextMenu current = this;
        while (current.parentEntry != null) {
            current = current.parentEntry.parent;
        }
        return current;
    }

    protected boolean isProtectedEntryIdentifier(@NotNull String identifier) {
        return SEARCH_ENTRY_IDENTIFIER.equals(identifier)
                || SEARCH_SEPARATOR_IDENTIFIER.equals(identifier);
    }

    protected boolean isProtectedEntry(@NotNull ContextMenuEntry<?> entry) {
        return entry == this.searchEntry
                || entry == this.searchSeparator
                || this.isProtectedEntryIdentifier(entry.identifier);
    }

    protected int getProtectedEntriesCount() {
        int count = 0;
        if (this.searchEntry != null) count++;
        if (this.searchSeparator != null) count++;
        return count;
    }

    /**
     * Stacks the given context menus into a single menu.
     * <p>
     * Only entries that are {@link ContextMenuEntry#isStackable()} and {@link ContextMenuEntry#isActive()}
     * in every menu are included. Stacked entries are linked via
     * {@link ContextMenuStackMeta#getNextInStack()}, with the first entry being the visible one.
     *
     * <p><b>Example (multi-select)</b>
     * <pre>{@code
     * ContextMenu stacked = ContextMenu.stackContextMenus(menu1, menu2, menu3);
     * stacked.openMenuAt(mouseX, mouseY);
     * }</pre>
     */
    @NotNull
    public static ContextMenu stackContextMenus(@NotNull List<ContextMenu> menusToStack) {
        return stackContextMenus(menusToStack.toArray(new ContextMenu[0]));
    }

    /**
     * Stacks the given context menus into a single menu.
     * <p>
     * This copies stackable entries from each menu, links them through {@link ContextMenuStackMeta},
     * and shares a single {@link RuntimePropertyContainer} across the stack.
     *
     * <p>Sub-menus are stacked recursively.
     */
    @NotNull
    public static ContextMenu stackContextMenus(@NotNull ContextMenu... menusToStack) {

        ContextMenu stacked = new ContextMenu();

        if (menusToStack.length > 0) {

            stacked.scale = menusToStack[0].scale;
            stacked.subMenuOpeningSide = menusToStack[0].subMenuOpeningSide;
            stacked.shadow = menusToStack[0].shadow;
            stacked.forceUIScale = menusToStack[0].forceUIScale;
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

    public static abstract class ContextMenuEntry<T extends ContextMenuEntry<T>> implements Renderable, GuiEventListener {

        protected String identifier;
        protected ContextMenu parent;
        /** Only for internal use. This gets set by the parent {@link ContextMenu}. **/
        protected float x;
        /** Only for internal use. This gets set by the parent {@link ContextMenu}. **/
        protected float y;
        /** Only for internal use. This gets set by the parent {@link ContextMenu}. **/
        protected float width;
        protected float height = 20;
        @Nullable
        protected EntryTask tickAction;
        protected EntryTask hoverAction;
        protected boolean hovered = false;
        /**
         * Stack metadata for this entry.
         * <p>
         * This metadata is populated when menus are stacked via {@link ContextMenu#stackContextMenus(ContextMenu...)}.
         */
        protected ContextMenuStackMeta stackMeta = new ContextMenuStackMeta();
        protected List<BooleanSupplier> activeStateSuppliers = new ArrayList<>();
        protected List<BooleanSupplier> visibleStateSuppliers = new ArrayList<>();
        @Nullable
        protected Supplier<UITooltip> tooltipSupplier;
        protected Font font = Minecraft.getInstance().font;
        protected boolean addSpaceForIcon = false;
        protected boolean changeBackgroundColorOnHover = true;
        /**
         * Optional applier used by stack-aware builders to apply values across the stack.
         * See {@link ContextMenuBuilder#applyStackAppliers(ContextMenuEntry, Object)}.
         */
        @Nullable
        protected StackApplier stackApplier;
        /**
         * Optional value supplier used to read the current value for mixed-state detection.
         * See {@link ContextMenuBuilder#resolveStackValue(ContextMenuEntry)}.
         */
        @Nullable
        protected StackValueSupplier stackValueSupplier;
        /**
         * Optional group key used by {@link ContextMenuBuilder#runStackedClickActions(ContextMenu.ClickableContextMenuEntry)}
         * to avoid duplicate actions when multiple entries represent the same logical action.
         */
        @Nullable
        protected Object stackGroupKey;

        public ContextMenuEntry(@NotNull String identifier, @NotNull ContextMenu parent) {
            this.identifier = identifier;
            this.parent = parent;
        }

        @Override
        public abstract void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial);

        @NotNull
        public String getIdentifier() {
            return identifier;
        }

        @NotNull
        public ContextMenu getParent() {
            return parent;
        }

        public float getHeight() {
            return this.height;
        }

        public T setHeight(float height) {
            this.height = height;
            return (T) this;
        }

        public abstract float getMinWidth();

        protected void setHovered(boolean hovered) {
            this.hovered = hovered;
        }

        public boolean isHovered() {
            if (!this.parent.isOpen()) return false;
            return this.hovered;
        }

        public boolean isChangeBackgroundColorOnHover() {
            return this.changeBackgroundColorOnHover;
        }

        public T setChangeBackgroundColorOnHover(boolean changeColor) {
            this.changeBackgroundColorOnHover = changeColor;
            return (T) this;
        }

        public boolean isActive() {
            for (BooleanSupplier b : this.activeStateSuppliers) {
                if (!b.getBoolean(this.parent, this)) return false;
            }
            return true;
        }

        /**
         * @deprecated Use {@link ContextMenuEntry#addIsActiveSupplier(BooleanSupplier)} instead.
         */
        @Deprecated
        public T setIsActiveSupplier(@Nullable BooleanSupplier activeStateSupplier) {
            if (activeStateSupplier != null) this.addIsActiveSupplier(activeStateSupplier);
            return (T) this;
        }

        /**
         * Add a {@link BooleanSupplier} that controls if this entry should be active (clickable).<br>
         * These controllers stack, so multiple controllers can handle the active state of the entry at the same time. If at least one controller returns false, the entry gets disabled.
         */
        public T addIsActiveSupplier(@NotNull BooleanSupplier activeStateSupplier) {
            this.activeStateSuppliers.add(Objects.requireNonNull(activeStateSupplier));
            return (T) this;
        }

        public boolean isVisible() {
            for (BooleanSupplier b : this.visibleStateSuppliers) {
                if (!b.getBoolean(this.parent, this)) return false;
            }
            return true;
        }

        /**
         * @deprecated Use {@link ContextMenuEntry#addIsVisibleSupplier(BooleanSupplier)} instead.
         */
        @Deprecated
        public T setIsVisibleSupplier(@Nullable BooleanSupplier visibleStateSupplier) {
            if (visibleStateSupplier != null) this.addIsVisibleSupplier(visibleStateSupplier);
            return (T) this;
        }

        /**
         * Add a {@link BooleanSupplier} that controls if this entry should be visible.<br>
         * These controllers stack, so multiple controllers can handle the visible state of the entry at the same time. If at least one controller returns false, the entry gets hidden.
         */
        public T addIsVisibleSupplier(@NotNull BooleanSupplier visibleStateSupplier) {
            this.visibleStateSuppliers.add(Objects.requireNonNull(visibleStateSupplier));
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

        public T setTooltipSupplier(@Nullable Supplier<UITooltip> tooltipSupplier) {
            this.tooltipSupplier = tooltipSupplier;
            return (T) this;
        }

        @Nullable
        public UITooltip getTooltip() {
            return (this.tooltipSupplier != null) ? this.tooltipSupplier.get(this.parent, this) : null;
        }

        /**
         * Marks this entry as stackable, allowing it to be included in stacked menus.
         */
        public T setStackable(boolean stackable) {
            this.getStackMeta().setStackable(stackable);
            return (T) this;
        }

        /**
         * @return true if this entry may be stacked with the same entry across menus.
         */
        public boolean isStackable() {
            return this.getStackMeta().isStackable();
        }

        /**
         * Returns the stack metadata for this entry.
         * <p>
         * Use {@link ContextMenuStackMeta#getNextInStack()} to walk the stack.
         */
        @NotNull
        public ContextMenuStackMeta getStackMeta() {
            return this.stackMeta;
        }

        /**
         * @return the stack applier assigned to this entry, or null if none.
         */
        @Nullable
        public StackApplier getStackApplier() {
            return this.stackApplier;
        }

        /**
         * Sets the stack applier for this entry.
         * <p>
         * The applier should only mutate the entry's {@link ContextMenuBuilder#self()} instance,
         * because it will be invoked once per stack entry.
         *
         * <p><b>Example</b>
         * <pre>{@code
         * entry.setStackApplier((stackEntry, value) -> {
         *     if (value instanceof Boolean b) {
         *         builder.self().setEnabled(b);
         *     }
         * });
         * }</pre>
         */
        @NotNull
        public T setStackApplier(@Nullable StackApplier stackApplier) {
            this.stackApplier = stackApplier;
            return (T) this;
        }

        /**
         * @return the stack value supplier for this entry, or null if none.
         */
        @Nullable
        public StackValueSupplier getStackValueSupplier() {
            return this.stackValueSupplier;
        }

        /**
         * Sets the stack value supplier for this entry.
         * <p>
         * This supplier is used by {@link ContextMenuBuilder#resolveStackValue(ContextMenuEntry)}
         * to detect mixed values.
         */
        @NotNull
        public T setStackValueSupplier(@Nullable StackValueSupplier stackValueSupplier) {
            this.stackValueSupplier = stackValueSupplier;
            return (T) this;
        }

        /**
         * @return the optional stack group key for this entry.
         */
        @Nullable
        public Object getStackGroupKey() {
            return this.stackGroupKey;
        }

        /**
         * Sets the optional stack group key for this entry.
         * <p>
         * Entries with the same group key are treated as a single logical action in
         * {@link ContextMenuBuilder#runStackedClickActions(ContextMenu.ClickableContextMenuEntry)}.
         */
        @NotNull
        public T setStackGroupKey(@Nullable Object stackGroupKey) {
            this.stackGroupKey = stackGroupKey;
            return (T) this;
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
        protected static final int ICON_PADDING_LEFT = 10;
        protected static final int ICON_LABEL_SPACING = 20;

        @NotNull
        protected ClickAction clickAction;
        @NotNull
        protected Supplier<Component> labelSupplier;
        @Nullable
        protected Supplier<Component> shortcutTextSupplier;
        @Nullable
        protected ResourceLocation icon;
        @Nullable
        protected MaterialIcon materialIcon;
        protected boolean tooltipIconHovered = false;
        protected boolean tooltipActive = false;
        protected long tooltipIconHoverStart = -1;
        protected boolean enableClickSound = true;
        @NotNull
        protected IconAnimation.Instance iconWiggleAnimation = IconAnimations.SHORT_WIGGLE_LEFT_RIGHT.createInstance();

        public ClickableContextMenuEntry(@NotNull String identifier, @NotNull ContextMenu parent, @NotNull Component label, @NotNull ClickAction clickAction) {
            super(identifier, parent);
            this.clickAction = clickAction;
            this.labelSupplier = (menu, entry) -> label;
        }

        @Override
        public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

            this.renderBackground(graphics);

            int labelX = this.getLabelX();
            int labelY = (int) (this.y + (this.height / 2) - (UIBase.getUITextHeightNormal() / 2));
            UIBase.renderText(graphics, this.getLabel(), labelX, labelY, this.getLabelColor());

            int shortcutTextWidth = 0;
            Component shortcutText = this.getShortcutText();
            if (shortcutText != null) {
                shortcutTextWidth = (int) UIBase.getUITextWidthSmall(shortcutText);
                int shortcutX = (int) (this.x + this.width - 10 - shortcutTextWidth);
                int shortcutY = (int) (this.y + (this.height / 2) - (UIBase.getUITextHeightSmall() / 2));
                UIBase.renderText(graphics, shortcutText, shortcutX, shortcutY, this.getLabelColor(), UIBase.getUITextSizeSmall());
            }

            this.renderIcon(graphics);

            this.renderTooltipIconAndRegisterTooltip(graphics, mouseX, mouseY);

        }

        protected void renderIcon(GuiGraphics graphics) {
            IconRenderData iconData = this.resolveIconData();
            if (iconData == null) {
                return;
            }
            float areaX = this.x + ICON_PADDING_LEFT + this.getIconWiggleOffsetX();
            float areaY = this.y + (this.getHeight() / 2.0F) - (ICON_WIDTH_HEIGHT / 2.0F);
            RenderSystem.enableBlend();
            UIBase.getUITheme().setUITextureShaderColor(graphics, 1.0F);
            this.blitScaledIcon(graphics, iconData, areaX, areaY, ICON_WIDTH_HEIGHT, ICON_WIDTH_HEIGHT);
            RenderingUtils.resetShaderColor(graphics);
        }

        @Nullable
        protected IconRenderData resolveIconData() {
            if (this.materialIcon != null) {
                float renderSize = ICON_WIDTH_HEIGHT;
                ResourceLocation location = this.materialIcon.getTextureLocationForUI(renderSize, renderSize);
                if (location == null) {
                    return null;
                }
                int iconSize = this.materialIcon.getTextureSizeForUI(renderSize, renderSize);
                int width = this.materialIcon.getWidth(iconSize);
                int height = this.materialIcon.getHeight(iconSize);
                if (width <= 0 || height <= 0) {
                    return null;
                }
                return new IconRenderData(location, width, height);
            }
            if (this.icon != null) {
                return new IconRenderData(this.icon, ICON_WIDTH_HEIGHT, ICON_WIDTH_HEIGHT);
            }
            return null;
        }

        protected void blitScaledIcon(@NotNull GuiGraphics graphics, @NotNull IconRenderData iconData, float areaX, float areaY, float areaWidth, float areaHeight) {
            this.blitScaledIcon(graphics, iconData, areaX, areaY, areaWidth, areaHeight, 0.0F);
        }

        protected void blitScaledIcon(@NotNull GuiGraphics graphics, @NotNull IconRenderData iconData, float areaX, float areaY, float areaWidth, float areaHeight, float rotationDegrees) {
            if (areaWidth <= 0.0F || areaHeight <= 0.0F) {
                return;
            }
            float scale = Math.min(areaWidth / (float) iconData.width, areaHeight / (float) iconData.height);
            if (!Float.isFinite(scale) || scale <= 0.0F) {
                return;
            }
            float scaledWidth = iconData.width * scale;
            float scaledHeight = iconData.height * scale;
            float drawX = areaX + (areaWidth - scaledWidth) * 0.5F;
            float drawY = areaY + (areaHeight - scaledHeight) * 0.5F;
            graphics.pose().pushPose();
            graphics.pose().translate(drawX, drawY, 0.0F);
            graphics.pose().scale(scale, scale, 1.0F);
            if (rotationDegrees != 0.0F) {
                graphics.pose().translate(iconData.width * 0.5F, iconData.height * 0.5F, 0.0F);
                graphics.pose().mulPose(Axis.ZP.rotationDegrees(rotationDegrees));
                graphics.pose().translate(-iconData.width * 0.5F, -iconData.height * 0.5F, 0.0F);
            }
            graphics.blit(iconData.texture, 0, 0, 0.0F, 0.0F, iconData.width, iconData.height, iconData.width, iconData.height);
            graphics.pose().popPose();
        }

        protected boolean hasIconAssigned() {
            return this.icon != null || this.materialIcon != null;
        }

        @Override
        protected void setHovered(boolean hovered) {
            boolean wasHovered = this.hovered;
            super.setHovered(hovered);
            if (!wasHovered && hovered && this.isActive() && UIBase.shouldPlayAnimations()) {
                this.iconWiggleAnimation.start();
            }
        }

        protected float getIconWiggleOffsetX() {
            if (!UIBase.shouldPlayAnimations()) {
                this.iconWiggleAnimation.reset();
                return 0.0F;
            }
            if (!this.isActive()) {
                this.iconWiggleAnimation.reset();
                return 0.0F;
            }
            return this.iconWiggleAnimation.getOffsetX();
        }

        protected void renderTooltipIconAndRegisterTooltip(GuiGraphics graphics, int mouseX, int mouseY) {

            UITooltip tooltip = this.getTooltip();

            if (tooltip != null) {

                this.tooltipIconHovered = this.isTooltipIconHovered(mouseX, mouseY);
                if (this.tooltipIconHovered) {
                    if (this.tooltipIconHoverStart == -1) {
                        this.tooltipIconHoverStart = System.currentTimeMillis();
                    }
                } else {
                    this.tooltipIconHoverStart = -1;
                }
                this.tooltipActive = (this.tooltipIconHoverStart != -1) && ((this.tooltipIconHoverStart + 200) < System.currentTimeMillis());

                RenderSystem.enableBlend();
                UIBase.getUITheme().ui_icon_texture_color.setAsShaderColor(graphics, this.tooltipIconHovered ? 1.0F : 0.2F);
                IconRenderData iconData = this.resolveTooltipIconData();
                if (iconData != null) {
                    this.blitScaledIcon(graphics, iconData, this.getTooltipIconX(), this.getTooltipIconY(), ICON_WIDTH_HEIGHT, ICON_WIDTH_HEIGHT);
                }
                RenderingUtils.resetShaderColor(graphics);

                if (this.tooltipActive) {
                    TooltipHandler.INSTANCE.addRenderTickTooltip(tooltip, () -> true);
                }

            } else {
                this.tooltipIconHovered = false;
                this.tooltipActive = false;
            }

        }

        protected boolean isTooltipIconHovered(int mouseX, int mouseY) {
            return UIBase.isXYInArea(mouseX, mouseY, this.getTooltipIconX(), this.getTooltipIconY(), ICON_WIDTH_HEIGHT, ICON_WIDTH_HEIGHT);
        }

        protected int getLabelX() {
            int labelX = (int) (this.x + ICON_PADDING_LEFT);
            if (this.hasIconAssigned() || this.addSpaceForIcon) {
                labelX += ICON_LABEL_SPACING;
            }
            return labelX;
        }

        protected int getTooltipIconX() {
            int labelX = this.getLabelX();
            int labelWidth = (int) UIBase.getUITextWidthNormal(this.getLabel());
            int gap = Math.round(ICON_WIDTH_HEIGHT * (2.0F / 3.0F));
            return labelX + labelWidth + gap;
        }

        protected int getTooltipIconY() {
            return (int) (this.y + 5);
        }

        @Nullable
        protected IconRenderData resolveTooltipIconData() {
            float renderSize = ICON_WIDTH_HEIGHT;
            ResourceLocation location = CONTEXT_MENU_TOOLTIP_ICON.getTextureLocationForUI(renderSize, renderSize);
            if (location == null) {
                return null;
            }
            int iconSize = CONTEXT_MENU_TOOLTIP_ICON.getTextureSizeForUI(renderSize, renderSize);
            int width = CONTEXT_MENU_TOOLTIP_ICON.getWidth(iconSize);
            int height = CONTEXT_MENU_TOOLTIP_ICON.getHeight(iconSize);
            if (width <= 0 || height <= 0) {
                return null;
            }
            return new IconRenderData(location, width, height);
        }

        protected void renderBackground(@NotNull GuiGraphics graphics) {
            if (this.isChangeBackgroundColorOnHover() && this.isHovered() && this.isActive()) {
                int backColor = UIBase.shouldBlur() ? UIBase.getUITheme().ui_blur_interface_widget_background_color_hover_type_1.getColorInt() : UIBase.getUITheme().ui_interface_widget_background_color_hover_type_1.getColorInt();
                RenderingUtils.fillF(graphics, (float) this.x, (float) this.y, (float) (this.x + this.width), (float) (this.y + this.height), backColor);
            }
        }

        protected int getLabelColor() {
            if (UIBase.shouldBlur()) {
                return this.isActive() ? UIBase.getUITheme().ui_blur_interface_widget_label_color_normal.getColorInt() : UIBase.getUITheme().ui_blur_interface_widget_label_color_inactive.getColorInt();
            }
            return this.isActive() ? UIBase.getUITheme().ui_interface_widget_label_color_normal.getColorInt() : UIBase.getUITheme().ui_interface_widget_label_color_inactive.getColorInt();
        }

        @Nullable
        public ResourceLocation getIcon() {
            if (this.icon != null) {
                return this.icon;
            }
            if (this.materialIcon != null) {
                float renderSize = ICON_WIDTH_HEIGHT;
                return this.materialIcon.getTextureLocationForUI(renderSize, renderSize);
            }
            return null;
        }

        /**
         * Icons should be completely white. No other colors should be used.
         */
        public T setIcon(@Nullable ResourceLocation icon) {
            this.icon = icon;
            this.materialIcon = null;
            return (T) this;
        }

        /**
         * Icons should be completely white. No other colors should be used.
         */
        public T setIcon(@Nullable MaterialIcon icon) {
            this.materialIcon = icon;
            this.icon = null;
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

        public void runClickAction() {
            this.clickAction.onClick(this.parent, this);
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

        public boolean isClickSoundEnabled() {
            return this.enableClickSound;
        }

        public T setClickSoundEnabled(boolean enabled) {
            this.enableClickSound = enabled;
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
            copy.activeStateSuppliers = new ArrayList<>(this.activeStateSuppliers);
            copy.icon = this.icon;
            copy.materialIcon = this.materialIcon;
            copy.iconWiggleAnimation = this.iconWiggleAnimation.getAnimation().createInstance();
            copy.stackApplier = this.stackApplier;
            copy.stackValueSupplier = this.stackValueSupplier;
            copy.stackGroupKey = this.stackGroupKey;
            return copy;
        }

        @Override
        public float getMinWidth() {
            int i = (int) (UIBase.getUITextWidthNormal(this.getLabel()) + 20);
            if (this.tooltipSupplier != null) {
                i += 30;
            }
            Component shortcutText = this.getShortcutText();
            if (shortcutText != null) {
                i += UIBase.getUITextWidthSmall(shortcutText) + 30;
            }
            if (this.hasIconAssigned() || this.addSpaceForIcon) {
                i += ICON_LABEL_SPACING;
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
                if (FancyMenu.getOptions().playUiClickSounds.getValue() && this.enableClickSound) {
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

    protected static final class IconRenderData {
        final ResourceLocation texture;
        final int width;
        final int height;

        private IconRenderData(@NotNull ResourceLocation texture, int width, int height) {
            this.texture = texture;
            this.width = width;
            this.height = height;
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
            copy.activeStateSuppliers = new ArrayList<>(this.activeStateSuppliers);
            copy.icon = this.icon;
            copy.materialIcon = this.materialIcon;
            copy.iconWiggleAnimation = this.iconWiggleAnimation.getAnimation().createInstance();
            copy.stackApplier = this.stackApplier;
            copy.stackValueSupplier = this.stackValueSupplier;
            copy.stackGroupKey = this.stackGroupKey;
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
        @NotNull
        protected IconAnimation.Instance subMenuArrowSpin = IconAnimations.SHORT_SPIN_UP_SUBTLE.createInstance();

        public SubMenuContextMenuEntry(@NotNull String identifier, @NotNull ContextMenu parent, @NotNull Component label, @NotNull ContextMenu subContextMenu) {
            super(identifier, parent, label, ((menu, entry) -> {}));
            this.subContextMenu = subContextMenu;
            this.subContextMenu.parentEntry = this;
            this.clickAction = (menu, entry) -> this.openSubMenu();
        }

        @Override
        public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

            this.tickEntry();

            super.render(graphics, mouseX, mouseY, partial);

            this.renderSubMenuArrow(graphics);

        }

        protected void renderSubMenuArrow(GuiGraphics graphics) {
            RenderSystem.enableBlend();
            UIBase.getUITheme().setUITextureShaderColor(graphics, 1.0F);
            IconRenderData iconData = this.resolveSubMenuArrowIconData();
            if (iconData != null) {
                this.blitScaledIcon(graphics, iconData, (int) (this.x + this.width - 20), (int) (this.y + 5), ICON_WIDTH_HEIGHT, ICON_WIDTH_HEIGHT, this.getSubMenuArrowRotation());
            }
            RenderingUtils.resetShaderColor(graphics);
        }

        protected float getSubMenuArrowRotation() {
            if (!UIBase.shouldPlayAnimations()) {
                this.subMenuArrowSpin.reset();
                return 0.0F;
            }
            if (!this.isActive()) {
                this.subMenuArrowSpin.reset();
                return 0.0F;
            }
            return this.subMenuArrowSpin.getRotationDegrees();
        }

        @Nullable
        protected IconRenderData resolveSubMenuArrowIconData() {
            float renderSize = ICON_WIDTH_HEIGHT;
            ResourceLocation location = SUB_CONTEXT_MENU_ARROW_ICON.getTextureLocationForUI(renderSize, renderSize);
            if (location == null) {
                return null;
            }
            int iconSize = SUB_CONTEXT_MENU_ARROW_ICON.getTextureSizeForUI(renderSize, renderSize);
            int width = SUB_CONTEXT_MENU_ARROW_ICON.getWidth(iconSize);
            int height = SUB_CONTEXT_MENU_ARROW_ICON.getHeight(iconSize);
            if (width <= 0 || height <= 0) {
                return null;
            }
            return new IconRenderData(location, width, height);
        }

        @Override
        protected void renderBackground(@NotNull GuiGraphics graphics) {
            boolean hover = this.hovered;
            this.hovered = this.hovered || this.subContextMenu.isOpen();
            super.renderBackground(graphics);
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
                int openSpeed = 400 / Math.max(1, FancyMenu.getOptions().contextMenuHoverOpenSpeed.getValue());
                if (((this.entryHoverStartTime + openSpeed) < now) && !this.subContextMenu.isOpen()) {
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

        /**
         * Opens the {@link ContextMenu} of this {@link SubMenuContextMenuEntry}.
         *
         * @param entryPath The {@link SubMenuContextMenuEntry} path of menus to open.
         */
        public void openSubMenu(@NotNull List<String> entryPath) {
            if (this.isActive() && !this.subContextMenu.isOpen()) {
                this.subMenuArrowSpin.start();
            }
            this.subContextMenu.openMenuAt(0, 0, entryPath);
        }

        /**
         * Opens the {@link ContextMenu} of this {@link SubMenuContextMenuEntry}.
         */
        public void openSubMenu() {
            if (this.isActive() && !this.subContextMenu.isOpen()) {
                this.subMenuArrowSpin.start();
            }
            this.subContextMenu.openMenuAt(0, 0);
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
            copy.activeStateSuppliers = new ArrayList<>(this.activeStateSuppliers);
            copy.labelSupplier = this.labelSupplier;
            copy.icon = this.icon;
            copy.materialIcon = this.materialIcon;
            copy.iconWiggleAnimation = this.iconWiggleAnimation.getAnimation().createInstance();
            copy.stackApplier = this.stackApplier;
            copy.stackValueSupplier = this.stackValueSupplier;
            copy.stackGroupKey = this.stackGroupKey;
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
        public float getMinWidth() {
            float i = super.getMinWidth();
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
        public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            float renderScale = UIBase.calculateFixedScale(this.parent.getScale());
            float lineThickness = renderScale > 0.0F ? (0.5F / renderScale) : 0.5F;
            float uiScale = UIBase.getUIScale();
            float thinnessT = (uiScale - 1.0F) / 1.5F;
            thinnessT = Math.min(1.0F, Math.max(0.0F, thinnessT));
            float alphaFactor = 0.55F + (0.45F * thinnessT);
            float minX = this.x + 10.0F;
            float maxX = this.x + this.width - 10.0F;
            float minY = this.y + 4.0F;
            if (renderScale > 0.0F) {
                float snappedPixelY = (float) Math.round(minY * renderScale);
                minY = snappedPixelY / renderScale;
            }
            float maxY = minY + lineThickness;
            int lineColor = UIBase.shouldBlur() ? UIBase.getUITheme().ui_blur_overlay_border_color.getColorInt() : UIBase.getUITheme().ui_overlay_border_color.getColorInt();
            if (alphaFactor < 0.999F) {
                int baseAlpha = (lineColor >>> 24) & 0xFF;
                int adjustedAlpha = Math.round(baseAlpha * alphaFactor);
                lineColor = RenderingUtils.replaceAlphaInColor(lineColor, adjustedAlpha);
            }
            RenderingUtils.fillF(graphics, minX, minY, maxX, maxY, lineColor);
        }

        @Override
        public SeparatorContextMenuEntry copy() {
            SeparatorContextMenuEntry copy = new SeparatorContextMenuEntry(this.identifier, this.parent);
            copy.height = this.height;
            copy.tickAction = this.tickAction;
            copy.tickAction = this.tickAction;
            copy.tooltipSupplier = this.tooltipSupplier;
            copy.activeStateSuppliers = new ArrayList<>(this.activeStateSuppliers);
            copy.stackApplier = this.stackApplier;
            copy.stackValueSupplier = this.stackValueSupplier;
            copy.stackGroupKey = this.stackGroupKey;
            return copy;
        }

        @Override
        public float getMinWidth() {
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
        public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        }

        @Override
        public float getMinWidth() {
            int i = 20;
            if (this.addSpaceForIcon) i += 20;
            return i;
        }

        @Override
        public SpacerContextMenuEntry copy() {
            SpacerContextMenuEntry e = new SpacerContextMenuEntry(this.identifier, this.parent);
            e.height = this.height;
            e.stackApplier = this.stackApplier;
            e.stackValueSupplier = this.stackValueSupplier;
            e.stackGroupKey = this.stackGroupKey;
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

    public static class SearchContextMenuEntry extends ContextMenuEntry<SearchContextMenuEntry> {

        private static final int FIELD_VERTICAL_PADDING = 2;
        private static final int FIELD_HORIZONTAL_PADDING = 10;
        private static final int MIN_FIELD_WIDTH = 40;
        private static final int MIN_FIELD_HEIGHT = 12;
        @Nullable
        private MaterialIcon icon = MaterialIcons.SEARCH;
        private final ExtendedEditBox searchBox;
        private boolean lastBlurState = UIBase.shouldBlur();

        public SearchContextMenuEntry(@NotNull String identifier, @NotNull ContextMenu parent) {
            super(identifier, parent);
            this.height = 20;
            this.searchBox = new ExtendedEditBox(Minecraft.getInstance().font, 0, 0, 0, 0, Component.empty());
            this.searchBox.setResponder(value -> parent.closeSubMenus());
            this.applyDefaultSkin();
            this.setChangeBackgroundColorOnHover(false);
        }

        @Override
        public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            this.applySkinIfNeeded();
            this.updateSearchBoxBounds();
            this.renderIcon(graphics);
            this.searchBox.render(graphics, mouseX, mouseY, partial);
        }

        @Override
        public float getMinWidth() {
            Component hint = Component.translatable("fancymenu.ui.generic.search");
            float width = UIBase.getUITextWidthNormal(hint) + (FIELD_HORIZONTAL_PADDING * 2.0F);
            if (this.addSpaceForIcon) {
                width += ClickableContextMenuEntry.ICON_LABEL_SPACING;
            }
            return Math.max(width, 120.0F);
        }

        @Override
        public SearchContextMenuEntry copy() {
            SearchContextMenuEntry copy = new SearchContextMenuEntry(this.identifier, this.parent);
            copy.height = this.height;
            copy.tickAction = this.tickAction;
            copy.tooltipSupplier = this.tooltipSupplier;
            copy.activeStateSuppliers = new ArrayList<>(this.activeStateSuppliers);
            copy.visibleStateSuppliers = new ArrayList<>(this.visibleStateSuppliers);
            copy.stackApplier = this.stackApplier;
            copy.stackValueSupplier = this.stackValueSupplier;
            copy.stackGroupKey = this.stackGroupKey;
            copy.icon = this.icon;
            copy.searchBox.setValue(this.searchBox.getValue());
            return copy;
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
            if (!this.isVisible()) return false;
            this.updateSearchBoxBounds();
            return this.searchBox.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            if (!this.isVisible()) return false;
            this.updateSearchBoxBounds();
            return this.searchBox.mouseReleased(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            if (!this.isVisible()) return false;
            this.updateSearchBoxBounds();
            return this.searchBox.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (!this.isVisible()) return false;
            return this.searchBox.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
            if (!this.isVisible()) return false;
            return this.searchBox.keyReleased(keyCode, scanCode, modifiers);
        }

        @Override
        public boolean charTyped(char codePoint, int modifiers) {
            if (!this.isVisible()) return false;
            if (this.parent.needsScrolling) {
                this.parent.scrollPosition = 0.0F;
            }
            return this.searchBox.charTyped(codePoint, modifiers);
        }

        public void resetSearchValue() {
            this.searchBox.setValue("");
            this.searchBox.setFocused(false);
        }

        public void focusAndSelectAll() {
            this.searchBox.setFocused(true);
            this.searchBox.moveCursorToEnd(false);
            this.searchBox.setHighlightPos(0);
        }

        @NotNull
        public String getSearchValue() {
            return this.searchBox.getValue();
        }

        public boolean hasIconAssigned() {
            return this.icon != null;
        }

        public SearchContextMenuEntry setIcon(@Nullable MaterialIcon icon) {
            this.icon = icon;
            return this;
        }

        public void prepareForImmediateInput(boolean addIconSpace) {
            this.addSpaceForIcon = addIconSpace;
            if (this.width <= 0.0F) {
                float fallbackWidth = Math.max(this.parent.getWidth(), this.getMinWidth());
                if (fallbackWidth <= 0.0F) {
                    fallbackWidth = this.getMinWidth();
                }
                this.width = fallbackWidth;
            }
            if (this.height <= 0.0F) {
                this.height = 20;
            }
            this.updateSearchBoxBounds();
            this.searchBox.setHighlightPos(this.searchBox.getCursorPosition());
        }

        private void updateSearchBoxBounds() {
            int paddingLeft = FIELD_HORIZONTAL_PADDING + (this.addSpaceForIcon ? ClickableContextMenuEntry.ICON_LABEL_SPACING : 0);
            int paddingRight = FIELD_HORIZONTAL_PADDING;
            int x = Math.round(this.x + paddingLeft);
            int y = Math.round(this.y + FIELD_VERTICAL_PADDING);
            int width = Math.max(MIN_FIELD_WIDTH, Math.round(this.width - paddingLeft - paddingRight));
            int height = Math.max(MIN_FIELD_HEIGHT, Math.round(this.height - FIELD_VERTICAL_PADDING * 2));
            this.searchBox.setX(x);
            this.searchBox.setY(y);
            this.searchBox.setWidth(width);
            this.searchBox.setHeight(height);
        }

        private void renderIcon(@NotNull GuiGraphics graphics) {
            IconRenderData iconData = this.resolveIconData();
            if (iconData == null) {
                return;
            }
            float areaX = this.x + ClickableContextMenuEntry.ICON_PADDING_LEFT;
            float areaY = this.y + (this.getHeight() / 2.0F) - (ClickableContextMenuEntry.ICON_WIDTH_HEIGHT / 2.0F);
            RenderSystem.enableBlend();
            UIBase.getUITheme().setUITextureShaderColor(graphics, 1.0F);
            this.blitScaledIcon(graphics, iconData, areaX, areaY, ClickableContextMenuEntry.ICON_WIDTH_HEIGHT, ClickableContextMenuEntry.ICON_WIDTH_HEIGHT);
            RenderingUtils.resetShaderColor(graphics);
        }

        @Nullable
        private IconRenderData resolveIconData() {
            if (this.icon == null) {
                return null;
            }
            float renderSize = ClickableContextMenuEntry.ICON_WIDTH_HEIGHT;
            ResourceLocation location = this.icon.getTextureLocationForUI(renderSize, renderSize);
            if (location == null) {
                return null;
            }
            int iconSize = this.icon.getTextureSizeForUI(renderSize, renderSize);
            int width = this.icon.getWidth(iconSize);
            int height = this.icon.getHeight(iconSize);
            if (width <= 0 || height <= 0) {
                return null;
            }
            return new IconRenderData(location, width, height);
        }

        private void blitScaledIcon(@NotNull GuiGraphics graphics, @NotNull IconRenderData iconData, float areaX, float areaY, float areaWidth, float areaHeight) {
            if (areaWidth <= 0.0F || areaHeight <= 0.0F) {
                return;
            }
            float scale = Math.min(areaWidth / (float) iconData.width, areaHeight / (float) iconData.height);
            if (!Float.isFinite(scale) || scale <= 0.0F) {
                return;
            }
            float scaledWidth = iconData.width * scale;
            float scaledHeight = iconData.height * scale;
            float drawX = areaX + (areaWidth - scaledWidth) * 0.5F;
            float drawY = areaY + (areaHeight - scaledHeight) * 0.5F;
            graphics.pose().pushPose();
            graphics.pose().translate(drawX, drawY, 0.0F);
            graphics.pose().scale(scale, scale, 1.0F);
            graphics.blit(iconData.texture, 0, 0, 0.0F, 0.0F, iconData.width, iconData.height, iconData.width, iconData.height);
            graphics.pose().popPose();
        }

        private void applySkinIfNeeded() {
            boolean blur = UIBase.shouldBlur();
            if (blur != this.lastBlurState) {
                this.lastBlurState = blur;
                this.applyDefaultSkin();
            }
        }

        private void applyDefaultSkin() {
            UIBase.applyDefaultWidgetSkinTo(this.searchBox, this.lastBlurState);
            this.searchBox.setBackgroundColor(DrawableColor.FULLY_TRANSPARENT);
            this.searchBox.setBorderNormalColor(DrawableColor.FULLY_TRANSPARENT);
            this.searchBox.setBorderFocusedColor(DrawableColor.FULLY_TRANSPARENT);
        }

    }

    /**
     * Metadata for a stacked entry chain.
     * <p>
     * Each entry in a stacked menu has a {@link ContextMenuStackMeta} instance. All entries in
     * the same stack share the same {@link #properties} object for coordination.
     */
    public static class ContextMenuStackMeta {

        protected RuntimePropertyContainer properties = new RuntimePropertyContainer();
        protected boolean stackable = false;
        protected boolean partOfStack = false;
        protected boolean firstInStack = true;
        protected boolean lastInStack = true;
        protected ContextMenuEntry<?> nextInStack;

        /**
         * This is a shared instance. Every entry in the stack has access to the same
         * {@link RuntimePropertyContainer} instance.
         */
        @NotNull
        public RuntimePropertyContainer getProperties() {
            return this.properties;
        }

        /**
         * @return true if this entry is part of a stack.
         */
        public boolean isPartOfStack() {
            return this.partOfStack;
        }

        /**
         * @return true if this is the first entry in the stack (the one rendered in the menu).
         */
        public boolean isFirstInStack() {
            return this.firstInStack;
        }

        /**
         * @return true if this is the last entry in the stack.
         */
        public boolean isLastInStack() {
            return this.lastInStack;
        }

        /**
         * @return true if this entry is marked as stackable.
         */
        public boolean isStackable() {
            return this.stackable;
        }

        /**
         * Sets whether this entry can be stacked.
         */
        public void setStackable(boolean stackable) {
            this.stackable = stackable;
        }

        /**
         * @return the next entry in the stack chain, or null if this is the last.
         */
        @Nullable
        public ContextMenuEntry<?> getNextInStack() {
            return this.nextInStack;
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

    /**
     * Stack applier used to apply a new value on a stack entry.
     * <p>
     * The entry argument is the specific stack entry being applied.
     *
     * <p><b>Example</b>
     * <pre>{@code
     * entry.setStackApplier((stackEntry, value) -> {
     *     if (value instanceof Integer i) {
     *         builder.self().setPadding(i);
     *     }
     * });
     * }</pre>
     */
    @FunctionalInterface
    public interface StackApplier {
        void apply(ContextMenuEntry<?> entry, @Nullable Object value);
    }

    /**
     * Supplies the current value for a stack entry, used to detect mixed state.
     *
     * <p><b>Example</b>
     * <pre>{@code
     * entry.setStackValueSupplier(stackEntry -> builder.self().getPadding());
     * }</pre>
     */
    @FunctionalInterface
    public interface StackValueSupplier {
        @Nullable
        Object get(ContextMenuEntry<?> entry);
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
            return ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/contextmenu/icons/" + iconName + ".png");
        }
    }

}
