package de.keksuccino.fancymenu.customization.element.elements.tooltip;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.util.enums.LocalizedCycleEnum;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownRenderer;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.text.IText;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import de.keksuccino.konkrete.input.StringUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Style;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class TooltipElement extends AbstractElement {

    private static final Logger LOGGER = LogManager.getLogger();

    @NotNull
    protected SourceMode sourceMode = SourceMode.DIRECT;
    @Nullable
    protected String source; //direct text or file path
    protected volatile String text;
    protected String lastText;
    @Nullable
    public ResourceSupplier<IText> textResourceSupplier;
    @Nullable
    public ResourceSupplier<ITexture> backgroundTexture;
    // Nine-slicing is mandatory for custom tooltip backgrounds
    public final Property.IntegerProperty nineSliceBorderTop = putProperty(Property.integerProperty("nine_slice_border_top", 5, "fancymenu.elements.tooltip.nine_slice.border.top"));
    public final Property.IntegerProperty nineSliceBorderRight = putProperty(Property.integerProperty("nine_slice_border_right", 5, "fancymenu.elements.tooltip.nine_slice.border.right"));
    public final Property.IntegerProperty nineSliceBorderBottom = putProperty(Property.integerProperty("nine_slice_border_bottom", 5, "fancymenu.elements.tooltip.nine_slice.border.bottom"));
    public final Property.IntegerProperty nineSliceBorderLeft = putProperty(Property.integerProperty("nine_slice_border_left", 5, "fancymenu.elements.tooltip.nine_slice.border.left"));
    public boolean mouseFollowing = false;
    public boolean interactable = false;
    @NotNull
    public volatile MarkdownRenderer markdownRenderer = new MarkdownRenderer();
    public final Property.FloatProperty textScale = putProperty(Property.floatProperty("scale", 1.0F, "fancymenu.elements.text.scale"));
    public final Property.IntegerProperty textBorder = putProperty(Property.integerProperty("text_border", 2, "fancymenu.elements.text.text_border"));
    public final Property.IntegerProperty lineSpacing = putProperty(Property.integerProperty("line_spacing", 2, "fancymenu.elements.text.line_spacing"));
    protected List<String> lastLines;
    protected IText lastIText;
    protected int cachedRealHeight = 0;

    public TooltipElement(@NotNull ElementBuilder<?, ?> builder) {
        super(builder);

        this.allowDepthTestManipulation = true;
        this.supportsTilting = false;
        
        // Configure markdown renderer for tooltip style
        this.markdownRenderer.setAutoLineBreakingEnabled(true);
        
        // Don't render markdown lines outside visible area (for performance reasons)
        this.markdownRenderer.addLineRenderValidator(line -> {
            if ((line.parent.getY() + line.offsetY + line.getLineHeight()) < this.getAbsoluteY()) {
                return false;
            }
            if ((line.parent.getY() + line.offsetY) > (this.getAbsoluteY() + this.getAbsoluteHeight())) {
                return false;
            }
            return true;
        });
    }

    @Override
    public void renderInternal(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (RenderingUtils.isTooltipRenderingBlocked()) return;

        if (isEditor()) {
            super.renderInternal(graphics, mouseX, mouseY, partial);
        } else {
            RenderingUtils.addDeferredScreenRenderingTask(super::renderInternal);
        }

    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (!this.shouldRender()) return;

        try {
            this.renderTick();
            
            this.markdownRenderer.setTextOpacity(this.opacity);
            
            int x = this.getAbsoluteX();
            int y = this.getAbsoluteY();
            int width = this.getAbsoluteWidth();
            int height = this.getAbsoluteHeight();
            
            // Add padding for the content
            int paddingX = 6;
            int paddingY = 6;
            
            RenderSystem.enableBlend();
            
            // Render tooltip background
            if (this.backgroundTexture != null) {
                ITexture tex = this.backgroundTexture.get();
                if (tex != null && tex.getResourceLocation() != null) {
                    // Get texture dimensions
                    int textureWidth = Math.max(1, tex.getWidth());
                    int textureHeight = Math.max(1, tex.getHeight());

                    if (!isEditor()) {
                        graphics.pose().pushPose();
                        graphics.pose().translate(0.0F, 0.0F, 400.0F);
                    }

                    // Render custom background texture with nine-slicing
                    RenderingUtils.blitNineSlicedTexture(graphics, tex.getResourceLocation(), 
                        x, y, width, height, 
                        textureWidth, textureHeight,
                        this.nineSliceBorderTop.getInteger(), this.nineSliceBorderRight.getInteger(), 
                        this.nineSliceBorderBottom.getInteger(), this.nineSliceBorderLeft.getInteger());

                    if (!isEditor()) graphics.pose().popPose();

                } else {
                    // Render vanilla tooltip background
                    this.renderVanillaTooltipBackground(graphics, x, y, width, height);
                }
            } else {
                // Render vanilla tooltip background
                this.renderVanillaTooltipBackground(graphics, x, y, width, height);
            }

            if (!isEditor()) {
                graphics.pose().pushPose();
                graphics.pose().translate(0.0F, 0.0F, 400.0F);
            }

            // Render markdown content with padding
            this.markdownRenderer.setX(x + paddingX);
            this.markdownRenderer.setY(y + paddingY);
            this.markdownRenderer.render(graphics, mouseX, mouseY, partial);

            if (!isEditor()) graphics.pose().popPose();
            
        } catch (Exception e) {
            LOGGER.error("[FANCYMENU] Failed to render Tooltip element!", e);
        }

        RenderingUtils.resetShaderColor(graphics);

    }
    
    private void renderVanillaTooltipBackground(GuiGraphics graphics, int x, int y, int width, int height) {
        // Use vanilla tooltip background rendering
        TooltipRenderUtil.renderTooltipBackground(graphics, x, y, width, height, isEditor() ? 0 : 400);
    }

    protected void renderTick() {
        this.syncMarkdownRendererProperties();

        // Update width for markdown renderer (account for padding)
        int paddingX = 6;
        this.markdownRenderer.setOptimalWidth(this.getAbsoluteWidth() - (paddingX * 2));
        
        // Update cached real height
        this.cachedRealHeight = (int) this.markdownRenderer.getRealHeight();
        
        // If IText instance or its content changes, update element
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

        // Update markdown renderer text if changed
        String t = this.text;
        if ((t != null) && ((this.lastText == null) || !this.lastText.equals(t))) {
            this.markdownRenderer.setText(t);
        }
        this.lastText = t;
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
    }

    @Override
    public int getAbsoluteX() {
        if (this.mouseFollowing && !isEditor()) {
            int x = this.cachedMouseX;
            int tooltipWidth = this.getAbsoluteWidth();
            int screenWidth = getScreenWidth();

            // If tooltip would go off the right edge, move it left
            if (x + tooltipWidth > screenWidth) {
                x = screenWidth - tooltipWidth;
            }
            // If tooltip would go off the left edge, move it right
            if (x < 0) {
                x = 0;
            }

            return x;
        }
        return super.getAbsoluteX();
    }

    @Override
    public int getAbsoluteY() {
        if (this.mouseFollowing && !isEditor()) {
            int y = this.cachedMouseY;
            int tooltipHeight = this.getAbsoluteHeight();
            int screenHeight = getScreenHeight();

            // If tooltip would go off the bottom edge, move it up
            if (y + tooltipHeight > screenHeight) {
                y = screenHeight - tooltipHeight;
            }
            // If tooltip would go off the top edge, move it down
            if (y < 0) {
                y = 0;
            }

            return y;
        }
        return super.getAbsoluteY();
    }

    @Override
    public int getAbsoluteHeight() {
        // Always return the height needed to fit the markdown content
        return Math.max(this.cachedRealHeight + 12, 20); // Add some padding
    }

    public void updateContent() {
        if (this.source == null) {
            this.markdownRenderer.setText("-------------------");
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
                    linesRaw.add("-------------------");
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
