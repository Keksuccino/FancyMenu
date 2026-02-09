package de.keksuccino.fancymenu.customization.element.elements.glsl;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.util.file.FileFilter;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.glsl.GlslShaderRuntime;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.text.IText;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

public class GlslElement extends AbstractElement {

    private static final FileFilter SHADER_FILE_FILTER = file -> {
        String name = file.getName().toLowerCase(Locale.ROOT);
        return name.endsWith(".txt");
    };

    public final Property<ResourceSupplier<IText>> shaderSource = putProperty(Property.resourceSupplierProperty(IText.class, "shader_source", null, "fancymenu.elements.glsl.shader_source", true, true, true, SHADER_FILE_FILTER));
    public final Property.StringProperty inlineShaderSource = putProperty(Property.stringProperty("inline_shader_source", "", true, false, "fancymenu.elements.glsl.inline_shader_source"));
    public final Property.BooleanProperty preferInlineShaderSource = putProperty(Property.booleanProperty("prefer_inline_shader_source", false, "fancymenu.elements.glsl.prefer_inline_shader_source"));
    public final Property.StringProperty compileMode = putProperty(Property.stringProperty("compile_mode", "auto", false, false, "fancymenu.elements.glsl.compile_mode"));
    public final Property.BooleanProperty forceShadertoyCompatibility = putProperty(Property.booleanProperty("force_shadertoy_compatibility", true, "fancymenu.elements.glsl.force_shadertoy_compatibility"));
    public final Property.BooleanProperty freezeTime = putProperty(Property.booleanProperty("freeze_time", false, "fancymenu.elements.glsl.freeze_time"));
    public final Property.FloatProperty timeScale = putProperty(Property.floatProperty("time_scale", 1.0F, "fancymenu.elements.glsl.time_scale"));
    public final Property.BooleanProperty enableBlending = putProperty(Property.booleanProperty("enable_blending", true, "fancymenu.elements.glsl.enable_blending"));
    public final Property.BooleanProperty useInput = putProperty(Property.booleanProperty("use_input", true, "fancymenu.elements.glsl.use_input"));
    public final Property.FloatProperty opacityMultiplier = putProperty(Property.floatProperty("opacity_multiplier", 1.0F, "fancymenu.elements.glsl.opacity_multiplier"));
    public final Property.BooleanProperty showCompileErrors = putProperty(Property.booleanProperty("show_compile_errors", true, "fancymenu.elements.glsl.show_compile_errors"));
    public final Property<ResourceSupplier<ITexture>> iChannel0Source = putProperty(Property.resourceSupplierProperty(ITexture.class, "ichannel_0_source", null, "fancymenu.elements.glsl.ichannel0_source", true, true, true, FileFilter.IMAGE_FILE_FILTER));
    public final Property<ResourceSupplier<ITexture>> iChannel1Source = putProperty(Property.resourceSupplierProperty(ITexture.class, "ichannel_1_source", null, "fancymenu.elements.glsl.ichannel1_source", true, true, true, FileFilter.IMAGE_FILE_FILTER));
    public final Property<ResourceSupplier<ITexture>> iChannel2Source = putProperty(Property.resourceSupplierProperty(ITexture.class, "ichannel_2_source", null, "fancymenu.elements.glsl.ichannel2_source", true, true, true, FileFilter.IMAGE_FILE_FILTER));
    public final Property<ResourceSupplier<ITexture>> iChannel3Source = putProperty(Property.resourceSupplierProperty(ITexture.class, "ichannel_3_source", null, "fancymenu.elements.glsl.ichannel3_source", true, true, true, FileFilter.IMAGE_FILE_FILTER));

    protected final GlslShaderRuntime shaderRuntime = new GlslShaderRuntime();

    public GlslElement(@NotNull ElementBuilder<?, ?> builder) {
        super(builder);
        this.allowDepthTestManipulation = true;
        this.setSupportsRotation(false);
        this.setSupportsTilting(false);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (!this.shouldRender()) {
            return;
        }

        int x = this.getAbsoluteX();
        int y = this.getAbsoluteY();
        int w = this.getAbsoluteWidth();
        int h = this.getAbsoluteHeight();

        float resolvedOpacity = Mth.clamp(this.opacity * Math.max(0.0F, this.opacityMultiplier.getFloat()), 0.0F, 1.0F);

        boolean rendered = this.shaderRuntime.render(
                graphics,
                x,
                y,
                w,
                h,
                partial,
                resolveShaderSource(),
                new GlslShaderRuntime.RenderSettings(
                        this.getCompileMode(),
                        this.forceShadertoyCompatibility.getBoolean(),
                        Math.max(0.0F, this.timeScale.getFloat()),
                        this.freezeTime.getBoolean(),
                        this.enableBlending.getBoolean(),
                        this.useInput.getBoolean(),
                        resolvedOpacity,
                        this.iChannel0Source.get(),
                        this.iChannel1Source.get(),
                        this.iChannel2Source.get(),
                        this.iChannel3Source.get()
                )
        );

        if (!this.showCompileErrors.getBoolean()) {
            return;
        }

        if (!rendered && this.shaderRuntime.isSourceMissing()) {
            this.renderErrorOverlay(graphics, x, y, w, h, Component.translatable("fancymenu.elements.glsl.error.no_source").getString());
            return;
        }

        String compileError = this.shaderRuntime.getLastCompileError();
        if (compileError != null && !compileError.isBlank()) {
            this.renderErrorOverlay(graphics, x, y, w, h, compileError);
        }

    }

    @Override
    public void onBeforeResizeScreen() {
        super.onBeforeResizeScreen();
        this.shaderRuntime.close();
    }

    @Override
    public void onDestroyElement() {
        super.onDestroyElement();
        this.shaderRuntime.close();
    }

    @Override
    public void onCloseScreen(@Nullable net.minecraft.client.gui.screens.Screen closedScreen, @Nullable net.minecraft.client.gui.screens.Screen newScreen) {
        super.onCloseScreen(closedScreen, newScreen);
        this.shaderRuntime.close();
    }

    @Nullable
    public String resolveShaderSource() {
        String inlineSource = this.inlineShaderSource.get();
        String fileSource = readSourceFromSupplier(this.shaderSource.get());

        if (this.preferInlineShaderSource.getBoolean()) {
            if (inlineSource != null && !inlineSource.isBlank()) {
                return inlineSource;
            }
            return fileSource;
        }

        if (fileSource != null && !fileSource.isBlank()) {
            return fileSource;
        }

        if (inlineSource != null && !inlineSource.isBlank()) {
            return inlineSource;
        }

        return null;
    }

    @Nullable
    private static String readSourceFromSupplier(@Nullable ResourceSupplier<IText> supplier) {
        if (supplier == null) {
            return null;
        }
        IText text = supplier.get();
        if (text == null || !text.isReady()) {
            return null;
        }
        List<String> lines = text.getTextLines();
        if (lines == null) {
            return null;
        }
        return String.join("\n", lines);
    }

    private void renderErrorOverlay(@NotNull GuiGraphics graphics, int x, int y, int width, int height, @NotNull String message) {

        if (width < 16 || height < 16) {
            return;
        }

        var font = Minecraft.getInstance().font;
        int maxWidth = Math.max(20, width - 8);

        String[] split = message.replace("\r", "").split("\n");
        int maxLines = Math.min(split.length, Math.max(1, (height - 8) / (font.lineHeight + 1)));

        int lineHeight = font.lineHeight + 1;
        int boxWidth = 0;
        String[] lines = new String[maxLines];
        for (int i = 0; i < maxLines; i++) {
            String line = split[i];
            if (font.width(line) > maxWidth) {
                line = font.plainSubstrByWidth(line, Math.max(1, maxWidth - font.width(".."))) + "..";
            }
            lines[i] = line;
            boxWidth = Math.max(boxWidth, font.width(line));
        }

        int boxHeight = (maxLines * lineHeight) + 6;
        int boxX = x + 2;
        int boxY = y + 2;

        graphics.fill(boxX, boxY, boxX + 4 + boxWidth, boxY + boxHeight, 0xCC000000);

        int lineY = boxY + 3;
        for (String line : lines) {
            graphics.drawString(font, line, boxX + 2, lineY, 0xFFFF6666, false);
            lineY += lineHeight;
        }
    }

    @NotNull
    public GlslShaderRuntime.CompileMode getCompileMode() {
        String raw = this.compileMode.get();
        if (raw != null) {
            try {
                return GlslShaderRuntime.CompileMode.valueOf(raw.toUpperCase(Locale.ROOT));
            } catch (Exception ignored) {
            }
        }
        return GlslShaderRuntime.CompileMode.AUTO;
    }

    public void setCompileMode(@NotNull GlslShaderRuntime.CompileMode mode) {
        this.compileMode.set(mode.name().toLowerCase(Locale.ROOT));
    }

}
