package de.keksuccino.fancymenu.customization.element.elements.scrollarea;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.ElementSpaceInputListener;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoint;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.element.container.ElementContainer;
import de.keksuccino.fancymenu.customization.element.container.ElementContainerDrawScale;
import de.keksuccino.fancymenu.customization.element.container.ElementContainerHierarchy;
import de.keksuccino.fancymenu.customization.element.container.ElementContainerLink;
import de.keksuccino.fancymenu.customization.element.elements.scrollarea.layout.ScrollAreaAlignment;
import de.keksuccino.fancymenu.customization.element.elements.scrollarea.layout.ScrollAreaArranger;
import de.keksuccino.fancymenu.customization.element.elements.scrollarea.layout.ScrollAreaLayoutMode;
import de.keksuccino.fancymenu.customization.element.elements.scrollarea.layout.ScrollAreaResizeBehavior;
import de.keksuccino.fancymenu.customization.element.style.ElementPanelBackground;
import de.keksuccino.fancymenu.customization.element.style.ScrollBarGrabberStyle;
import de.keksuccino.fancymenu.customization.element.style.ScrollBarTrackBackground;
import de.keksuccino.fancymenu.customization.element.scrolling.ElementScrollBars;
import de.keksuccino.fancymenu.customization.element.scrolling.ElementScrollInput;
import de.keksuccino.fancymenu.customization.element.scrolling.ElementScrolling;
import de.keksuccino.fancymenu.customization.element.scrolling.ScrollDirection;
import de.keksuccino.fancymenu.customization.element.scrolling.ScrollEdgeShadows;
import de.keksuccino.fancymenu.customization.element.scrolling.ScrollGeometry;
import de.keksuccino.fancymenu.customization.element.scrolling.ScrollSettings;
import de.keksuccino.fancymenu.customization.element.scrolling.ScrollSnapMode;
import de.keksuccino.fancymenu.customization.element.scrolling.ScrollSnapPoints;
import de.keksuccino.fancymenu.customization.element.scrolling.ScrollableElement;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.ScreenUtils;
import de.keksuccino.fancymenu.util.rendering.RenderFrameCounter;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.FancyMenuUiComponent;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollbar.ScrollBar;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * An area that holds other elements and shows a part of them: its children live in a content that can be bigger than the area,
 * and the area scrolls over it. Everything of a child outside of the area is cut off.<br><br>
 *
 * The area works out its geometry once per frame, in {@link #updateFrame()}, from the size it has in that frame. Its children ask it for
 * that geometry every time they calculate their position, so nothing about the size of the area or its content is remembered longer than a frame.
 */
public class ScrollAreaElement extends AbstractElement implements ElementContainer, ScrollableElement {

    /** Where the mouse is for the area as long as nothing told it: far away from everything. */
    protected static final int MOUSE_OUTSIDE = -100000;
    /** The frame an area that does not show its children last showed them in: none. */
    private static final long NEVER_SHOWN = -2L;
    /** The smallest a proportional content gets. Smaller than this, nothing in it can be told apart anymore, and a scale of zero would break all math. */
    private static final float MIN_PROPORTIONAL_SCALE = 0.05F;
    /** Where the content of an area can come to rest. It holds elements and no text, so it has neither lines nor headlines. */
    private static final Set<ScrollSnapMode> SNAP_MODES = Collections.unmodifiableSet(EnumSet.of(ScrollSnapMode.NONE, ScrollSnapMode.ELEMENTS, ScrollSnapMode.PAGES));

    public final ScrollSettings scrollSettings = new ScrollSettings(this, "fancymenu.elements.scroll_area", ScrollDirection.VERTICAL, SNAP_MODES);
    /** The size of the content in the units of the content. Only counts on axes the area scrolls on, and never makes the content smaller than the area. */
    public final Property.IntegerProperty contentWidth = putProperty(Property.integerProperty("content_width", 0, "fancymenu.elements.scroll_area.content_width", Property.NumericInputBehavior.<Integer>builder().freeInput().build()));
    public final Property.IntegerProperty contentHeight = putProperty(Property.integerProperty("content_height", 0, "fancymenu.elements.scroll_area.content_height", Property.NumericInputBehavior.<Integer>builder().freeInput().build()));
    public final Property<ScrollAreaLayoutMode> layoutMode = putProperty(ScrollAreaLayoutMode.buildProperty());
    public final Property<ScrollAreaAlignment> childAlignment = putProperty(ScrollAreaAlignment.buildProperty());
    public final Property.IntegerProperty contentPadding = putProperty(Property.integerProperty("content_padding", 4, "fancymenu.elements.scroll_area.content_padding", Property.NumericInputBehavior.<Integer>builder().rangeInput(0, 64).build()));
    public final Property.IntegerProperty childSpacing = putProperty(Property.integerProperty("child_spacing", 4, "fancymenu.elements.scroll_area.child_spacing", Property.NumericInputBehavior.<Integer>builder().rangeInput(0, 64).build()));
    public final Property.IntegerProperty gridLines = putProperty(Property.integerProperty("grid_lines", 3, "fancymenu.elements.scroll_area.grid_lines", Property.NumericInputBehavior.<Integer>builder().rangeInput(1, 12).build()));
    public final Property.BooleanProperty gridAutoFit = putProperty(Property.booleanProperty("grid_auto_fit", false, "fancymenu.elements.scroll_area.grid_auto_fit"));
    public final Property.IntegerProperty gridMinCellSize = putProperty(Property.integerProperty("grid_min_cell_size", 60, "fancymenu.elements.scroll_area.grid_min_cell_size", Property.NumericInputBehavior.<Integer>builder().rangeInput(10, 400).step(5).build()));
    public final Property<ScrollAreaResizeBehavior> resizeBehavior = putProperty(ScrollAreaResizeBehavior.buildProperty());
    /**
     * The size the area had when its proportional content got made, in the units of the content. The content is as big on the screen
     * as the area is in relation to this, so at this size one unit of the content is one screen pixel.
     */
    public final Property.IntegerProperty proportionalReferenceWidth = putProperty(Property.integerProperty("proportional_reference_width", 0, "fancymenu.elements.scroll_area.proportional_reference"));
    public final Property.IntegerProperty proportionalReferenceHeight = putProperty(Property.integerProperty("proportional_reference_height", 0, "fancymenu.elements.scroll_area.proportional_reference"));
    /** If the layer list of the layout editor shows the area without the layers of its children. */
    public final Property.BooleanProperty collapsedInLayerList = putProperty(Property.booleanProperty("collapsed_in_layer_list", false, "fancymenu.elements.scroll_area.collapsed_in_layer_list"));
    public final ScrollBarGrabberStyle grabberStyle = new ScrollBarGrabberStyle(this);
    public final ScrollBarTrackBackground scrollBarBackground = new ScrollBarTrackBackground(this);
    public final ElementPanelBackground background = new ElementPanelBackground(this, "fancymenu.elements.scroll_area.background", this::getScrollBarSpace);

    protected final ElementScrolling scrolling = new ElementScrolling(this);
    protected final ScrollGeometry geometry = new ScrollGeometry();
    protected final ElementScrollBars bars = new ElementScrollBars(this, this.scrolling.getScroller());
    protected final ElementScrollInput input = new ElementScrollInput(this);
    protected final ScrollAreaChildLayout childLayout = new ScrollAreaChildLayout();
    protected final List<AbstractElement> children = new ArrayList<>();
    @NotNull
    protected ChildRenderer childRenderer = AbstractElement::renderInternal;
    /** Renders something into the area while it has no children. Only the layout editor has something to show there. */
    @Nullable
    protected EmptyContentRenderer emptyContentRenderer;
    protected boolean frameReady = false;
    /** True while the geometry gets worked out. Children that ask for it in that time get the geometry of the frame before. */
    protected boolean updatingFrame = false;
    /**
     * The frame the area last rendered its children in. The area shows its children as long as this is not more than one frame ago:
     * input comes in between frames, and children that come before the area in a frame ask before the area rendered in it.
     */
    protected long lastChildrenShownFrame = NEVER_SHOWN;
    /** If the area rendered its children since {@link #renderInternal(GuiGraphics, int, int, float)} started. */
    protected boolean renderedChildren = false;
    protected float childOpacity = 1.0F;
    /** Where the mouse was when the area rendered the last time, for what moves the content along with it. */
    protected int frameMouseX = MOUSE_OUTSIDE;
    protected int frameMouseY = MOUSE_OUTSIDE;
    /** Where a child that must not react to the mouse gets told the mouse is, X and Y. */
    protected final int[] movedMouse = new int[2];

    public ScrollAreaElement(@NotNull ElementBuilder<?, ?> builder) {
        super(builder);
        this.allowDepthTestManipulation = true;
        // The area cuts its children to a rectangle on the screen, which a rotated or tilted area would not be
        this.setSupportsRotation(false);
        this.setSupportsTilting(false);
        // Arranged children run in one direction, so an area that arranges them goes on to the next direction when it gets set to both
        this.scrollSettings.scrollDirection.setValueSetProcessor(direction -> ((direction == ScrollDirection.BOTH) && !this.getLayoutMode().supportsBothDirections()) ? ScrollDirection.VERTICAL : direction);
        // The same goes for an alignment the mode of the area has no use for
        this.childAlignment.setValueSetProcessor(alignment -> ((alignment == ScrollAreaAlignment.FILL) && !this.getLayoutMode().supportsFillAlignment()) ? ScrollAreaAlignment.START : alignment);
    }

    @Override
    public void afterConstruction() {
        this.scrolling.restoreFromMemory();
    }

    @Override
    public void onOpenScreen() {
        if (this.scrollSettings.resetScrollOnOpen.getBoolean()) this.scrolling.resetToStartPosition();
    }

    /**
     * An area that does not render still takes its children through their frame. They do not show in it and notice that there,
     * which is what stops their sounds and videos and starts their delays and fades over, like it does for every element that stops showing.
     */
    @Override
    public void renderInternal(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        this.renderedChildren = false;
        super.renderInternal(graphics, mouseX, mouseY, partial);
        if (this.renderedChildren) return;
        // Has to be known before the children get asked if they show
        this.lastChildrenShownFrame = NEVER_SHOWN;
        for (int i = 0; i < this.children.size(); i++) {
            this.childRenderer.render(this.children.get(i), graphics, mouseX, mouseY, partial);
        }
    }

    @Override
    public void onRenderInternalSkipped() {
        this.lastChildrenShownFrame = NEVER_SHOWN;
        for (int i = 0; i < this.children.size(); i++) {
            this.children.get(i).onRenderInternalSkipped();
        }
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        // Has to be known before the frame gets worked out: children only take part in an arrangement while the area shows them
        this.lastChildrenShownFrame = RenderFrameCounter.getFrame();
        this.renderedChildren = true;
        // The opacity includes the one of the containers this area is in while it renders, which is the only time children ask for it
        this.childOpacity = this.opacity;
        this.frameMouseX = mouseX;
        this.frameMouseY = mouseY;

        // A grabber the user holds follows the mouse where it really is, before the frame gets worked out with where that takes the content
        this.bars.followPointer();

        this.updateFrame();

        ScrollGeometry geometry = this.geometry;
        int viewportX = geometry.getViewportX();
        int viewportY = geometry.getViewportY();

        // The bars have to know where they are before the background gets drawn, which reaches behind the ones that show
        this.bars.update(geometry, this.opacity);
        this.background.render(graphics, viewportX, viewportY, geometry.getViewportWidth(), geometry.getViewportHeight(), this.bars.isVerticalShown(), this.bars.isHorizontalShown(), this.scrollSettings.verticalScrollBarLeft.getBoolean(), this.scrollSettings.horizontalScrollBarTop.getBoolean(), this.opacity);
        boolean mouseOverContent = geometry.isInViewport(mouseX, mouseY) && this.isScreenPointVisibleInContainer(mouseX, mouseY);
        // Nothing in the content reacts to a mouse that is not over it, or that drags the content along
        boolean childrenSeeMouse = mouseOverContent && !this.scrolling.isDragging();

        if (this.children.isEmpty() && (this.emptyContentRenderer != null)) {
            this.emptyContentRenderer.render(graphics, viewportX, viewportY, geometry.getViewportWidth(), geometry.getViewportHeight(), mouseOverContent);
        }

        graphics.enableScissor(viewportX, viewportY, viewportX + geometry.getViewportWidth(), viewportY + geometry.getViewportHeight());
        // The area draws itself without the depth test, like most elements do. Its children draw the way they do outside of an area, where some of them need it.
        RenderingUtils.setDepthTestLocked(false);
        com.mojang.blaze3d.opengl.GlStateManager._enableDepthTest();
        try {
            for (int i = 0; i < this.children.size(); i++) {
                AbstractElement child = this.children.get(i);
                if (childrenSeeMouse) {
                    this.renderChild(child, graphics, mouseX, mouseY, partial);
                } else {
                    moveOutOf(child, mouseX, mouseY, this.movedMouse);
                    this.renderChild(child, graphics, this.movedMouse[0], this.movedMouse[1], partial);
                }
            }
        } finally {
            com.mojang.blaze3d.opengl.GlStateManager._disableDepthTest();
            RenderingUtils.setDepthTestLocked(true);
            graphics.disableScissor();
        }

        if (this.scrollSettings.edgeShadows.getBoolean()) {
            ScrollEdgeShadows.render(graphics, geometry, this.scrolling.getScroller(), this.scrollSettings.edgeShadowColor.getDrawable().getColorInt(), this.opacity);
        }

        this.bars.render(graphics, mouseX, mouseY, partial);

    }

    /**
     * Works out where a child gets told the mouse is while it must not react to it: where it is, unless that is in the child,
     * where it becomes the closest place next to the child. The child is not hovered there, and what follows the mouse in it,
     * like eyes that look at it, still looks the right way.
     *
     * @param moved Gets filled with the X and the Y position the child gets told.
     */
    protected static void moveOutOf(@NotNull AbstractElement child, int mouseX, int mouseY, int @NotNull [] moved) {
        moved[0] = mouseX;
        moved[1] = mouseY;
        int childX = child.getAbsoluteX();
        int childY = child.getAbsoluteY();
        int toLeft = mouseX - childX;
        int toTop = mouseY - childY;
        // The mouse is in the child while it is at or behind its left and top edge and in front of its right and bottom edge
        if ((toLeft < 0) || (toTop < 0)) return;
        int toRight = (childX + child.getAbsoluteWidth()) - mouseX;
        int toBottom = (childY + child.getAbsoluteHeight()) - mouseY;
        if ((toRight <= 0) || (toBottom <= 0)) return;
        int nearest = Math.min(Math.min(toLeft + 1, toRight), Math.min(toTop + 1, toBottom));
        if (nearest == (toLeft + 1)) {
            moved[0] = mouseX - nearest;
        } else if (nearest == toRight) {
            moved[0] = mouseX + nearest;
        } else if (nearest == (toTop + 1)) {
            moved[1] = mouseY - nearest;
        } else {
            moved[1] = mouseY + nearest;
        }
    }

    /**
     * Renders a child. Children that draw scaled by the area get the scale on the pose, around their top-left corner,
     * and are in the unscaled space while they render: they report the size they have in the units of the content
     * and get the mouse where it is in that space.
     */
    protected void renderChild(@NotNull AbstractElement child, @NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        this.renderChild(child, graphics, mouseX, mouseY, partial, this.childRenderer);
    }

    /**
     * Renders a child with the given renderer, in the space the area gives its children. Does not cut anything of the child off.
     */
    public void renderChild(@NotNull AbstractElement child, @NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial, @NotNull ChildRenderer renderer) {
        ElementContainerLink link = child.getContainerLink();
        if ((link == null) || !ElementContainerLink.drawsScaled(child, this)) {
            renderer.render(child, graphics, mouseX, mouseY, partial);
            return;
        }
        float scale = this.geometry.getScale();
        int x = child.getAbsoluteX();
        int y = child.getAbsoluteY();
        int screenWidth = child.getAbsoluteWidth();
        int screenHeight = child.getAbsoluteHeight();
        graphics.pose().pushMatrix();
        ElementContainerLink.SizeSpace previousSpace = link.setSizeSpace(ElementContainerLink.SizeSpace.CONTENT);
        try {
            int scaledMouseX = toUnscaledSpace(mouseX, x, screenWidth, child.getAbsoluteWidth(), scale);
            int scaledMouseY = toUnscaledSpace(mouseY, y, screenHeight, child.getAbsoluteHeight(), scale);
            graphics.pose().translate(x, y);
            graphics.pose().scale(scale, scale);
            graphics.pose().translate(-x, -y);
            // What the child cuts and what it checks the mouse against are places in its unscaled space, which have to be found on the screen
            ElementContainerDrawScale.begin(scale, x, y);
            renderer.render(child, graphics, scaledMouseX, scaledMouseY, partial);
        } finally {
            ElementContainerDrawScale.end();
            link.setSizeSpace(previousSpace);
            graphics.pose().popMatrix();
        }
    }

    /**
     * Maps the mouse into the unscaled space a child renders in, on one axis.<br>
     * The child has a whole number of pixels in both spaces, so its last pixel on the screen can map to the place next to it, and the
     * place next to it can map into it. A mouse that is in the child on the screen is in it there as well, and one that is not is not.
     *
     * @param start Where the child starts, which is the same in both spaces.
     */
    protected static int toUnscaledSpace(int mouse, int start, int screenSize, int unscaledSize, float scale) {
        int mapped = (int) Math.floor(start + ((mouse - start) / scale));
        if (mouse >= (start + screenSize)) return Math.max(mapped, start + unscaledSize);
        if (mouse >= start) return Math.min(mapped, (start + unscaledSize) - 1);
        return mapped;
    }

    /**
     * Works out the geometry of the area for this frame and moves the content towards where it is scrolling to.
     */
    protected void updateFrame() {

        if (this.updatingFrame) return;
        this.updatingFrame = true;
        try {

            ElementContainer parent = this.getParentContainer();
            float parentScale = (parent != null) ? parent.getContentScale() : 1.0F;
            ScrollAreaLayoutMode mode = this.getLayoutMode();
            ScrollDirection direction = this.getScrollDirection();
            int width = this.getAbsoluteWidth();
            int height = this.getAbsoluteHeight();
            float scale = parentScale;
            if (mode == ScrollAreaLayoutMode.PROPORTIONAL) scale *= this.calculateProportionalScale(width / parentScale, height / parentScale, direction);
            this.geometry.setViewport(this.getAbsoluteX(), this.getAbsoluteY(), width, height, scale);

            int wantedContentWidth = this.contentWidth.getInteger();
            int wantedContentHeight = this.contentHeight.getInteger();
            ScrollAreaArranger arranger = mode.getArranger();
            if (arranger != null) {
                this.childLayout.arrange(this, arranger, this.children, this.geometry);
                if (!mode.hasUserContentSize()) {
                    wantedContentWidth = this.childLayout.getContentWidth();
                    wantedContentHeight = this.childLayout.getContentHeight();
                }
            } else {
                this.childLayout.clear(this.children);
            }
            this.geometry.setContentSize(wantedContentWidth, wantedContentHeight, direction.scrollsX(), direction.scrollsY());

            this.scrolling.update(this.geometry, this.geometry.isInViewport(this.frameMouseX, this.frameMouseY));

            this.frameReady = true;

        } finally {
            this.updatingFrame = false;
        }

    }

    /**
     * Makes sure there is a geometry to answer with for everything that asks before the area rendered for the first time.
     */
    protected void ensureFrame() {
        if (!this.frameReady) this.updateFrame();
    }

    /**
     * Offers a scroll of the mouse wheel to what is in the area under the mouse and scrolls on its own, like a text.
     * The area gets every scroll of the screen before the screen hands it to what is under the mouse, so it has to do that for its children itself.
     *
     * @return If something in the area took the scroll.
     */
    @Override
    public boolean scrollContentByWheel(double mouseX, double mouseY, double deltaX, double deltaY) {
        Screen screen = ScreenUtils.getScreen();
        if (screen == null) return false;
        for (GuiEventListener listener : screen.children()) {
            // FancyMenu UI components get every scroll on their own, in front of the area if they are in it
            if ((listener instanceof FancyMenuUiComponent) || !listener.isMouseOver(mouseX, mouseY)) continue;
            if ((this.findChildOwning(listener) != null) && listener.mouseScrolled(mouseX, mouseY, deltaX, deltaY)) return true;
        }
        return false;
    }

    /**
     * Returns the direction the area scrolls in. An area that arranges its children does it in one direction and only scrolls in that one,
     * so it scrolls up and down when it is set to both.
     */
    @Override
    @NotNull
    public ScrollDirection getScrollDirection() {
        ScrollDirection direction = this.scrollSettings.scrollDirection.tryGetNonNullElse(ScrollDirection.VERTICAL);
        return ((direction == ScrollDirection.BOTH) && !this.getLayoutMode().supportsBothDirections()) ? ScrollDirection.VERTICAL : direction;
    }

    /**
     * Returns the space a scroll bar of the area has its grabber in, next to the area.
     */
    public int getScrollBarSpace() {
        return this.bars.getBarSpace();
    }

    @NotNull
    public ScrollAreaLayoutMode getLayoutMode() {
        return this.layoutMode.tryGetNonNullElse(ScrollAreaLayoutMode.ANCHORS);
    }

    /**
     * Returns where the children sit across the direction they line up in. Children that flow keep the size they have,
     * so an area that is set to fill its lines with them puts them at the start of their lines.
     */
    @NotNull
    public ScrollAreaAlignment getChildAlignment() {
        ScrollAreaAlignment alignment = this.childAlignment.tryGetNonNullElse(ScrollAreaAlignment.START);
        return ((alignment == ScrollAreaAlignment.FILL) && !this.getLayoutMode().supportsFillAlignment()) ? ScrollAreaAlignment.START : alignment;
    }

    /**
     * Returns if the content is as big as the user sets it. If not, the area works its size out from its children.
     */
    public boolean hasUserContentSize() {
        return this.getLayoutMode().hasUserContentSize();
    }

    /**
     * Returns if the child has a place in what the area arranges. Children that do not show leave no gap behind.
     */
    public boolean takesPartInArrangement(@NotNull AbstractElement child) {
        if (isEditor()) return !child.layerHiddenInEditor;
        return child.shouldRender();
    }

    /**
     * Returns how many screen pixels one unit of a proportional content is, for an area of the given size.<br>
     * The content follows the size the area has across the direction it scrolls in, which keeps its proportions:
     * its width for an area that scrolls up and down, its height for one that scrolls sideways, and the smaller of both for one that does both.
     */
    protected float calculateProportionalScale(float width, float height, @NotNull ScrollDirection direction) {
        int referenceWidth = this.proportionalReferenceWidth.getInteger();
        int referenceHeight = this.proportionalReferenceHeight.getInteger();
        float scaleX = (referenceWidth > 0) ? (width / referenceWidth) : 1.0F;
        float scaleY = (referenceHeight > 0) ? (height / referenceHeight) : 1.0F;
        float scale = (direction == ScrollDirection.VERTICAL) ? scaleX : ((direction == ScrollDirection.HORIZONTAL) ? scaleY : Math.min(scaleX, scaleY));
        return Math.max(MIN_PROPORTIONAL_SCALE, scale);
    }

    /**
     * Makes the size the area has right now the size its proportional content is made for, at which the content shows unscaled.
     */
    public void captureProportionalReference() {
        ElementContainer parent = this.getParentContainer();
        float parentScale = (parent != null) ? parent.getContentScale() : 1.0F;
        this.proportionalReferenceWidth.set(Math.max(1, Math.round(this.getAbsoluteWidth() / parentScale)));
        this.proportionalReferenceHeight.set(Math.max(1, Math.round(this.getAbsoluteHeight() / parentScale)));
    }

    /**
     * Works out the geometry of the area again right now, for changes that have to show before the next frame.
     */
    public void refreshFrame() {
        this.childLayout.invalidate();
        this.updateFrame();
    }

    /**
     * Returns if the size comes from something the user typed that is no plain number, like a placeholder.
     * Such a size changes on its own, so nothing in the editor should replace it with a number.
     */
    public static boolean isAdvancedSize(@NotNull Property.IntegerProperty size) {
        String input = size.getManualInput();
        return (input != null) && !MathUtils.isInteger(input.trim());
    }

    /**
     * Makes the content end where the last of its children ends, on the axes the area scrolls on.
     * Sizes that come from a placeholder stay as they are.<br>
     * Only children that are placed from the start of the content count. A child that is anchored to the middle or the end
     * of the content moves when the content changes its size, so it has no place the content could end at.
     */
    public void fitContentToChildren() {
        this.ensureFrame();
        int right = 0;
        int bottom = 0;
        for (int i = 0; i < this.children.size(); i++) {
            AbstractElement child = this.children.get(i);
            if (isPlacedFromContentStart(child, true)) right = Math.max(right, ElementContainerLink.toContentX(this, child.getAbsoluteX()) + child.getContentSpaceWidth());
            if (isPlacedFromContentStart(child, false)) bottom = Math.max(bottom, ElementContainerLink.toContentY(this, child.getAbsoluteY()) + child.getContentSpaceHeight());
        }
        ScrollDirection direction = this.getScrollDirection();
        if (direction.scrollsX() && !isAdvancedSize(this.contentWidth)) {
            this.contentWidth.set(right);
        }
        if (direction.scrollsY() && !isAdvancedSize(this.contentHeight)) {
            this.contentHeight.set(bottom);
        }
    }

    /**
     * Returns if the position of the child on the given axis stays the same when the content changes its size on that axis.
     */
    protected static boolean isPlacedFromContentStart(@NotNull AbstractElement child, boolean horizontal) {
        if (horizontal ? child.isStretchedX() : child.isStretchedY()) return false;
        if (horizontal ? !child.advancedX.isDefault() : !child.advancedY.isDefault()) return true;
        ElementAnchorPoint anchor = child.anchorPoint;
        if ((anchor == null) || (anchor == ElementAnchorPoints.ELEMENT)) return true;
        if (horizontal) return (anchor == ElementAnchorPoints.TOP_LEFT) || (anchor == ElementAnchorPoints.MID_LEFT) || (anchor == ElementAnchorPoints.BOTTOM_LEFT);
        return (anchor == ElementAnchorPoints.TOP_LEFT) || (anchor == ElementAnchorPoints.TOP_CENTERED) || (anchor == ElementAnchorPoints.TOP_RIGHT);
    }

    @NotNull
    public ElementScrollInput getScrollInput() {
        return this.input;
    }

    /**
     * Sets what renders into the area while it has no children.
     */
    public void setEmptyContentRenderer(@Nullable EmptyContentRenderer emptyContentRenderer) {
        this.emptyContentRenderer = emptyContentRenderer;
    }

    /**
     * Returns the child the given widget belongs to: one of its own widgets, or a widget of something in a child that is an area itself.
     * Returns NULL if the widget belongs to nothing in the area.
     *
     * @param widget The widget, or what it is registered to the screen as.
     */
    @Override
    @Nullable
    public AbstractElement findChildOwning(@NotNull GuiEventListener widget) {
        // Screens know widgets of children as the listener that maps their input, and elements can hand their widgets out as one as well
        GuiEventListener searched = ElementSpaceInputListener.unwrap(widget);
        for (int i = 0; i < this.children.size(); i++) {
            AbstractElement child = this.children.get(i);
            if ((child instanceof ScrollAreaElement inner) && (inner.findChildOwning(searched) != null)) return child;
            List<GuiEventListener> widgets = child.getWidgetsToRegister();
            if (widgets == null) continue;
            for (GuiEventListener childWidget : widgets) {
                if (ElementSpaceInputListener.unwrap(childWidget) == searched) return child;
            }
        }
        return null;
    }

    /**
     * Returns if something in the area takes a press of the mouse at the given position on the screen: a widget of a child,
     * or an area in the area.
     */
    @Override
    public boolean takesContentPressAt(double screenX, double screenY) {
        // What scrolls takes presses without a widget that is hovered, so the areas and texts in the area get asked on their own
        for (int i = 0; i < this.children.size(); i++) {
            AbstractElement child = this.children.get(i);
            if ((child instanceof ScrollableElement) && child.takesMousePressAt(screenX, screenY)) return true;
        }
        Screen screen = ScreenUtils.getScreen();
        if (screen == null) return false;
        for (GuiEventListener listener : screen.children()) {
            if (!listener.isMouseOver(screenX, screenY)) continue;
            AbstractElement child = this.findChildOwning(listener);
            if ((child != null) && child.takesMousePressAt(screenX, screenY)) return true;
        }
        return false;
    }

    /**
     * A press is for the area where it grabs one of its scroll bars, where it starts dragging its content,
     * and where it is for something in the area.
     */
    @Override
    public boolean takesMousePressAt(double screenX, double screenY) {
        if (!this.isShowingChildren()) return false;
        if (this.bars.isUsedByMouse()) return true;
        if (!this.isScreenPointVisible(screenX, screenY)) return false;
        return this.scrolling.canDragContent() || this.takesContentPressAt(screenX, screenY);
    }

    @Override
    public @NotNull AbstractElement getScrollElement() {
        return this;
    }

    @Override
    public @NotNull ScrollSettings getScrollSettings() {
        return this.scrollSettings;
    }

    @Override
    public @NotNull ElementScrolling getScrolling() {
        return this.scrolling;
    }

    @Override
    public @NotNull ElementScrollBars getScrollBars() {
        return this.bars;
    }

    @Override
    public @NotNull ScrollGeometry getScrollGeometry() {
        this.ensureFrame();
        return this.geometry;
    }

    @Override
    public boolean isShowingScrollContent() {
        return this.isShowingChildren();
    }

    @Override
    public boolean isInEditor() {
        return isEditor();
    }

    @Override
    public int getScrollBarDistance() {
        return this.background.getScrollBarDistance();
    }

    @Override
    public @NotNull ScrollBarTrackBackground getScrollBarBackground() {
        return this.scrollBarBackground;
    }

    @Override
    public void applyScrollGrabberLooks(@NotNull ScrollBar vertical, @NotNull ScrollBar horizontal, float opacity) {
        this.grabberStyle.apply(vertical, horizontal, opacity);
    }

    /**
     * The content comes to rest with one of the children at the start of what the area shows.
     */
    @Override
    public void addSnapPoints(@NotNull ScrollSnapPoints points, boolean horizontal) {
        ScrollGeometry geometry = this.getScrollGeometry();
        // Arranged children have a padding in front of the first one, which stays in sight in front of all of them
        int padding = this.arrangesChildren() ? Math.round(this.contentPadding.getInteger() * geometry.getScale()) : 0;
        int contentStart = horizontal ? geometry.getContentScreenX() : geometry.getContentScreenY();
        for (int i = 0; i < this.children.size(); i++) {
            AbstractElement child = this.children.get(i);
            if (!this.takesPartInArrangement(child)) continue;
            points.add(((horizontal ? child.getAbsoluteX() : child.getAbsoluteY()) - contentStart) - padding);
        }
    }

    @Override
    public boolean isScrollPointVisible(double screenX, double screenY) {
        return this.isScreenPointVisible(screenX, screenY);
    }

    @Override
    public void scrollToElement(@NotNull AbstractElement element, boolean animate) {
        if (ElementContainerHierarchy.isInContainer(element, this)) this.scrolling.scrollToChild(element, animate);
    }

    /**
     * Returns the children of the area, in the order they render in. The list belongs to the area and must not be changed.
     */
    @NotNull
    public List<AbstractElement> getChildren() {
        return this.children;
    }

    /**
     * Sets what renders the children of the area. The layout editor renders them as what they are in the editor.
     */
    public void setChildRenderer(@Nullable ChildRenderer childRenderer) {
        this.childRenderer = (childRenderer != null) ? childRenderer : AbstractElement::renderInternal;
    }

    @Override
    public @Nullable List<GuiEventListener> getWidgetsToRegister() {
        return List.of(this.input);
    }

    @Override
    public boolean supportsPoseScaling() {
        // The area cuts its children with a scissor and positions them on the screen, which both happen past the pose
        return false;
    }

    @Override
    public @NotNull AbstractElement getContainerElement() {
        return this;
    }

    @Override
    public void setContainerChildren(@NotNull List<? extends AbstractElement> children) {
        this.children.clear();
        this.children.addAll(children);
    }

    @Override
    public int getContentWidth() {
        this.ensureFrame();
        return this.geometry.getContentWidth();
    }

    @Override
    public int getContentHeight() {
        this.ensureFrame();
        return this.geometry.getContentHeight();
    }

    @Override
    public int getContentScreenX() {
        this.ensureFrame();
        return this.geometry.getContentScreenX();
    }

    @Override
    public int getContentScreenY() {
        this.ensureFrame();
        return this.geometry.getContentScreenY();
    }

    @Override
    public float getContentScale() {
        this.ensureFrame();
        return this.geometry.getScale();
    }

    @Override
    public boolean scalesChildVisuals() {
        if ((this.getLayoutMode() == ScrollAreaLayoutMode.PROPORTIONAL) && (this.resizeBehavior.tryGetNonNullElse(ScrollAreaResizeBehavior.SCALE) == ScrollAreaResizeBehavior.SCALE)) return true;
        ElementContainer parent = this.getParentContainer();
        return (parent != null) && parent.scalesChildVisuals();
    }

    @Override
    public boolean managesChildBoxes() {
        return this.getLayoutMode().getArranger() != null;
    }

    @Override
    public void refreshChildBoxes() {
        this.refreshFrame();
    }

    @Override
    public boolean arrangesChildren() {
        return this.getLayoutMode().arrangesChildren();
    }

    @Override
    public boolean isScreenPointVisible(double screenX, double screenY) {
        this.ensureFrame();
        return this.geometry.isInViewport(screenX, screenY) && this.isScreenPointVisibleInContainer(screenX, screenY);
    }

    @Override
    public boolean clipToShownArea(int @NotNull [] area) {
        this.ensureFrame();
        ScrollGeometry geometry = this.geometry;
        area[0] = Math.max(area[0], geometry.getViewportX());
        area[1] = Math.max(area[1], geometry.getViewportY());
        area[2] = Math.min(area[2], geometry.getViewportX() + geometry.getViewportWidth());
        area[3] = Math.min(area[3], geometry.getViewportY() + geometry.getViewportHeight());
        if ((area[0] >= area[2]) || (area[1] >= area[3])) return false;
        ElementContainer parent = this.getParentContainer();
        return (parent == null) || parent.clipToShownArea(area);
    }

    @Override
    public boolean isShowingChildren() {
        return (RenderFrameCounter.getFrame() - this.lastChildrenShownFrame) <= 1L;
    }

    @Override
    public float getChildOpacity() {
        return this.childOpacity;
    }

    @Override
    public boolean isCollapsedInLayerList() {
        return this.collapsedInLayerList.getBoolean();
    }

    @Override
    public void setCollapsedInLayerList(boolean collapsed) {
        this.collapsedInLayerList.set(collapsed);
    }

    /**
     * Renders something into a scrollable area that has no children.
     */
    @FunctionalInterface
    public interface EmptyContentRenderer {

        /**
         * @param mouseOver If the mouse is over the area.
         */
        void render(@NotNull GuiGraphics graphics, int x, int y, int width, int height, boolean mouseOver);

    }

    /**
     * Renders a child of a scrollable area.
     */
    @FunctionalInterface
    public interface ChildRenderer {

        void render(@NotNull AbstractElement child, @NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial);

    }

}
