package de.keksuccino.fancymenu.customization.element.elements.splash;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinSplashRenderer;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.text.IText;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.awt.*;
import java.util.List;
import java.util.Objects;

public class SplashTextElement extends AbstractElement {

    public SourceMode sourceMode = SourceMode.DIRECT_TEXT;
    public String source = "Splash Text";
    @Nullable
    public ResourceSupplier<IText> textFileSupplier;
    public float scale = 1.0F;
    public boolean shadow = true;
    public boolean bounce = true;
    public float rotation = 20.0F;
    public DrawableColor baseColor = DrawableColor.of(new Color(255, 255, 0));
    public boolean refreshOnMenuReload = false;
    public Font font = Minecraft.getInstance().font;
    protected float baseScale = 1.8F;
    protected RenderText renderText = null;
    protected String lastSource = null;
    protected SourceMode lastSourceMode = null;
    protected boolean refreshedOnMenuLoad = false;

    public SplashTextElement(@NotNull ElementBuilder<?, ?> builder) {
        super(builder);
        this.allowDepthTestManipulation = true;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        if (this.shouldRender()) {
            this.updateSplash();
            this.renderSplash(graphics);
        }
    }

    public void refresh() {
        this.getBuilder().splashCache.remove(this.getInstanceIdentifier());
        this.renderText = null;
    }

    /**
     * Updates the splash text content based on the source mode.
     * This logic is safe and does not need changes.
     */
    protected void updateSplash() {
        if (isEditor()) {
            if (!Objects.equals(this.lastSource, this.source) || !Objects.equals(this.lastSourceMode, this.sourceMode)) {
                this.refresh();
            }
            this.lastSource = this.source;
            this.lastSourceMode = this.sourceMode;
        }

        if ((this.sourceMode != SourceMode.VANILLA) && (this.source == null)) return;

        if (this.getBuilder().isNewMenu && this.refreshOnMenuReload && !this.refreshedOnMenuLoad) {
            this.refresh();
            this.refreshedOnMenuLoad = true;
        }
        if ((this.renderText == null) && (this.getBuilder().splashCache.containsKey(this.getInstanceIdentifier()))) {
            this.renderText = this.getBuilder().splashCache.get(this.getInstanceIdentifier()).renderText;
        }

        if (this.renderText == null) {
            //VANILLA
            if (this.sourceMode == SourceMode.VANILLA) {
                SplashRenderer splashRenderer = Minecraft.getInstance().getSplashManager().getSplash();
                // Assumes IMixinSplashRenderer correctly retrieves the splash text string
                this.renderText = new RenderText((splashRenderer != null) ? ((IMixinSplashRenderer)splashRenderer).getSplashFancyMenu() : Component.empty(), null);
            }
            //TEXT FILE
            if (this.sourceMode == SourceMode.TEXT_FILE) {
                if (this.textFileSupplier != null) {
                    IText text = this.textFileSupplier.get();
                    if (text != null) {
                        List<String> l = text.getTextLines();
                        if (l != null) {
                            if (!l.isEmpty() && ((l.size() > 1) || (!l.get(0).trim().isEmpty()))) {
                                int i = MathUtils.getRandomNumberInRange(0, l.size() - 1);
                                this.renderText = new RenderText(null, l.get(i));
                            } else {
                                this.renderText = new RenderText(Component.literal("§cERROR: SPLASH FILE IS EMPTY"), null);
                            }
                        }
                    }
                }
            }
            //DIRECT
            if (this.sourceMode == SourceMode.DIRECT_TEXT) {
                this.renderText = new RenderText(null, this.source);
            }
        }

        this.getBuilder().splashCache.put(this.getInstanceIdentifier(), this);
    }

    /**
     * Renders the splash text using the modern GuiGraphics transformation stack.
     *
     * @param graphics The GuiGraphics instance.
     */
    protected void renderSplash(GuiGraphics graphics) {
        if (this.renderText == null) {
            if (isEditor()) {
                this.renderText = new RenderText(Component.literal("< empty splash element >"), null);
            } else {
                return;
            }
        }

        var renderTextComponent = this.renderText.buildForRenderTick();

        // Calculate the "bounce" effect scale, same logic as vanilla.
        float bounceScale = this.baseScale;
        if (this.bounce) {
            bounceScale -= Mth.abs(Mth.sin((float) (System.currentTimeMillis() % 1000L) / 1000.0F * ((float) Math.PI * 2F)) * 0.1F);
        }
        // Adjust scale based on text length to prevent it from being too wide.
        bounceScale *= 100.0F / (float) (font.width(renderTextComponent) + 32);

        // Calculate final color and alpha
        int finalAlpha = Mth.ceil(this.opacity * 255.0F);
        int finalColor = ARGB.color(finalAlpha, this.baseColor.getColorInt());

        // --- NEW: Modern Transformation Logic ---
        graphics.pose().pushMatrix();

        // 1. Translate to the center of the element's defined area. This becomes our origin.
        graphics.pose().translate(this.getAbsoluteX() + (this.getAbsoluteWidth() / 2.0F), this.getAbsoluteY() + (this.getAbsoluteHeight() / 2.0F));

        // 2. Rotate around the new origin. We negate the rotation to match vanilla's slant.
        //    JOML's rotate method takes radians.
        graphics.pose().rotate((float) Math.toRadians(-this.rotation));

        // 3. Apply the combined scale (user-defined scale * bounce scale).
        float finalScale = this.scale * bounceScale;
        graphics.pose().scale(finalScale, finalScale);

        // 4. Draw the string. We use drawString to respect the 'shadow' property.
        //    We center it manually around the new (0,0) origin.
        int textWidth = font.width(renderTextComponent);
        graphics.drawString(font, renderTextComponent, -textWidth / 2, -font.lineHeight / 2, finalColor, this.shadow);

        graphics.pose().popMatrix();
    }

    protected SplashTextElementBuilder getBuilder() {
        return (SplashTextElementBuilder) this.builder;
    }

    public enum SourceMode {
        DIRECT_TEXT("direct"),
        TEXT_FILE("text_file"),
        VANILLA("vanilla");

        final String name;

        SourceMode(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        public static SourceMode getByName(String name) {
            for (SourceMode i : SourceMode.values()) {
                if (i.getName().equals(name)) {
                    return i;
                }
            }
            return null;
        }
    }

    protected record RenderText(@Nullable Component component, @Nullable String string) {
        @NotNull
        protected Component buildForRenderTick() {
            return (this.component != null) ? this.component : buildComponent(Objects.requireNonNullElse(this.string, ""));
        }
    }

}