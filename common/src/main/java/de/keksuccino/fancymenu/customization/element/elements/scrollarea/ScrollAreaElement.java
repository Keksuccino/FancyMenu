package de.keksuccino.fancymenu.customization.element.elements.scrollarea;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoint;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.element.container.ElementContainer;
import de.keksuccino.fancymenu.customization.element.container.ElementContainerLink;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.properties.RuntimePropertyContainer;
import de.keksuccino.fancymenu.util.rendering.RenderFrameCounter;
import de.keksuccino.fancymenu.util.window.WindowHandler;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
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
    /** How many screen pixels one step of the mouse wheel scrolls at a scroll speed of 1. */
    private static final double WHEEL_STEP = 30.0D;
    /** The longest time one frame moves the content for. Longer frames, like the first one after the game hung, would make the content jump. */
    private static final double MAX_FRAME_SECONDS = 0.1D;
    private static final String MEMORY_SCROLL_X = "scroll_area_scroll_x";
    private static final String MEMORY_SCROLL_Y = "scroll_area_scroll_y";

    public final Property<ScrollDirection> scrollDirection = putProperty(ScrollDirection.buildProperty());
    /** The size of the content in the units of the content. Only counts on axes the area scrolls on, and never makes the content smaller than the area. */
    public final Property.IntegerProperty contentWidth = putProperty(Property.integerProperty("content_width", 0, "fancymenu.elements.scroll_area.content_width", Property.NumericInputBehavior.<Integer>builder().freeInput().build()));
    public final Property.IntegerProperty contentHeight = putProperty(Property.integerProperty("content_height", 0, "fancymenu.elements.scroll_area.content_height", Property.NumericInputBehavior.<Integer>builder().freeInput().build()));
    public final Property.FloatProperty scrollSpeed = putProperty(Property.floatProperty("scroll_speed", 1.0F, "fancymenu.elements.scroll_area.scroll_speed", Property.NumericInputBehavior.<Float>builder().rangeInput(0.1F, 5.0F).build()));

    protected final ScrollAreaScroller scroller = new ScrollAreaScroller();
    protected final ScrollAreaGeometry geometry = new ScrollAreaGeometry();
    protected final ScrollAreaBars bars = new ScrollAreaBars(this.scroller);
    protected final ScrollAreaInput input = new ScrollAreaInput(this);
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
    /** The frame the area last rendered its children in. Input comes in between frames, so the area still shows them in the frame after it. */
    protected long lastChildrenShownFrame = -2L;
    protected float childOpacity = 1.0F;
    protected double savedScrollX = 0.0D;
    protected double savedScrollY = 0.0D;

    public ScrollAreaElement(@NotNull ElementBuilder<?, ?> builder) {
        super(builder);
        // The area cuts its children to a rectangle on the screen, which a rotated or tilted area would not be
        this.setSupportsRotation(false);
        this.setSupportsTilting(false);
    }

    @Override
    public void afterConstruction() {
        // Elements get rebuilt whenever the screen changes its size, and the content stays where it was scrolled to
        RuntimePropertyContainer memory = this.getMemory();
        Double scrollX = memory.getProperty(MEMORY_SCROLL_X, Double.class);
        Double scrollY = memory.getProperty(MEMORY_SCROLL_Y, Double.class);
        if (scrollX != null) this.scroller.x().restore(scrollX);
        if (scrollY != null) this.scroller.y().restore(scrollY);
        this.savedScrollX = this.scroller.x().getTarget();
        this.savedScrollY = this.scroller.y().getTarget();
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {

        this.updateFrame();

        // The opacity includes the one of the containers this area is in while it renders, which is the only time children ask for it
        this.childOpacity = this.opacity;
        this.lastChildrenShownFrame = RenderFrameCounter.getFrame();

        ScrollAreaGeometry geometry = this.geometry;
        int viewportX = geometry.getViewportX();
        int viewportY = geometry.getViewportY();
        boolean mouseOverContent = geometry.isInViewport(mouseX, mouseY) && this.isScreenPointVisibleInContainer(mouseX, mouseY);
        int childMouseX = mouseOverContent ? mouseX : MOUSE_OUTSIDE;
        int childMouseY = mouseOverContent ? mouseY : MOUSE_OUTSIDE;

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

        this.bars.update(geometry, this.scroller, this.opacity);
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
            float scale = (parent != null) ? parent.getContentScale() : 1.0F;
            this.geometry.setViewport(this.getAbsoluteX(), this.getAbsoluteY(), this.getAbsoluteWidth(), this.getAbsoluteHeight(), scale);

            ScrollDirection direction = this.getScrollDirection();
            this.geometry.setContentSize(this.contentWidth.getInteger(), this.contentHeight.getInteger(), direction.scrollsX(), direction.scrollsY());

            long now = System.nanoTime();
            double deltaSeconds = (this.lastFrameNanos == 0L) ? 0.0D : Math.min(MAX_FRAME_SECONDS, (now - this.lastFrameNanos) / 1.0E9D);
            this.lastFrameNanos = now;

            this.scroller.setRange(this.geometry.getMaxScrollX(), this.geometry.getMaxScrollY());
            this.scroller.update(deltaSeconds);
            this.geometry.setScroll(this.scroller.x().getPosition(), this.scroller.y().getPosition(), WindowHandler.getGuiScale());

            this.rememberScroll();
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

    protected void rememberScroll() {
        double scrollX = this.scroller.x().getTarget();
        double scrollY = this.scroller.y().getTarget();
        if ((scrollX == this.savedScrollX) && (scrollY == this.savedScrollY)) return;
        this.savedScrollX = scrollX;
        this.savedScrollY = scrollY;
        RuntimePropertyContainer memory = this.getMemory();
        memory.putProperty(MEMORY_SCROLL_X, scrollX);
        memory.putProperty(MEMORY_SCROLL_Y, scrollY);
    }

    /**
     * Scrolls the content with the mouse wheel.<br>
     * An area that only scrolls sideways takes the normal wheel for it. An area that scrolls both ways scrolls up and down
     * with the wheel and sideways with a sideways wheel or with Shift held.
     *
     * @return If the content has somewhere to go in the scrolled direction. If not, the scroll is for whatever is behind the area.
     */
    public boolean scrollByWheel(double deltaX, double deltaY) {
        double step = WHEEL_STEP * Math.max(0.0F, this.scrollSpeed.getFloat());
        ScrollDirection direction = this.getScrollDirection();
        if (direction == ScrollDirection.VERTICAL) {
            return (deltaY != 0.0D) && this.scroller.y().scrollBy(-deltaY * step);
        }
        if (direction == ScrollDirection.HORIZONTAL) {
            double delta = (deltaX != 0.0D) ? deltaX : deltaY;
            return (delta != 0.0D) && this.scroller.x().scrollBy(-delta * step);
        }
        if (Minecraft.getInstance().hasShiftDown() && (deltaX == 0.0D)) {
            return (deltaY != 0.0D) && this.scroller.x().scrollBy(-deltaY * step);
        }
        boolean scrolledX = (deltaX != 0.0D) && this.scroller.x().scrollBy(-deltaX * step);
        boolean scrolledY = (deltaY != 0.0D) && this.scroller.y().scrollBy(-deltaY * step);
        return scrolledX || scrolledY;
    }

    @NotNull
    public ScrollDirection getScrollDirection() {
        return this.scrollDirection.tryGetNonNullElse(ScrollDirection.VERTICAL);
    }

    /**
     * Returns if the content is as big as the user sets it. If not, the area works its size out from its children.
     */
    public boolean hasUserContentSize() {
        return true;
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

    @NotNull
    public ScrollAreaScroller getScroller() {
        return this.scroller;
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
        ElementContainer parent = this.getParentContainer();
        return (parent != null) && parent.scalesChildVisuals();
    }

    @Override
    public boolean managesChildBoxes() {
        return false;
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
