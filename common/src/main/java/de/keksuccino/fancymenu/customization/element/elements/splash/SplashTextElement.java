package de.keksuccino.fancymenu.customization.element.elements.splash;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.text.IText;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.awt.*;
import java.util.List;
import java.util.Objects;

public class SplashTextElement extends AbstractElement {

    private static final Logger LOGGER = LogManager.getLogger();

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

            this.renderSplash(graphics.pose());

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
                this.renderText = Minecraft.getInstance().getSplashManager().getSplash();
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

    protected void renderSplash(PoseStack pose) {

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

        pose.pushPose();
        pose.scale(this.scale, this.scale, this.scale);

        pose.pushPose();
        pose.translate(((this.getAbsoluteX() + (this.getAbsoluteWidth() / 2F)) / this.scale), this.getAbsoluteY() / this.scale, 0.0F);
        pose.mulPose(Vector3f.ZP.rotationDegrees(this.rotation));
        pose.scale(splashBaseScale, splashBaseScale, splashBaseScale);

        int alpha = this.baseColor.getColor().getAlpha();
        int i = Mth.ceil(this.opacity * 255.0F);
        if (i < alpha) {
            alpha = i;
        }

        if (this.shadow) {
            font.drawShadow(pose, renderTextComponent, -((float)font.width(renderTextComponent) / 2F), 0F, RenderingUtils.replaceAlphaInColor(this.baseColor.getColorInt(), alpha));
        } else {
            font.draw(pose, renderTextComponent, -((float)font.width(renderTextComponent) / 2F), 0F, RenderingUtils.replaceAlphaInColor(this.baseColor.getColorInt(), alpha));
        }

        pose.popPose();
        pose.popPose();

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

}
