package de.keksuccino.fancymenu.customization.element.elements.scrollarea;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.ElementSpaceInputListener;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoint;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.element.container.ElementContainer;
import de.keksuccino.fancymenu.customization.element.container.ElementContainerHierarchy;
import de.keksuccino.fancymenu.customization.element.container.ElementContainerLink;
import de.keksuccino.fancymenu.customization.element.elements.scrollarea.layout.ScrollAreaAlignment;
import de.keksuccino.fancymenu.customization.element.elements.scrollarea.layout.ScrollAreaArranger;
import de.keksuccino.fancymenu.customization.element.elements.scrollarea.layout.ScrollAreaLayoutMode;
import de.keksuccino.fancymenu.customization.element.elements.scrollarea.layout.ScrollAreaResizeBehavior;
import de.keksuccino.fancymenu.customization.element.style.ElementPanelBackground;
import de.keksuccino.fancymenu.customization.element.style.PixelScrollGrabber;
import de.keksuccino.fancymenu.customization.element.style.ScrollBarGrabberStyle;
import de.keksuccino.fancymenu.customization.element.style.ScrollBarTrackBackground;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.ScreenUtils;
import de.keksuccino.fancymenu.util.rendering.RenderFrameCounter;
import de.keksuccino.fancymenu.util.window.WindowHandler;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * An area that holds other elements and shows a part of them: its children live in a content that can be bigger than the area,
 * and the area scrolls over it. Everything of a child outside of the area is cut off.<br><br>
 *
 * The area works out its geometry once per frame, in {@link #updateFrame()}, from the size it has in that frame. Its children ask it for
 * that geometry every time they calculate their position, so nothing about the size of the area or its content is remembered longer than a frame.
 */
public class ScrollAreaElement extends AbstractElement implements ElementContainer {

    /** Where children get told the mouse is while it is not over what the area shows of them, so nothing in them reacts to it. */
    public static final int MOUSE_OUTSIDE = -100000;
    /** The longest time one frame moves the content for. Longer frames, like the first one after the game hung, would make the content jump. */
    private static final double MAX_FRAME_SECONDS = 0.1D;
    /** The smallest a proportional content gets. Smaller than this, nothing in it can be told apart anymore, and a scale of zero would break all math. */
    private static final float MIN_PROPORTIONAL_SCALE = 0.05F;

    public final Property<ScrollDirection> scrollDirection = putProperty(ScrollDirection.buildProperty());
    /** The size of the content in the units of the content. Only counts on axes the area scrolls on, and never makes the content smaller than the area. */
    public final Property.IntegerProperty contentWidth = putProperty(Property.integerProperty("content_width", 0, "fancymenu.elements.scroll_area.content_width", Property.NumericInputBehavior.<Integer>builder().freeInput().build()));
    public final Property.IntegerProperty contentHeight = putProperty(Property.integerProperty("content_height", 0, "fancymenu.elements.scroll_area.content_height", Property.NumericInputBehavior.<Integer>builder().freeInput().build()));
    /** If the user can scroll the content. Without it, the content only moves on its own and when something tells it to. */
    public final Property.BooleanProperty userScrolling = putProperty(Property.booleanProperty("user_scrolling", true, "fancymenu.elements.scroll_area.scrolling.user"));
    public final Property.FloatProperty scrollSpeed = putProperty(Property.floatProperty("scroll_speed", 1.0F, "fancymenu.elements.scroll_area.scroll_speed", Property.NumericInputBehavior.<Float>builder().rangeInput(0.1F, 5.0F).build()));
    public final Property.BooleanProperty smoothScrolling = putProperty(Property.booleanProperty("smooth_scrolling", true, "fancymenu.elements.scroll_area.scrolling.smooth"));
    public final Property.BooleanProperty dragScrolling = putProperty(Property.booleanProperty("drag_scrolling", false, "fancymenu.elements.scroll_area.scrolling.drag"));
    public final Property.BooleanProperty keyboardScrolling = putProperty(Property.booleanProperty("keyboard_scrolling", true, "fancymenu.elements.scroll_area.scrolling.keyboard"));
    public final Property<ScrollSnapMode> snapMode = putProperty(ScrollSnapMode.buildProperty());
    public final Property<ScrollStartPosition> startPosition = putProperty(ScrollStartPosition.buildProperty());
    /** If the content goes back to its start position every time the screen gets opened. Without it, it stays where it was until the game closes. */
    public final Property.BooleanProperty resetScrollOnOpen = putProperty(Property.booleanProperty("reset_scroll_on_open", true, "fancymenu.elements.scroll_area.scrolling.reset_on_open"));
    public final Property.BooleanProperty autoScroll = putProperty(Property.booleanProperty("auto_scroll", false, "fancymenu.elements.scroll_area.auto_scroll.enabled"));
    /** How many screen pixels per second the content scrolls on its own. */
    public final Property.FloatProperty autoScrollSpeed = putProperty(Property.floatProperty("auto_scroll_speed", 20.0F, "fancymenu.elements.scroll_area.auto_scroll.speed", Property.NumericInputBehavior.<Float>builder().rangeInput(2.0F, 200.0F).build()));
    public final Property.BooleanProperty autoScrollLoop = putProperty(Property.booleanProperty("auto_scroll_loop", true, "fancymenu.elements.scroll_area.auto_scroll.loop"));
    public final Property.BooleanProperty autoScrollPauseOnHover = putProperty(Property.booleanProperty("auto_scroll_pause_on_hover", true, "fancymenu.elements.scroll_area.auto_scroll.pause_on_hover"));
    public final Property.BooleanProperty edgeShadows = putProperty(Property.booleanProperty("edge_shadows", false, "fancymenu.elements.scroll_area.edge_shadows"));
    public final Property.ColorProperty edgeShadowColor = putProperty(Property.hexColorProperty("edge_shadow_color", "#000000A0", true, "fancymenu.elements.scroll_area.edge_shadows.color"));
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
    public final Property<ScrollBarVisibility> scrollBarVisibility = putProperty(ScrollBarVisibility.buildProperty());
    /** How thick the scroll bars are. Their grabbers are twice as long as they are thick, like the pixel art handle they are made for. */
    public final Property.IntegerProperty scrollBarThickness = putProperty(Property.integerProperty("scroll_bar_thickness", PixelScrollGrabber.THICKNESS, "fancymenu.elements.scroll_area.scroll_bars.thickness", Property.NumericInputBehavior.<Integer>builder().rangeInput(4, 32).build()));
    public final Property.BooleanProperty verticalScrollBarLeft = putProperty(Property.booleanProperty("vertical_scroll_bar_left", false, "fancymenu.elements.scroll_area.scroll_bars.vertical_left"));
    public final Property.BooleanProperty horizontalScrollBarTop = putProperty(Property.booleanProperty("horizontal_scroll_bar_top", false, "fancymenu.elements.scroll_area.scroll_bars.horizontal_top"));
    public final ScrollBarGrabberStyle grabberStyle = new ScrollBarGrabberStyle(this);
    public final ScrollBarTrackBackground scrollBarBackground = new ScrollBarTrackBackground(this);
    public final ElementPanelBackground background = new ElementPanelBackground(this, "fancymenu.elements.scroll_area.background", this::getScrollBarSpace);

    protected final ScrollAreaScrolling scrolling = new ScrollAreaScrolling(this);
    protected final ScrollAreaGeometry geometry = new ScrollAreaGeometry();
    protected final ScrollAreaBars bars = new ScrollAreaBars(this);
    protected final ScrollAreaInput input = new ScrollAreaInput(this);
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
    protected long lastFrameNanos = 0L;
    /** The time the frame before this one took, which is how long everything that moves on its own moves for in this one. */
    protected double frameDeltaSeconds = 0.0D;
    /** The frame the area last rendered its children in. Input comes in between frames, so the area still shows them in the frame after it. */
    protected long lastChildrenShownFrame = -2L;
    protected float childOpacity = 1.0F;
    /** Where the mouse was when the area rendered the last time, for what moves the content along with it. */
    protected int frameMouseX = MOUSE_OUTSIDE;
    protected int frameMouseY = MOUSE_OUTSIDE;

    public ScrollAreaElement(@NotNull ElementBuilder<?, ?> builder) {
        super(builder);
        // The area cuts its children to a rectangle on the screen, which a rotated or tilted area would not be
        this.setSupportsRotation(false);
        this.setSupportsTilting(false);
        // Arranged children run in one direction, so an area that arranges them goes on to the next direction when it gets set to both
        this.scrollDirection.setValueSetProcessor(direction -> ((direction == ScrollDirection.BOTH) && !this.getLayoutMode().supportsBothDirections()) ? ScrollDirection.VERTICAL : direction);
    }

    @Override
    public void afterConstruction() {
        this.scrolling.restoreFromMemory();
    }

    @Override
    public void onOpenScreen() {
        if (this.resetScrollOnOpen.getBoolean()) this.scrolling.resetToStartPosition();
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {

        // Has to be known before the frame gets worked out: children only take part in an arrangement while the area shows them
        this.lastChildrenShownFrame = RenderFrameCounter.getFrame();
        // The opacity includes the one of the containers this area is in while it renders, which is the only time children ask for it
        this.childOpacity = this.opacity;
        this.frameMouseX = mouseX;
        this.frameMouseY = mouseY;

        this.updateFrame();

        ScrollAreaGeometry geometry = this.geometry;
        int viewportX = geometry.getViewportX();
        int viewportY = geometry.getViewportY();

        // The bars have to know where they are before the background gets drawn, which reaches behind the ones that show
        this.bars.update(geometry, this.opacity, this.frameDeltaSeconds);
        this.background.render(graphics, viewportX, viewportY, geometry.getViewportWidth(), geometry.getViewportHeight(), this.bars.isVerticalShown(), this.bars.isHorizontalShown(), this.isVerticalScrollBarAtStart(), this.isHorizontalScrollBarAtStart(), this.opacity);
        boolean mouseOverContent = geometry.isInViewport(mouseX, mouseY) && this.isScreenPointVisibleInContainer(mouseX, mouseY);
        // Nothing in the content reacts to a mouse that is not over it, or that drags the content along
        boolean childrenSeeMouse = mouseOverContent && !this.scrolling.isDragging();
        int childMouseX = childrenSeeMouse ? mouseX : MOUSE_OUTSIDE;
        int childMouseY = childrenSeeMouse ? mouseY : MOUSE_OUTSIDE;

        if (this.children.isEmpty() && (this.emptyContentRenderer != null)) {
            this.emptyContentRenderer.render(graphics, viewportX, viewportY, geometry.getViewportWidth(), geometry.getViewportHeight(), mouseOverContent);
        }

        graphics.enableScissor(viewportX, viewportY, viewportX + geometry.getViewportWidth(), viewportY + geometry.getViewportHeight());
        graphics.pose().pushMatrix();
        try {
            // Positions of children are whole screen pixels. The rest of the scroll moves what they draw, so the content moves as smooth as the window allows.
            graphics.pose().translate(-geometry.getSubPixelScrollX(), -geometry.getSubPixelScrollY());
            for (int i = 0; i < this.children.size(); i++) {
                this.renderChild(this.children.get(i), graphics, childMouseX, childMouseY, partial);
            }
        } finally {
            graphics.pose().popMatrix();
            graphics.disableScissor();
        }

        if (this.edgeShadows.getBoolean()) {
            ScrollAreaEdgeShadows.render(graphics, geometry, this.scrolling.getScroller(), this.edgeShadowColor.getDrawable().getColorInt(), this.opacity);
        }

        this.bars.render(graphics, mouseX, mouseY, partial);

    }

    /**
     * Renders a child. Children that draw scaled by the area get the scale on the pose, around their top-left corner,
     * and are in the unscaled space while they render: they report the size they have in the units of the content
     * and get the mouse where it is in that space.
     */
    protected void renderChild(@NotNull AbstractElement child, @NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        this.renderChild(child, graphics, mouseX, mouseY, partial, this.childRenderer);
    }

    /**
     * Renders a child with the given renderer, in the space the area gives its children. Does not cut anything of the child off.
     */
    public void renderChild(@NotNull AbstractElement child, @NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial, @NotNull ChildRenderer renderer) {
        ElementContainerLink link = child.getContainerLink();
        if ((link == null) || !ElementContainerLink.drawsScaled(child, this)) {
            renderer.render(child, graphics, mouseX, mouseY, partial);
            return;
        }
        float scale = this.geometry.getScale();
        int x = child.getAbsoluteX();
        int y = child.getAbsoluteY();
        int scaledMouseX = (mouseX == MOUSE_OUTSIDE) ? MOUSE_OUTSIDE : (int) Math.floor(x + ((mouseX - x) / scale));
        int scaledMouseY = (mouseY == MOUSE_OUTSIDE) ? MOUSE_OUTSIDE : (int) Math.floor(y + ((mouseY - y) / scale));
        graphics.pose().pushMatrix();
        ElementContainerLink.SizeSpace previousSpace = link.setSizeSpace(ElementContainerLink.SizeSpace.CONTENT);
        try {
            graphics.pose().translate(x, y);
            graphics.pose().scale(scale, scale);
            graphics.pose().translate(-x, -y);
            renderer.render(child, graphics, scaledMouseX, scaledMouseY, partial);
        } finally {
            link.setSizeSpace(previousSpace);
            graphics.pose().popMatrix();
        }
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

            long now = this.getNanoTime();
            double deltaSeconds = (this.lastFrameNanos == 0L) ? 0.0D : Math.min(MAX_FRAME_SECONDS, (now - this.lastFrameNanos) / 1.0E9D);
            this.lastFrameNanos = now;
            this.frameDeltaSeconds = deltaSeconds;

            boolean mouseOver = this.geometry.isInViewport(this.frameMouseX, this.frameMouseY);
            this.scrolling.update(this.geometry, deltaSeconds, this.frameMouseX, this.frameMouseY, mouseOver);
            ScrollAreaScroller scroller = this.scrolling.getScroller();
            this.geometry.setScroll(scroller.x().getPosition(), scroller.y().getPosition(), this.getGuiScale());

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
     * Scrolls the content with the mouse wheel.
     *
     * @return If the content has somewhere to go in the scrolled direction. If not, the scroll is for whatever is behind the area.
     */
    public boolean scrollByWheel(double deltaX, double deltaY) {
        return this.scrolling.scrollByWheel(deltaX, deltaY);
    }

    /**
     * Returns the direction the area scrolls in. An area that arranges its children does it in one direction and only scrolls in that one,
     * so it scrolls up and down when it is set to both.
     */
    @NotNull
    public ScrollDirection getScrollDirection() {
        ScrollDirection direction = this.scrollDirection.tryGetNonNullElse(ScrollDirection.VERTICAL);
        return ((direction == ScrollDirection.BOTH) && !this.getLayoutMode().supportsBothDirections()) ? ScrollDirection.VERTICAL : direction;
    }

    public boolean isVerticalScrollBarAtStart() {
        return this.verticalScrollBarLeft.getBoolean();
    }

    public boolean isHorizontalScrollBarAtStart() {
        return this.horizontalScrollBarTop.getBoolean();
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

    @NotNull
    public ScrollAreaAlignment getChildAlignment() {
        return this.childAlignment.tryGetNonNullElse(ScrollAreaAlignment.START);
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
            this.contentWidth.clearManualInput();
            this.contentWidth.set(right);
        }
        if (direction.scrollsY() && !isAdvancedSize(this.contentHeight)) {
            this.contentHeight.clearManualInput();
            this.contentHeight.set(bottom);
        }
    }

    /**
     * Returns if the position of the child on the given axis stays the same when the content changes its size on that axis.
     */
    protected static boolean isPlacedFromContentStart(@NotNull AbstractElement child, boolean horizontal) {
        if (horizontal ? child.stretchX.getBoolean() : child.stretchY.getBoolean()) return false;
        if (horizontal ? !child.advancedX.isDefault() : !child.advancedY.isDefault()) return true;
        ElementAnchorPoint anchor = child.anchorPoint;
        if ((anchor == null) || (anchor == ElementAnchorPoints.ELEMENT)) return true;
        if (horizontal) return (anchor == ElementAnchorPoints.TOP_LEFT) || (anchor == ElementAnchorPoints.MID_LEFT) || (anchor == ElementAnchorPoints.BOTTOM_LEFT);
        return (anchor == ElementAnchorPoints.TOP_LEFT) || (anchor == ElementAnchorPoints.TOP_CENTERED) || (anchor == ElementAnchorPoints.TOP_RIGHT);
    }

    @NotNull
    public ScrollAreaInput getInput() {
        return this.input;
    }

    /**
     * Sets what renders into the area while it has no children.
     */
    public void setEmptyContentRenderer(@Nullable EmptyContentRenderer emptyContentRenderer) {
        this.emptyContentRenderer = emptyContentRenderer;
    }

    /**
     * Returns how many pixels of the window one screen pixel is, which is the smallest step the content can move by and still show sharp.
     */
    protected double getGuiScale() {
        return WindowHandler.getGuiScale();
    }

    protected boolean isShiftDown() {
        return Minecraft.getInstance().hasShiftDown();
    }

    /**
     * Returns the time everything in the area that moves on its own measures its frames with.
     */
    protected long getNanoTime() {
        return System.nanoTime();
    }

    protected boolean isInEditor() {
        return isEditor();
    }

    /**
     * Returns what has the focus of the screen right now, which the keyboard moves from widget to widget.
     */
    @Nullable
    protected GuiEventListener getFocusedListener() {
        Screen screen = ScreenUtils.getScreen();
        return (screen != null) ? screen.getFocused() : null;
    }

    /**
     * Returns the child the given widget belongs to: one of its own widgets, or a widget of something in a child that is an area itself.
     * Returns NULL if the widget belongs to nothing in the area.
     */
    @Nullable
    public AbstractElement findChildOwning(@NotNull GuiEventListener widget) {
        for (int i = 0; i < this.children.size(); i++) {
            AbstractElement child = this.children.get(i);
            if ((child instanceof ScrollAreaElement inner) && (inner.findChildOwning(widget) != null)) return child;
            List<GuiEventListener> widgets = child.getWidgetsToRegister();
            if (widgets == null) continue;
            for (GuiEventListener childWidget : widgets) {
                // Widgets that take their input in the space of their element get registered as the listener that maps it
                if ((childWidget == widget) || ((childWidget instanceof ElementSpaceInputListener listener) && (listener.getWidget() == widget))) return child;
            }
        }
        return null;
    }

    /**
     * Returns if something in the area that takes mouse input is at the given position on the screen: a widget of a child.
     */
    public boolean hasInteractiveChildAt(double screenX, double screenY) {
        Screen screen = ScreenUtils.getScreen();
        if (screen == null) return false;
        for (GuiEventListener listener : screen.children()) {
            if ((listener instanceof ElementSpaceInputListener childListener) && ElementContainerHierarchy.isInContainer(childListener.getElement(), this) && childListener.isMouseOver(screenX, screenY)) return true;
        }
        return false;
    }

    @NotNull
    public ScrollSnapMode getSnapMode() {
        return this.snapMode.tryGetNonNullElse(ScrollSnapMode.NONE);
    }

    @NotNull
    public ScrollAreaScrolling getScrolling() {
        return this.scrolling;
    }

    @NotNull
    public ScrollAreaScroller getScroller() {
        return this.scrolling.getScroller();
    }

    @NotNull
    public ScrollAreaGeometry getGeometry() {
        this.ensureFrame();
        return this.geometry;
    }

    @NotNull
    public ScrollAreaBars getBars() {
        return this.bars;
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
    public boolean arrangesChildren() {
        return this.getLayoutMode().arrangesChildren();
    }

    @Override
    public boolean isScreenPointVisible(double screenX, double screenY) {
        this.ensureFrame();
        return this.geometry.isInViewport(screenX, screenY) && this.isScreenPointVisibleInContainer(screenX, screenY);
    }

    @Override
    public boolean isShowingChildren() {
        return ((RenderFrameCounter.getFrame() - this.lastChildrenShownFrame) <= 1L) && this.shouldRender();
    }

    @Override
    public float getChildOpacity() {
        return this.childOpacity;
    }

    /**
     * Renders something into a scrollable area that has no children.
     */
    @FunctionalInterface
    public interface EmptyContentRenderer {

        /**
         * @param mouseOver If the mouse is over the area.
         */
        void render(@NotNull GuiGraphicsExtractor graphics, int x, int y, int width, int height, boolean mouseOver);

    }

    /**
     * Renders a child of a scrollable area.
     */
    @FunctionalInterface
    public interface ChildRenderer {

        void render(@NotNull AbstractElement child, @NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial);

    }

}
