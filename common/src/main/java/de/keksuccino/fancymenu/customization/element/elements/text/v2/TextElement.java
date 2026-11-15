package de.keksuccino.fancymenu.customization.element.elements.text.v2;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.ElementSpaceInputListener;
import de.keksuccino.fancymenu.customization.element.elements.MarkdownSettings;
import de.keksuccino.fancymenu.customization.element.scrolling.ElementScrollBars;
import de.keksuccino.fancymenu.customization.element.scrolling.ElementScrollInput;
import de.keksuccino.fancymenu.customization.element.scrolling.ElementScrolling;
import de.keksuccino.fancymenu.customization.element.scrolling.ScrollDirection;
import de.keksuccino.fancymenu.customization.element.scrolling.ScrollGeometry;
import de.keksuccino.fancymenu.customization.element.scrolling.ScrollSettings;
import de.keksuccino.fancymenu.customization.element.scrolling.ScrollSnapMode;
import de.keksuccino.fancymenu.customization.element.scrolling.ScrollSnapPoints;
import de.keksuccino.fancymenu.customization.element.scrolling.ScrollableElement;
import de.keksuccino.fancymenu.customization.element.style.ElementPanelBackground;
import de.keksuccino.fancymenu.customization.element.style.ScrollBarGrabberStyle;
import de.keksuccino.fancymenu.customization.element.style.ScrollBarTrackBackground;
import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import de.keksuccino.fancymenu.util.enums.LocalizedCycleEnum;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderFrameCounter;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownRenderer;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownTextFragment;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollbar.ScrollBar;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.text.IText;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import de.keksuccino.fancymenu.util.threading.FancyMenuThreads;
import de.keksuccino.konkrete.input.StringUtils;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/**
 * A text that shows in an area of its own and scrolls in it when it does not fit.<br>
 * The text is the content the element scrolls: it wraps at the width of the element, and its real size is the size of the content.
 * The element works out where the text is once per frame, in {@link #updateFrame()}, from the size it has in that frame.
 */
public class TextElement extends AbstractElement implements ScrollableElement {

    /** How far the text is from the edges of the content, on every side. */
    public static final int TEXT_INSET = 1;
    /** Where the mouse is for the text while it must not react to it: far away from everything. */
    protected static final int MOUSE_OUTSIDE = -100000;
    /** The frame an element that does not show its text last showed it in: none. */
    private static final long NEVER_SHOWN = -2L;
    /** Where the text can come to rest. It holds no elements, so it can't snap to any. */
    private static final Set<ScrollSnapMode> SNAP_MODES = Collections.unmodifiableSet(EnumSet.of(ScrollSnapMode.NONE, ScrollSnapMode.LINES, ScrollSnapMode.HEADLINES, ScrollSnapMode.PAGES));
    /** The grabber colors of the UI theme, which grabbers without a color of the user get drawn in. */
    private static final Supplier<DrawableColor> THEME_GRABBER_COLOR_NORMAL = () -> UIBase.getUITheme().scroll_grabber_color_normal;
    private static final Supplier<DrawableColor> THEME_GRABBER_COLOR_HOVER = () -> UIBase.getUITheme().scroll_grabber_color_hover;

    @NotNull
    protected SourceMode sourceMode = SourceMode.DIRECT;
    @Nullable
    protected String source; //direct text, file path, link
    protected volatile String text;
    protected String lastText;

    @Nullable
    public ResourceSupplier<IText> textResourceSupplier;
    public ResourceSupplier<ITexture> verticalScrollGrabberTextureNormal;
    public ResourceSupplier<ITexture> verticalScrollGrabberTextureHover;
    public ResourceSupplier<ITexture> horizontalScrollGrabberTextureNormal;
    public ResourceSupplier<ITexture> horizontalScrollGrabberTextureHover;
    public boolean interactable = true;
    @NotNull
    public volatile MarkdownRenderer markdownRenderer = new MarkdownRenderer();

    public final Property.ColorProperty scrollGrabberColorHexNormal = putProperty(Property.hexColorProperty("grabber_color_normal", null, true, "fancymenu.elements.text.scroll_grabber_color.normal"));
    public final Property.ColorProperty scrollGrabberColorHexHover = putProperty(Property.hexColorProperty("grabber_color_hover", null, true, "fancymenu.elements.text.scroll_grabber_color.hover"));
    public final MarkdownSettings markdownSettings = new MarkdownSettings(this, this.markdownRenderer);
    public final Property<ScrollGrabberDesign> scrollGrabberDesign = putProperty(ScrollGrabberDesign.buildProperty());
    /** A text scrolls both ways unless it is set to something else: up and down, and sideways where a line is wider than the element. */
    public final ScrollSettings scrollSettings = new ScrollSettings(this, "fancymenu.elements.text", ScrollDirection.BOTH, SNAP_MODES);
    public final ElementPanelBackground background = new ElementPanelBackground(this, "fancymenu.elements.text.background", this::getScrollBarSpace);
    public final ScrollBarTrackBackground scrollBarBackground = new ScrollBarTrackBackground(this);

    protected final ElementScrolling scrolling = new ElementScrolling(this);
    protected final ScrollGeometry geometry = new ScrollGeometry();
    protected final ElementScrollBars bars = new ElementScrollBars(this, this.scrolling.getScroller());
    protected final ElementScrollInput scrollInput = new ElementScrollInput(this);
    /** The text renders in the untransformed space of the element, so screens send it its input through this. */
    protected final ElementSpaceInputListener markdownInput;
    /**
     * What the element registers to the screen. The scrolling comes first: the text takes every press on one of its words, also where a press
     * does nothing in it, so it would keep them from dragging the text. The scrolling leaves presses on what can be clicked, like a link, to the text.
     */
    protected final List<GuiEventListener> widgets;
    protected List<String> lastLines;
    protected IText lastIText;
    protected boolean lastTickShouldRender = false;
    /** The grabber colors of the user, read once per frame. The suppliers the scroll bars ask for them are the same objects for every frame. */
    @Nullable
    protected DrawableColor customGrabberColorNormal;
    @Nullable
    protected DrawableColor customGrabberColorHover;
    protected final Supplier<DrawableColor> customGrabberColorNormalSupplier = () -> this.customGrabberColorNormal;
    protected final Supplier<DrawableColor> customGrabberColorHoverSupplier = () -> this.customGrabberColorHover;
    protected boolean frameReady = false;
    /**
     * The frame the element last rendered its text in. It shows its text as long as this is not more than one frame ago:
     * input comes in between frames.
     */
    protected long lastShownFrame = NEVER_SHOWN;
    /** Where the mouse was when the element rendered the last time, in the space the text scrolls in. */
    protected int frameMouseX = MOUSE_OUTSIDE;
    protected int frameMouseY = MOUSE_OUTSIDE;

    public TextElement(@NotNull ElementBuilder<?, ?> builder) {

        super(builder);

        this.allowDepthTestManipulation = true;

        // The grabbers of the bar design are bars in a color, which have round corners like everything else of the UI theme
        this.bars.setRoundedGrabbers(true);
        ScrollGrabberDesign.followAppliedLooks(this.scrollGrabberDesign, this.markdownSettings);

        this.markdownInput = ElementSpaceInputListener.of(this, this.markdownRenderer);
        this.widgets = List.of(this.scrollInput, this.markdownInput);

        this.markdownRenderer.setTextEventHandler(new MarkdownRenderer.TextEventHandler() {
            @Override
            public void onTextClickEvent(@NotNull String eventId) {
                if (TextElement.this.isInEditor() || !TextElement.this.interactable) {
                    return;
                }
                if (eventId.isEmpty()) {
                    return;
                }
                if (Listeners.ON_TEXT_CLICKED.hasInstancesListening()) {
                    Listeners.ON_TEXT_CLICKED.onTextClicked(eventId);
                }
            }

            @Override
            public void onTextHoverEvent(@NotNull String eventId) {
                if (TextElement.this.isInEditor() || !TextElement.this.interactable) {
                    return;
                }
                if (eventId.isEmpty()) {
                    return;
                }
                if (Listeners.ON_TEXT_HOVERED.hasInstancesListening()) {
                    Listeners.ON_TEXT_HOVERED.onTextHovered(eventId);
                }
            }
        });

    }

    @Override
    public void afterConstruction() {
        this.scrolling.restoreFromMemory();
    }

    @Override
    public void onOpenScreen() {
        if (this.scrollSettings.resetScrollOnOpen.getBoolean()) this.scrolling.resetToStartPosition();
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {

        if (this.shouldRender()) {

            try {

                this.renderTick();

                this.markdownRenderer.setTextOpacity(this.opacity);

                this.lastShownFrame = RenderFrameCounter.getFrame();
                // The text renders in the untransformed space of the element, so everything that checks the mouse needs it in that space
                int elementMouseX = (int)Math.floor(this.untransformMouseX(mouseX, mouseY));
                int elementMouseY = (int)Math.floor(this.untransformMouseY(mouseX, mouseY));
                this.frameMouseX = elementMouseX;
                this.frameMouseY = elementMouseY;

                // A grabber the user holds follows the mouse where it really is, before the frame gets worked out with where that takes the text
                this.bars.followPointer();

                this.updateFrame();

                ScrollGeometry geometry = this.geometry;
                int x = geometry.getViewportX();
                int y = geometry.getViewportY();
                int width = geometry.getViewportWidth();
                int height = geometry.getViewportHeight();

                // The bars have to know where they are before the background gets drawn, which reaches behind the ones that show
                this.bars.update(geometry, this.opacity);
                this.background.render(graphics, x, y, width, height, this.bars.isVerticalShown(), this.bars.isHorizontalShown(), this.scrollSettings.verticalScrollBarLeft.getBoolean(), this.scrollSettings.horizontalScrollBarTop.getBoolean(), this.opacity);

                // Nothing in the text reacts to a mouse that is not over what shows of it, or that drags the text along
                boolean textSeesMouse = geometry.isInViewport(elementMouseX, elementMouseY) && this.isRenderSpacePointVisibleInContainer(mouseX, mouseY) && !this.scrolling.isDragging();

                MarkdownRenderer markdown = this.markdownRenderer;
                markdown.setX(geometry.getContentScreenX() + TEXT_INSET);
                markdown.setY(geometry.getContentScreenY() + TEXT_INSET);
                // Lines outside of what the element shows don't get rendered
                markdown.setVisibleVerticalRange(y, y + height);
                graphics.enableScissor(x, y, x + width, y + height);
                try {
                    markdown.extractRenderState(graphics, textSeesMouse ? elementMouseX : MOUSE_OUTSIDE, textSeesMouse ? elementMouseY : MOUSE_OUTSIDE, partial);
                } finally {
                    graphics.disableScissor();
                }

                this.bars.render(graphics, elementMouseX, elementMouseY, partial);

            } catch (Exception e) {
                e.printStackTrace();
            }

            RenderingUtils.resetShaderColor(graphics);

        }

    }

    /**
     * Works out the geometry of the element for this frame and moves the text towards where it is scrolling to.
     */
    protected void updateFrame() {
        ScrollDirection direction = this.getScrollDirection();
        MarkdownRenderer markdown = this.markdownRenderer;
        int width = this.getAbsoluteWidth();
        // The text wraps at the width of the element. Asking for its real size lays it out again if that width changed.
        markdown.setOptimalWidth(Math.max(0, width - (TEXT_INSET * 2)));
        this.geometry.setViewport(this.getAbsoluteX(), this.getAbsoluteY(), width, this.getAbsoluteHeight(), 1.0F);
        this.geometry.setContentSize((int) Math.ceil(markdown.getRealWidth()) + (TEXT_INSET * 2), this.getContentHeight(), direction.scrollsX(), direction.scrollsY());
        this.scrolling.update(this.geometry, this.geometry.isInViewport(this.frameMouseX, this.frameMouseY));
        this.frameReady = true;
    }

    @Override
    public void renderInternal(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {

        super.renderInternal(graphics, mouseX, mouseY, partial);

        if (!this.shouldRender() && this.lastTickShouldRender) {
            this.markdownRenderer.resetHovered();
        }
        this.lastTickShouldRender = this.shouldRender();

    }

    @Override
    public @Nullable List<GuiEventListener> getWidgetsToRegister() {
        return this.interactable ? this.widgets : null;
    }

    /**
     * The text and the scrolling map the mouse into the space of the element themselves, which includes the scale of a container.
     */
    @Override
    public boolean registersChildWidgetsDirectly() {
        return true;
    }

    /**
     * A press is for the element where it grabs one of its scroll bars, where it starts dragging the text,
     * and where it is for something that can be clicked in the text, like a link.
     */
    @Override
    public boolean takesMousePressAt(double screenX, double screenY) {
        if (!this.interactable || !this.isShowingScrollContent()) return false;
        if (this.bars.isUsedByMouse()) return true;
        if (!this.isScrollPointVisible(screenX, screenY)) return false;
        return this.scrolling.canDragContent() || this.takesContentPressAt(screenX, screenY);
    }

    @Override
    public boolean takesContentPressAt(double screenX, double screenY) {
        // The text renders in the untransformed space of the element, which is where its fragments are
        double textX = this.untransformMouseX(screenX, screenY);
        double textY = this.untransformMouseY(screenX, screenY);
        return (this.markdownRenderer.getChildAt(textX, textY).orElse(null) instanceof MarkdownTextFragment fragment) && fragment.takesClickAt(textX, textY);
    }

    /** Returns the listener that takes the mouse wheel, the scroll bars and the dragging of the text for this element. */
    @NotNull
    public ElementScrollInput getScrollInput() {
        return this.scrollInput;
    }

    /**
     * Returns the space a scroll bar of the element has its grabber in, next to the text.
     */
    public int getScrollBarSpace() {
        return this.bars.getBarSpace();
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
        // Makes sure there is a geometry to answer with for everything that asks before the element rendered for the first time
        if (!this.frameReady) this.updateFrame();
        return this.geometry;
    }

    @Override
    public @NotNull ScrollDirection getScrollDirection() {
        return this.scrollSettings.scrollDirection.tryGetNonNullElse(ScrollDirection.BOTH);
    }

    @Override
    public boolean isShowingScrollContent() {
        return (RenderFrameCounter.getFrame() - this.lastShownFrame) <= 1L;
    }

    @Override
    public boolean isInEditor() {
        return isEditor();
    }

    /**
     * An element that takes no input takes none from the keyboard either.
     */
    @Override
    public boolean takesScrollKeys() {
        return this.interactable;
    }

    @Override
    public int getScrollBarDistance() {
        return this.background.getScrollBarDistance();
    }

    @Override
    public @NotNull ScrollBarTrackBackground getScrollBarBackground() {
        return this.scrollBarBackground;
    }

    /**
     * Textures and colors of the user win. Without any of them, the design of the element decides what the grabbers look like.
     */
    @Override
    public void applyScrollGrabberLooks(@NotNull ScrollBar vertical, @NotNull ScrollBar horizontal, float opacity) {

        this.customGrabberColorNormal = (this.scrollGrabberColorHexNormal.get() != null) ? this.scrollGrabberColorHexNormal.getDrawable() : null;
        this.customGrabberColorHover = (this.scrollGrabberColorHexHover.get() != null) ? this.scrollGrabberColorHexHover.getDrawable() : null;
        Supplier<DrawableColor> normalColor = (this.customGrabberColorNormal != null) ? this.customGrabberColorNormalSupplier : THEME_GRABBER_COLOR_NORMAL;
        Supplier<DrawableColor> hoverColor = (this.customGrabberColorHover != null) ? this.customGrabberColorHoverSupplier : THEME_GRABBER_COLOR_HOVER;

        boolean customized = (this.customGrabberColorNormal != null) || (this.customGrabberColorHover != null)
                || (this.verticalScrollGrabberTextureNormal != null) || (this.verticalScrollGrabberTextureHover != null)
                || (this.horizontalScrollGrabberTextureNormal != null) || (this.horizontalScrollGrabberTextureHover != null);
        ScrollGrabberDesign design = this.scrollGrabberDesign.tryGetNonNullElse(ScrollGrabberDesign.THEME_BAR).unlessCustomized(customized);
        ScrollBarGrabberStyle.apply(vertical, this.verticalScrollGrabberTextureNormal, this.verticalScrollGrabberTextureHover, normalColor, hoverColor, design.getTexture(true, false), design.getTexture(true, true), opacity);
        ScrollBarGrabberStyle.apply(horizontal, this.horizontalScrollGrabberTextureNormal, this.horizontalScrollGrabberTextureHover, normalColor, hoverColor, design.getTexture(false, false), design.getTexture(false, true), opacity);

    }

    /**
     * The text comes to rest with one of its lines or headlines at the start of what the element shows. Sideways, it has nothing to rest at.
     */
    @Override
    public void addSnapPoints(@NotNull ScrollSnapPoints points, boolean horizontal) {
        if (horizontal) return;
        this.markdownRenderer.getLinePositions().forEach(this.scrollSettings.getSnapMode() == ScrollSnapMode.HEADLINES, points::add);
    }

    @Override
    public boolean snapsOnAxis(boolean horizontal) {
        return !horizontal;
    }

    /**
     * The lines only are where they show once the text got laid out at the width of the element, so an element that does not show its text yet
     * looks for the line once it does.
     */
    @Override
    public void scrollToLine(int lineNumber, boolean animate) {
        this.scrolling.whenShowingContent(() -> this.scrolling.scrollToPosition(false, this.markdownRenderer.getLinePositions().find(lineNumber), animate));
    }

    @Override
    public void scrollToText(@NotNull String text, boolean animate) {
        this.scrolling.whenShowingContent(() -> this.scrolling.scrollToPosition(false, this.markdownRenderer.getLinePositions().find(text), animate));
    }

    protected void renderTick() {

        this.markdownSettings.tick();

        //If IText instance or its content changes, update element
        if (this.sourceMode == SourceMode.RESOURCE) {
            IText iText = (this.textResourceSupplier != null) ? this.textResourceSupplier.get() : null;
            List<String> lines = (iText != null) ? iText.getTextLines() : null;
            if (lines != null) lines = new ArrayList<>(lines);
            if (!Objects.equals(this.lastIText, iText) || !Objects.equals(this.lastLines, lines)) {
                this.updateContent();
            }
            this.lastLines = lines;
            this.lastIText = iText;
        }

        //Update markdown renderer text if changed
        String t = this.text;
        if ((t != null) && ((this.lastText == null) || !this.lastText.equals(t))) {
            this.markdownRenderer.setText(t);
        }
        this.lastText = t;

    }

    /**
     * Returns the height in pixels this element needs to show all of its text without scrolling.<br>
     * The markdown renderer lays out the text while the element renders, so this reflects the most recently rendered text, line wrapping and text settings.
     */
    public int getContentHeight() {
        return (int) Math.ceil(this.markdownRenderer.getRealHeight() + (TEXT_INSET * 2));
    }

    public void updateContent() {

        if (this.source == null) {
            this.markdownRenderer.setText(I18n.get("fancymenu.elements.text.placeholder"));
            return;
        }

        FancyMenuThreads.startDaemonThread(() -> {

            List<String> linesRaw = new ArrayList<>();

            try {
                if ((this.source != null) && !this.source.isEmpty()) {
                    if (this.sourceMode == SourceMode.DIRECT) {
                        String s = this.source.replace("%n%", "\n").replace("\r", "\n");
                        if (s.contains("\n")) {
                            linesRaw.addAll(Arrays.asList(StringUtils.splitLines(s, "\n")));
                        } else {
                            linesRaw.add(s);
                        }
                    } else if (this.textResourceSupplier != null) {
                        IText iText = this.textResourceSupplier.get();
                        if (iText != null) linesRaw = iText.getTextLines();
                        linesRaw = (linesRaw != null) ? new ArrayList<>(linesRaw) : new ArrayList<>();
                    }
                } else {
                    linesRaw.add(I18n.get("fancymenu.elements.text.placeholder"));
                }
            } catch (Exception ex) {
                if (linesRaw == null) linesRaw = new ArrayList<>();
                linesRaw.clear();
            }

            if (linesRaw.isEmpty()) {
                if (isEditor()) {
                    linesRaw.add(I18n.get("fancymenu.elements.text.status.unable_to_load"));
                } else {
                    linesRaw.add("");
                }
            }

            StringBuilder text = new StringBuilder();
            for (String s : linesRaw) {
                if (!text.isEmpty()) {
                    text.append("\n");
                }
                text.append(s);
            }
            this.text = text.toString();

        }, "TextElement-ContentLoader");

    }

    public void setSource(@NotNull SourceMode sourceMode, @Nullable String source) {
        this.sourceMode = Objects.requireNonNull(sourceMode);
        this.source = source;
        this.textResourceSupplier = null;
        if ((sourceMode == SourceMode.RESOURCE) && (this.source != null)) {
            this.textResourceSupplier = ResourceSupplier.text(this.source);
        }
        this.text = null;
        this.lastText = null;
        this.lastIText = null;
        this.lastLines = null;
        this.updateContent();
    }

    public enum SourceMode implements LocalizedCycleEnum<SourceMode> {

        DIRECT("direct"),
        RESOURCE("resource");

        final String name;

        SourceMode(@NotNull String name) {
            this.name = name;
        }

        @NotNull
        public String getName() {
            return this.name;
        }

        @Override
        public @NotNull SourceMode[] getValues() {
            return SourceMode.values();
        }

        @Override
        public @Nullable SourceMode getByNameInternal(@NotNull String name) {
            return getByName(name);
        }

        @Nullable
        public static SourceMode getByName(String name) {
            for (SourceMode i : SourceMode.values()) {
                if (i.getName().equals(name)) {
                    return i;
                }
            }
            return null;
        }

        @Override
        public @NotNull String getLocalizationKeyBase() {
            return "fancymenu.elements.text.v2.source_mode";
        }

        @Override
        public @NotNull Style getValueComponentStyle() {
            return WARNING_TEXT_STYLE.get();
        }

    }

}
