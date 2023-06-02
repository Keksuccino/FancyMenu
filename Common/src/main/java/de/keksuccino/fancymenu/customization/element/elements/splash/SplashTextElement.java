package de.keksuccino.fancymenu.customization.element.elements.splash;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.rendering.DrawableColor;
import de.keksuccino.konkrete.file.FileUtils;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;
import java.util.Objects;

public class SplashTextElement extends AbstractElement {

    //TODO FIXEN: Splash wird bei resize reloaded (isNewMenu in builder fixen??)
    //TODO FIXEN: Splash wird bei resize reloaded (isNewMenu in builder fixen??)
    //TODO FIXEN: Splash wird bei resize reloaded (isNewMenu in builder fixen??)
    //TODO FIXEN: Splash wird bei resize reloaded (isNewMenu in builder fixen??)

    private static final Logger LOGGER = LogManager.getLogger();

    public SourceMode sourceMode = SourceMode.DIRECT_TEXT;
    public String source = "Splash Text";
    public float scale = 1.0F;
    public boolean shadow = true;
    public boolean bounce = true;
    public float rotation = 20.0F;
    public DrawableColor baseColor = DrawableColor.of(255, 255, 0);
    public boolean refreshOnMenuReload = false;

    public Font font = Minecraft.getInstance().font;
    protected float baseScale = 1.8F;
    protected String renderText = null;

    protected String lastSource = null;
    protected SourceMode lastSourceMode = null;
    protected boolean refreshedOnMenuLoad = false;

    public SplashTextElement(@NotNull ElementBuilder<?, ?> builder) {
        super(builder);
    }

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

        if (this.shouldRender()) {

            this.updateSplash();

            this.renderSplash(pose);

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

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
            if (this.sourceMode == SourceMode.VANILLA) {
                this.renderText = Minecraft.getInstance().getSplashManager().getSplash();
            }
            if (this.sourceMode == SourceMode.TEXT_FILE) {
                File f = new File(ScreenCustomization.getAbsoluteGameDirectoryPath(this.source));
                if (f.isFile()) {
                    List<String> l = FileUtils.getFileLines(f);
                    if (!l.isEmpty() && ((l.size() > 1) || (l.get(0).replace(" ", "").length() > 0))) {
                        int i = MathUtils.getRandomNumberInRange(0, l.size()-1);
                        this.renderText = l.get(i);
                    } else {
                        this.renderText = "§cERROR: SPLASH FILE IS EMPTY";
                    }
                } else {
                    this.renderText = "§cERROR: MISSING SPLASH FILE";
                }
            }
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

        String renderTextFinal = PlaceholderParser.replacePlaceholders(this.renderText);
        if (isEditor()) {
            renderTextFinal = StringUtils.convertFormatCodes(this.renderText, "&", "§");
        }

        float splashBaseScale = this.baseScale;
        if (this.bounce) {
            splashBaseScale = splashBaseScale - Mth.abs(Mth.sin((float) (System.currentTimeMillis() % 1000L) / 1000.0F * ((float) Math.PI * 2F)) * 0.1F);
        }
        splashBaseScale = splashBaseScale * 100.0F / (float) (font.width(renderTextFinal) + 32);

        RenderSystem.enableBlend();

        pose.pushPose();
        pose.scale(this.scale, this.scale, this.scale);

        pose.pushPose();
        pose.translate(((this.getX() + (this.getWidth() / 2F)) / this.scale), this.getY() / this.scale, 0.0F);
        pose.mulPose(Axis.ZP.rotationDegrees(this.rotation));
        pose.scale(splashBaseScale, splashBaseScale, splashBaseScale);

        int alpha = this.baseColor.getColor().getAlpha();
        int i = Mth.ceil(this.opacity * 255.0F);
        if (i < alpha) {
            alpha = i;
        }
        int c = FastColor.ARGB32.color(alpha, this.baseColor.getColor().getRed(), this.baseColor.getColor().getGreen(), this.baseColor.getColor().getBlue());

        if (this.shadow) {
            font.drawShadow(pose, renderTextFinal, -((float)font.width(renderTextFinal) / 2F), 0F, c);
        } else {
            font.draw(pose, renderTextFinal, -((float)font.width(renderTextFinal) / 2F), 0F, c);
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
