package de.keksuccino.fancymenu.customization.element.elements.tooltip;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.util.ListUtils;
import de.keksuccino.fancymenu.util.enums.LocalizedCycleEnum;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownRenderer;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.text.IText;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import de.keksuccino.konkrete.input.StringUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class TooltipElement extends AbstractElement {

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
    public int nineSliceBorderTop = 5;
    public int nineSliceBorderRight = 5;
    public int nineSliceBorderBottom = 5;
    public int nineSliceBorderLeft = 5;
    public boolean mouseFollowing = false;
    public int maxWidth = 200;
    public boolean interactable = false;
    @NotNull
    public volatile MarkdownRenderer markdownRenderer = new MarkdownRenderer();
    protected List<String> lastLines;
    protected IText lastIText;
    protected int cachedRealHeight = 0;

    public TooltipElement(@NotNull ElementBuilder<?, ?> builder) {
        super(builder);
        
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
    public int getAbsoluteX() {
        if (this.mouseFollowing && !isEditor()) {
            return this.cachedMouseX;
        }
        return super.getAbsoluteX();
    }
    
    @Override
    public int getAbsoluteY() {
        if (this.mouseFollowing && !isEditor()) {
            return this.cachedMouseY;
        }
        return super.getAbsoluteY();
    }
    
    @Override
    public int getAbsoluteWidth() {
        // Use maxWidth as the width of the tooltip
        return this.maxWidth;
    }
    
    @Override
    public int getAbsoluteHeight() {
        // Always return the height needed to fit the markdown content
        return Math.max(this.cachedRealHeight + 12, 20); // Add some padding
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

                    graphics.pose().pushPose();
                    graphics.pose().translate(0.0F, 0.0F, 400.0F);

                    // Render custom background texture with nine-slicing
                    RenderingUtils.blitNineSlicedTexture(graphics, tex.getResourceLocation(), 
                        x, y, width, height, 
                        textureWidth, textureHeight,
                        this.nineSliceBorderTop, this.nineSliceBorderRight, 
                        this.nineSliceBorderBottom, this.nineSliceBorderLeft);

                    graphics.pose().popPose();

                } else {
                    // Render vanilla tooltip background
                    this.renderVanillaTooltipBackground(graphics, x, y, width, height);
                }
            } else {
                // Render vanilla tooltip background
                this.renderVanillaTooltipBackground(graphics, x, y, width, height);
            }

            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, 0.0F, 400.0F);
            
            // Render markdown content with padding
            this.markdownRenderer.setX(x + paddingX);
            this.markdownRenderer.setY(y + paddingY);
            this.markdownRenderer.render(graphics, mouseX, mouseY, partial);

            graphics.pose().popPose();
            
        } catch (Exception e) {
            e.printStackTrace();
        }

        RenderingUtils.resetShaderColor(graphics);
    }
    
    private void renderVanillaTooltipBackground(GuiGraphics graphics, int x, int y, int width, int height) {
        // Use vanilla tooltip background rendering
        TooltipRenderUtil.renderTooltipBackground(graphics, x, y, width, height, 400);
    }

    @Override
    public @Nullable List<GuiEventListener> getWidgetsToRegister() {
        return this.interactable ? ListUtils.of(this.markdownRenderer) : null;
    }

    protected void renderTick() {
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

    public void updateContent() {
        if (this.source == null) {
            this.markdownRenderer.setText(I18n.get("fancymenu.customization.items.tooltip.placeholder"));
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
                    linesRaw.add(I18n.get("fancymenu.customization.items.tooltip.placeholder"));
                }
            } catch (Exception ex) {
                if (linesRaw == null) linesRaw = new ArrayList<>();
                linesRaw.clear();
            }

            if (linesRaw.isEmpty()) {
                if (isEditor()) {
                    linesRaw.add(I18n.get("fancymenu.customization.items.text.status.unable_to_load"));
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
