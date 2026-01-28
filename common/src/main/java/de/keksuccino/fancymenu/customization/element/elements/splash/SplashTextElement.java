package de.keksuccino.fancymenu.customization.element.elements.splash;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinSplashRenderer;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.text.IText;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.network.chat.Component;
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
    public boolean refreshOnMenuReload = false;
    public Font font = Minecraft.getInstance().font;

    public final Property.ColorProperty baseColor = putProperty(Property.hexColorProperty("base_color", DrawableColor.of(new Color(255, 255, 0)).getHex(), true, "fancymenu.elements.splash.basecolor"));

    protected float baseScale = 1.8F;
    protected String renderText = null;
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

            RenderingUtils.resetShaderColor(graphics);

        }

    }

    public void refresh() {
        this.getBuilder().splashCache.remove(this.getInstanceIdentifier());
        this.renderText = null;
    }

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
                this.renderText = (splashRenderer != null) ? ((IMixinSplashRenderer)splashRenderer).getSplashFancyMenu() : "";
            }
            //TEXT FILE
            if (this.sourceMode == SourceMode.TEXT_FILE) {
                if (this.textFileSupplier != null) {
                    IText text = this.textFileSupplier.get();
                    if (text != null) {
                        List<String> l = text.getTextLines();
                        if (l != null) {
                            if (!l.isEmpty() && ((l.size() > 1) || (l.get(0).replace(" ", "").length() > 0))) {
                                int i = MathUtils.getRandomNumberInRange(0, l.size() - 1);
                                this.renderText = l.get(i);
                            } else {
                                this.renderText = "§cERROR: SPLASH FILE IS EMPTY";
                            }
                        }
                    }
                }
            }
            //DIRECT
            if (this.sourceMode == SourceMode.DIRECT_TEXT) {
                this.renderText = this.source;
            }
        }

        this.getBuilder().splashCache.put(this.getInstanceIdentifier(), this);

    }

    protected void renderSplash(GuiGraphics graphics) {

        if (this.renderText == null) {
            if (isEditor()) {
                this.renderText = "< empty splash element >";
            } else {
                return;
            }
        }

        Component renderTextComponent = buildComponent(this.renderText);

        float splashBaseScale = this.baseScale;
        if (this.bounce) {
            splashBaseScale = splashBaseScale - Mth.abs(Mth.sin((float) (System.currentTimeMillis() % 1000L) / 1000.0F * ((float) Math.PI * 2F)) * 0.1F);
        }
        splashBaseScale = splashBaseScale * 100.0F / (float) (font.width(renderTextComponent) + 32);

        RenderSystem.enableBlend();

        graphics.pose().pushPose();
        graphics.pose().scale(this.scale, this.scale, this.scale);

        graphics.pose().pushPose();
        graphics.pose().translate(((this.getAbsoluteX() + (this.getAbsoluteWidth() / 2F)) / this.scale), this.getAbsoluteY() / this.scale, 0.0F);
        graphics.pose().mulPose(Axis.ZP.rotationDegrees(this.rotation));
        graphics.pose().scale(splashBaseScale, splashBaseScale, splashBaseScale);

        DrawableColor c = this.baseColor.getDrawable();
        int alpha = c.getColor().getAlpha();
        int i = Mth.ceil(this.opacity * 255.0F);
        if (i < alpha) {
            alpha = i;
        }

        graphics.drawString(font, renderTextComponent, -(font.width(renderTextComponent) / 2), 0, RenderingUtils.replaceAlphaInColor(c.getColorInt(), alpha), this.shadow);

        graphics.pose().popPose();
        graphics.pose().popPose();

    }

    public SplashTextElementBuilder getBuilder() {
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

}
