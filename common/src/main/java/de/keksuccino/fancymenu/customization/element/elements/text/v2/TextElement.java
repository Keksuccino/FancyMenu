package de.keksuccino.fancymenu.customization.element.elements.text.v2;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import de.keksuccino.fancymenu.util.ListUtils;
import de.keksuccino.fancymenu.util.enums.LocalizedCycleEnum;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownRenderer;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.text.IText;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import de.keksuccino.konkrete.input.StringUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class TextElement extends AbstractElement {

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
    public final Property.FloatProperty textScale = putProperty(Property.floatProperty("scale", 1.0F, "fancymenu.elements.text.scale"));
    public final Property.IntegerProperty textBorder = putProperty(Property.integerProperty("text_border", 2, "fancymenu.elements.text.text_border"));
    public final Property.IntegerProperty lineSpacing = putProperty(Property.integerProperty("line_spacing", 2, "fancymenu.elements.text.line_spacing"));
    public final Property.IntegerProperty quoteIndent = putProperty(Property.integerProperty("quote_indent", 8, "fancymenu.elements.text.markdown.quote.indent"));
    public final Property.IntegerProperty bulletListIndent = putProperty(Property.integerProperty("bullet_list_indent", 8, "fancymenu.elements.text.markdown.bullet_list.indent"));
    public final Property.IntegerProperty bulletListSpacing = putProperty(Property.integerProperty("bullet_list_spacing", 3, "fancymenu.elements.text.markdown.bullet_list.spacing"));
    public final Property.FloatProperty tableLineThickness = putProperty(Property.floatProperty("table_line_thickness", 1.0F, "fancymenu.elements.text.markdown.tables.line_thickness"));
    public final Property.FloatProperty tableCellPadding = putProperty(Property.floatProperty("table_cell_padding", 8.0F, "fancymenu.elements.text.markdown.tables.cell_padding"));
    public final Property.FloatProperty tableMargin = putProperty(Property.floatProperty("table_margin", 4.0F, "fancymenu.elements.text.markdown.tables.margin"));

    protected List<String> lastLines;
    protected IText lastIText;
    protected boolean lastTickShouldRender = false;

    public TextElement(@NotNull ElementBuilder<?, ?> builder) {

        super(builder);

        this.allowDepthTestManipulation = true;

        this.scrollArea = new ScrollArea(0, 0, this.getAbsoluteWidth(), this.getAbsoluteHeight()) {
            @Override
            public void updateScrollArea() {
                super.updateScrollArea();
                //Manually update scroll bar area size, so the grabbers are outside the area
                if (Minecraft.getInstance().screen != null) {
                    this.verticalScrollBar.scrollAreaEndX = TextElement.this.getAbsoluteX() + TextElement.this.getAbsoluteWidth() + 12;
                    this.horizontalScrollBar.scrollAreaEndY = TextElement.this.getAbsoluteY() + TextElement.this.getAbsoluteHeight() + 12;
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
        this.scrollArea.backgroundColor = () -> DrawableColor.of(0,0,0,0);
        this.scrollArea.borderColor = () -> DrawableColor.of(0,0,0,0);

        this.scrollArea.addEntry(new MarkdownRendererEntry(this.scrollArea, this.markdownRenderer));
        // Ensure markdown can render once to measure its size before culling kicks in.
        this.scrollArea.setRenderOnlyEntriesInArea(false);

        //Don't render markdown lines outside visible area (for performance reasons)
        this.markdownRenderer.addLineRenderValidator(line -> {
            if ((line.parent.getY() + line.offsetY + line.getLineHeight()) < this.scrollArea.getInnerY()) {
                return false;
            }
            if ((line.parent.getY() + line.offsetY) > (this.scrollArea.getInnerY() + this.scrollArea.getInnerHeight())) {
                return false;
            }
            return true;
        });

        this.markdownRenderer.setTextEventHandler(new MarkdownRenderer.TextEventHandler() {
            @Override
            public void onTextClickEvent(@NotNull String eventId) {
                if (TextElement.this.isEditor() || !TextElement.this.interactable) {
                    return;
                }
                if (eventId.isEmpty()) {
                    return;
                }
                Listeners.ON_TEXT_CLICKED.onTextClicked(eventId);
            }

            @Override
            public void onTextHoverEvent(@NotNull String eventId) {
                if (TextElement.this.isEditor() || !TextElement.this.interactable) {
                    return;
                }
                if (eventId.isEmpty()) {
                    return;
                }
                Listeners.ON_TEXT_HOVERED.onTextHovered(eventId);
            }
        });

    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (this.shouldRender()) {

            try {

                this.renderTick();

                this.markdownRenderer.setTextOpacity(this.opacity);

                RenderSystem.enableBlend();
                this.scrollArea.setX(this.getAbsoluteX(), true);
                this.scrollArea.setY(this.getAbsoluteY(), true);
                this.scrollArea.setWidth(this.getAbsoluteWidth(), true);
                this.scrollArea.setHeight(this.getAbsoluteHeight(), true);
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

        this.syncMarkdownRendererProperties();

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

        //Update scroll grabber colors
        if (this.scrollGrabberColorHexNormal.get() != null) {
            DrawableColor c = this.scrollGrabberColorHexNormal.getDrawable();
            this.scrollArea.verticalScrollBar.idleBarColor = () -> c;
            this.scrollArea.horizontalScrollBar.idleBarColor = () -> c;
        }
        if (this.scrollGrabberColorHexHover.get() != null) {
            DrawableColor c = this.scrollGrabberColorHexHover.getDrawable();
            this.scrollArea.verticalScrollBar.hoverBarColor = () -> c;
            this.scrollArea.horizontalScrollBar.hoverBarColor = () -> c;
        }

        //Update scroll grabber textures
        if (this.verticalScrollGrabberTextureNormal != null) {
            ITexture r = this.verticalScrollGrabberTextureNormal.get();
            if (r != null) {
                this.scrollArea.verticalScrollBar.idleBarTexture = r.getResourceLocation();
            }
        } else {
            this.scrollArea.verticalScrollBar.idleBarTexture = null;
        }
        if (this.verticalScrollGrabberTextureHover != null) {
            ITexture r = this.verticalScrollGrabberTextureHover.get();
            if (r != null) {
                this.scrollArea.verticalScrollBar.hoverBarTexture = r.getResourceLocation();
            }
        } else {
            this.scrollArea.verticalScrollBar.hoverBarTexture = null;
        }
        if (this.horizontalScrollGrabberTextureNormal != null) {
            ITexture r = this.horizontalScrollGrabberTextureNormal.get();
            if (r != null) {
                this.scrollArea.horizontalScrollBar.idleBarTexture = r.getResourceLocation();
            }
        } else {
            this.scrollArea.horizontalScrollBar.idleBarTexture = null;
        }
        if (this.horizontalScrollGrabberTextureHover != null) {
            ITexture r = this.horizontalScrollGrabberTextureHover.get();
            if (r != null) {
                this.scrollArea.horizontalScrollBar.hoverBarTexture = r.getResourceLocation();
            }
        } else {
            this.scrollArea.horizontalScrollBar.hoverBarTexture = null;
        }

    }

    protected void syncMarkdownRendererProperties() {
        float scale = Math.max(0.0F, this.textScale.getFloat());
        if (this.markdownRenderer.getTextBaseScale() != scale) {
            this.markdownRenderer.setTextBaseScale(scale);
        }

        int border = Math.max(0, this.textBorder.getInteger());
        if ((int)this.markdownRenderer.getBorder() != border) {
            this.markdownRenderer.setBorder(border);
        }

        int spacing = Math.max(0, this.lineSpacing.getInteger());
        if ((int)this.markdownRenderer.getLineSpacing() != spacing) {
            this.markdownRenderer.setLineSpacing(spacing);
        }

        int quoteIndentValue = Math.max(0, this.quoteIndent.getInteger());
        if ((int)this.markdownRenderer.getQuoteIndent() != quoteIndentValue) {
            this.markdownRenderer.setQuoteIndent(quoteIndentValue);
        }

        int bulletIndentValue = Math.max(0, this.bulletListIndent.getInteger());
        if ((int)this.markdownRenderer.getBulletListIndent() != bulletIndentValue) {
            this.markdownRenderer.setBulletListIndent(bulletIndentValue);
        }

        int bulletSpacingValue = Math.max(0, this.bulletListSpacing.getInteger());
        if ((int)this.markdownRenderer.getBulletListSpacing() != bulletSpacingValue) {
            this.markdownRenderer.setBulletListSpacing(bulletSpacingValue);
        }

        float lineThickness = Math.max(0.0F, this.tableLineThickness.getFloat());
        if (this.markdownRenderer.getTableLineThickness() != lineThickness) {
            this.markdownRenderer.setTableLineThickness(lineThickness);
        }

        float cellPadding = Math.max(0.0F, this.tableCellPadding.getFloat());
        if (this.markdownRenderer.getTableCellPadding() != cellPadding) {
            this.markdownRenderer.setTableCellPadding(cellPadding);
        }

        float margin = Math.max(0.0F, this.tableMargin.getFloat());
        if (this.markdownRenderer.getTableMargin() != margin) {
            this.markdownRenderer.setTableMargin(margin);
        }
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

    protected static class MarkdownRendererEntry extends ScrollAreaEntry {

        protected MarkdownRenderer markdownRenderer;

        public MarkdownRendererEntry(ScrollArea parent, MarkdownRenderer markdownRenderer) {
            super(parent, 20, 20);
            this.markdownRenderer = markdownRenderer;
            this.selectable = false;
            this.playClickSound = false;
            this.backgroundColorNormal = () -> DrawableColor.of(0,0,0,0);
            this.backgroundColorHover = () -> DrawableColor.of(0,0,0,0);
        }

        @Override
        public void renderEntry(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            this.markdownRenderer.setOptimalWidth(this.parent.getInnerWidth());
            this.markdownRenderer.setX(this.x);
            this.markdownRenderer.setY(this.y);
            this.setWidth(this.markdownRenderer.getRealWidth());
            this.setHeight(this.markdownRenderer.getRealHeight());
            this.markdownRenderer.render(graphics, mouseX, mouseY, partial);
        }

        @Override
        public void onClick(ScrollAreaEntry entry, double mouseX, double mouseY, int button) {
        }

    }

}
