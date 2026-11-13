package de.keksuccino.fancymenu.customization.element.elements.text.v2;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.elements.MarkdownSettings;
import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import de.keksuccino.fancymenu.util.ListUtils;
import de.keksuccino.fancymenu.util.enums.LocalizedCycleEnum;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownRenderer;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownRendererEntry;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollbar.ScrollBar;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.text.IText;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import de.keksuccino.konkrete.input.StringUtils;
import net.minecraft.client.Minecraft;
import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class TextElement extends AbstractElement {

    /** The space a scroll bar has its grabber in. It lies outside of the element: next to the text area, or next to a background that does not reach behind the scroll bars. */
    public static final int SCROLL_BAR_SPACE = 12;

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
    public boolean enableScrolling = true;
    public boolean interactable = true;
    @NotNull
    public volatile MarkdownRenderer markdownRenderer = new MarkdownRenderer();
    @NotNull
    public volatile ScrollArea scrollArea;

    public final Property.ColorProperty scrollGrabberColorHexNormal = putProperty(Property.hexColorProperty("grabber_color_normal", null, true, "fancymenu.elements.text.scroll_grabber_color.normal"));
    public final Property.ColorProperty scrollGrabberColorHexHover = putProperty(Property.hexColorProperty("grabber_color_hover", null, true, "fancymenu.elements.text.scroll_grabber_color.hover"));
    public final MarkdownSettings markdownSettings = new MarkdownSettings(this, this.markdownRenderer);
    public final Property<ScrollGrabberDesign> scrollGrabberDesign = putProperty(ScrollGrabberDesign.buildProperty());
    public final TextBackground background = new TextBackground(this);

    protected List<String> lastLines;
    protected IText lastIText;
    protected boolean lastTickShouldRender = false;
    /** The grabber colors of the UI theme, which the scroll bars of the scroll area start with. */
    protected final Supplier<DrawableColor> themeGrabberColorNormal;
    protected final Supplier<DrawableColor> themeGrabberColorHover;
    /** The grabber colors of the user, read once per frame. The suppliers the scroll bars ask for them are the same objects for every frame. */
    @Nullable
    protected DrawableColor customGrabberColorNormal;
    @Nullable
    protected DrawableColor customGrabberColorHover;
    protected final Supplier<DrawableColor> customGrabberColorNormalSupplier = () -> this.customGrabberColorNormal;
    protected final Supplier<DrawableColor> customGrabberColorHoverSupplier = () -> this.customGrabberColorHover;

    public TextElement(@NotNull ElementBuilder<?, ?> builder) {

        super(builder);

        this.allowDepthTestManipulation = true;

        this.scrollArea = new ScrollArea(0, 0, this.getAbsoluteWidth(), this.getAbsoluteHeight()) {
            @Override
            public void updateScrollArea() {
                super.updateScrollArea();
                //Manually update scroll bar area size, so the grabbers are outside the area, and outside of a background that does not reach behind them
                if (Minecraft.getInstance().screen != null) {
                    int distance = TextElement.this.background.getScrollBarDistance();
                    this.verticalScrollBar.scrollAreaEndX = TextElement.this.getAbsoluteX() + TextElement.this.getAbsoluteWidth() + distance + SCROLL_BAR_SPACE;
                    this.horizontalScrollBar.scrollAreaEndY = TextElement.this.getAbsoluteY() + TextElement.this.getAbsoluteHeight() + distance + SCROLL_BAR_SPACE;
                }
            }
            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (isEditor()) {
                    if (this.verticalScrollBar.mouseClicked(mouseX, mouseY, button)) return true;
                    if (this.horizontalScrollBar.mouseClicked(mouseX, mouseY, button)) return true;
                    return false;
                }
                return super.mouseClicked(mouseX, mouseY, button);
            }
            @Override
            public boolean isMouseOver(double mouseX, double mouseY) {
                if (isEditor()) return false;
                return super.isMouseOver(mouseX, mouseY);
            }
        };
        this.scrollArea.minimumEntryWidthIsAreaWidth = false;
        this.scrollArea.makeEntriesWidthOfArea = false;
        this.scrollArea.makeAllEntriesWidthOfWidestEntry = false;
        this.scrollArea.verticalScrollBar.grabberWidth = 10;
        this.scrollArea.verticalScrollBar.grabberHeight = 20;
        this.scrollArea.horizontalScrollBar.grabberWidth = 20;
        this.scrollArea.horizontalScrollBar.grabberHeight = 10;
        this.scrollArea.backgroundColor = () -> DrawableColor.FULLY_TRANSPARENT;
        this.scrollArea.borderColor = () -> DrawableColor.FULLY_TRANSPARENT;

        this.themeGrabberColorNormal = this.scrollArea.verticalScrollBar.idleBarColor;
        this.themeGrabberColorHover = this.scrollArea.verticalScrollBar.hoverBarColor;
        ScrollGrabberDesign.followAppliedLooks(this.scrollGrabberDesign, this.markdownSettings);

        this.scrollArea.addEntry(new MarkdownRendererEntry(this.scrollArea, this.markdownRenderer));
        // Ensure markdown can render once to measure its size before culling kicks in.
        this.scrollArea.setRenderOnlyEntriesInArea(false);

        this.markdownRenderer.setTextEventHandler(new MarkdownRenderer.TextEventHandler() {
            @Override
            public void onTextClickEvent(@NotNull String eventId) {
                if (TextElement.this.isEditor() || !TextElement.this.interactable) {
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
                if (TextElement.this.isEditor() || !TextElement.this.interactable) {
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
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (this.shouldRender()) {

            try {

                this.renderTick();

                this.markdownRenderer.setTextOpacity(this.opacity);

                int x = this.getAbsoluteX();
                int y = this.getAbsoluteY();
                int width = this.getAbsoluteWidth();
                int height = this.getAbsoluteHeight();

                this.background.render(graphics, x, y, width, height, this.scrollArea, this.opacity);

                RenderSystem.enableBlend();
                this.scrollArea.setX(x, true);
                this.scrollArea.setY(y, true);
                this.scrollArea.setWidth(width, true);
                this.scrollArea.setHeight(height, true);
                this.scrollArea.render(graphics, mouseX, mouseY, partial);

            } catch (Exception e) {
                e.printStackTrace();
            }

            RenderingUtils.resetShaderColor(graphics);

        }

    }

    @Override
    public void renderInternal(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        super.renderInternal(graphics, mouseX, mouseY, partial);

        if (!this.shouldRender() && this.lastTickShouldRender) {
            this.markdownRenderer.resetHovered();
        }
        this.lastTickShouldRender = this.shouldRender();

    }

    @Override
    public @Nullable List<GuiEventListener> getWidgetsToRegister() {
        return this.interactable ? ListUtils.of(this.markdownRenderer, this.scrollArea) : null;
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

        //Update scroll wheel ONLY FOR vertical bar (horizontal never scrolls via scroll wheel)
        this.scrollArea.verticalScrollBar.setScrollWheelAllowed(this.enableScrolling);

        //Update active state of scroll bars
        this.scrollArea.verticalScrollBar.active = (this.scrollArea.getTotalEntryHeight() > this.scrollArea.getInnerHeight()) && this.enableScrolling;
        this.scrollArea.horizontalScrollBar.active = (this.scrollArea.getTotalEntryWidth() > this.scrollArea.getInnerWidth()) && this.enableScrolling;

        this.updateScrollGrabbers();

    }

    /**
     * Gives the scroll bars the colors and textures of their grabbers, and the opacity of the element.<br>
     * Textures and colors of the user win. Without any of them, the design of the element decides what the grabbers look like.
     */
    protected void updateScrollGrabbers() {

        ScrollBar vertical = this.scrollArea.verticalScrollBar;
        ScrollBar horizontal = this.scrollArea.horizontalScrollBar;

        vertical.grabberOpacity = this.opacity;
        horizontal.grabberOpacity = this.opacity;

        this.customGrabberColorNormal = (this.scrollGrabberColorHexNormal.get() != null) ? this.scrollGrabberColorHexNormal.getDrawable() : null;
        this.customGrabberColorHover = (this.scrollGrabberColorHexHover.get() != null) ? this.scrollGrabberColorHexHover.getDrawable() : null;
        Supplier<DrawableColor> normalColor = (this.customGrabberColorNormal != null) ? this.customGrabberColorNormalSupplier : this.themeGrabberColorNormal;
        Supplier<DrawableColor> hoverColor = (this.customGrabberColorHover != null) ? this.customGrabberColorHoverSupplier : this.themeGrabberColorHover;
        vertical.idleBarColor = normalColor;
        horizontal.idleBarColor = normalColor;
        vertical.hoverBarColor = hoverColor;
        horizontal.hoverBarColor = hoverColor;

        boolean customized = (this.customGrabberColorNormal != null) || (this.customGrabberColorHover != null)
                || (this.verticalScrollGrabberTextureNormal != null) || (this.verticalScrollGrabberTextureHover != null)
                || (this.horizontalScrollGrabberTextureNormal != null) || (this.horizontalScrollGrabberTextureHover != null);
        ScrollGrabberDesign design = this.scrollGrabberDesign.tryGetNonNullElse(ScrollGrabberDesign.THEME_BAR).unlessCustomized(customized);
        vertical.idleBarTexture = getGrabberTexture(this.verticalScrollGrabberTextureNormal, vertical.idleBarTexture, design.getTexture(true, false));
        vertical.hoverBarTexture = getGrabberTexture(this.verticalScrollGrabberTextureHover, vertical.hoverBarTexture, design.getTexture(true, true));
        horizontal.idleBarTexture = getGrabberTexture(this.horizontalScrollGrabberTextureNormal, horizontal.idleBarTexture, design.getTexture(false, false));
        horizontal.hoverBarTexture = getGrabberTexture(this.horizontalScrollGrabberTextureHover, horizontal.hoverBarTexture, design.getTexture(false, true));

    }

    /**
     * Returns the texture a grabber gets drawn with, or NULL if it gets drawn in a color.
     *
     * @param custom The texture of the user for the grabber, if there is one.
     * @param current The texture the grabber has right now. A texture of the user that is still loading keeps it in place,
     *                unless it is a texture of a design, which the user did not ask for anymore.
     * @param designTexture The texture the design of the element has for the grabber.
     */
    @Nullable
    protected static ResourceLocation getGrabberTexture(@Nullable ResourceSupplier<ITexture> custom, @Nullable ResourceLocation current, @Nullable ResourceLocation designTexture) {
        if (custom == null) return designTexture;
        ITexture texture = custom.get();
        if (texture != null) return texture.getResourceLocation();
        return ScrollGrabberDesign.isDesignTexture(current) ? null : current;
    }

    /**
     * Returns the height in pixels this element needs to show all of its text without scrolling.<br>
     * The markdown renderer lays out the text while the element renders, so this reflects the most recently rendered text, line wrapping and text settings.
     */
    public int getContentHeight() {
        return (int) Math.ceil(this.markdownRenderer.getRealHeight() + (this.scrollArea.getBorderThickness() * 2));
    }

    public void updateContent() {

        if (this.source == null) {
            this.markdownRenderer.setText(I18n.get("fancymenu.elements.text.placeholder"));
            return;
        }

        new Thread(() -> {

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

        }).start();

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
