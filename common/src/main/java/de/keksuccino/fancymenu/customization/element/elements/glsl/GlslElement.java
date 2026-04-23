package de.keksuccino.fancymenu.customization.element.elements.glsl;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.util.file.FileFilter;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.glsl.GlslShaderRuntime;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class GlslElement extends AbstractElement {

    public final Property.StringProperty inlineShaderSource = putProperty(Property.stringProperty("inline_shader_source", "", true, false, "fancymenu.elements.glsl.inline_shader_source"));
    public final Property.StringProperty bufferAInlineSource = putProperty(Property.stringProperty("buffer_a_inline_source", "", true, false, "fancymenu.elements.glsl.buffer_a_inline_source"));
    public final Property.StringProperty bufferBInlineSource = putProperty(Property.stringProperty("buffer_b_inline_source", "", true, false, "fancymenu.elements.glsl.buffer_b_inline_source"));
    public final Property.StringProperty bufferCInlineSource = putProperty(Property.stringProperty("buffer_c_inline_source", "", true, false, "fancymenu.elements.glsl.buffer_c_inline_source"));
    public final Property.StringProperty bufferDInlineSource = putProperty(Property.stringProperty("buffer_d_inline_source", "", true, false, "fancymenu.elements.glsl.buffer_d_inline_source"));

    public final Property.StringProperty imageIChannel0Input = putProperty(Property.stringProperty("image_ichannel0_input", GlslShaderRuntime.ChannelInput.NONE.serializedName(), false, false, "fancymenu.elements.glsl.image_ichannel0_input"));
    public final Property.StringProperty imageIChannel1Input = putProperty(Property.stringProperty("image_ichannel1_input", GlslShaderRuntime.ChannelInput.NONE.serializedName(), false, false, "fancymenu.elements.glsl.image_ichannel1_input"));
    public final Property.StringProperty imageIChannel2Input = putProperty(Property.stringProperty("image_ichannel2_input", GlslShaderRuntime.ChannelInput.NONE.serializedName(), false, false, "fancymenu.elements.glsl.image_ichannel2_input"));
    public final Property.StringProperty imageIChannel3Input = putProperty(Property.stringProperty("image_ichannel3_input", GlslShaderRuntime.ChannelInput.NONE.serializedName(), false, false, "fancymenu.elements.glsl.image_ichannel3_input"));
    public final Property.StringProperty bufferAIChannel0Input = putProperty(Property.stringProperty("buffer_a_ichannel0_input", GlslShaderRuntime.ChannelInput.NONE.serializedName(), false, false, "fancymenu.elements.glsl.buffer_a_ichannel0_input"));
    public final Property.StringProperty bufferAIChannel1Input = putProperty(Property.stringProperty("buffer_a_ichannel1_input", GlslShaderRuntime.ChannelInput.NONE.serializedName(), false, false, "fancymenu.elements.glsl.buffer_a_ichannel1_input"));
    public final Property.StringProperty bufferAIChannel2Input = putProperty(Property.stringProperty("buffer_a_ichannel2_input", GlslShaderRuntime.ChannelInput.NONE.serializedName(), false, false, "fancymenu.elements.glsl.buffer_a_ichannel2_input"));
    public final Property.StringProperty bufferAIChannel3Input = putProperty(Property.stringProperty("buffer_a_ichannel3_input", GlslShaderRuntime.ChannelInput.NONE.serializedName(), false, false, "fancymenu.elements.glsl.buffer_a_ichannel3_input"));
    public final Property.StringProperty bufferBIChannel0Input = putProperty(Property.stringProperty("buffer_b_ichannel0_input", GlslShaderRuntime.ChannelInput.NONE.serializedName(), false, false, "fancymenu.elements.glsl.buffer_b_ichannel0_input"));
    public final Property.StringProperty bufferBIChannel1Input = putProperty(Property.stringProperty("buffer_b_ichannel1_input", GlslShaderRuntime.ChannelInput.NONE.serializedName(), false, false, "fancymenu.elements.glsl.buffer_b_ichannel1_input"));
    public final Property.StringProperty bufferBIChannel2Input = putProperty(Property.stringProperty("buffer_b_ichannel2_input", GlslShaderRuntime.ChannelInput.NONE.serializedName(), false, false, "fancymenu.elements.glsl.buffer_b_ichannel2_input"));
    public final Property.StringProperty bufferBIChannel3Input = putProperty(Property.stringProperty("buffer_b_ichannel3_input", GlslShaderRuntime.ChannelInput.NONE.serializedName(), false, false, "fancymenu.elements.glsl.buffer_b_ichannel3_input"));
    public final Property.StringProperty bufferCIChannel0Input = putProperty(Property.stringProperty("buffer_c_ichannel0_input", GlslShaderRuntime.ChannelInput.NONE.serializedName(), false, false, "fancymenu.elements.glsl.buffer_c_ichannel0_input"));
    public final Property.StringProperty bufferCIChannel1Input = putProperty(Property.stringProperty("buffer_c_ichannel1_input", GlslShaderRuntime.ChannelInput.NONE.serializedName(), false, false, "fancymenu.elements.glsl.buffer_c_ichannel1_input"));
    public final Property.StringProperty bufferCIChannel2Input = putProperty(Property.stringProperty("buffer_c_ichannel2_input", GlslShaderRuntime.ChannelInput.NONE.serializedName(), false, false, "fancymenu.elements.glsl.buffer_c_ichannel2_input"));
    public final Property.StringProperty bufferCIChannel3Input = putProperty(Property.stringProperty("buffer_c_ichannel3_input", GlslShaderRuntime.ChannelInput.NONE.serializedName(), false, false, "fancymenu.elements.glsl.buffer_c_ichannel3_input"));
    public final Property.StringProperty bufferDIChannel0Input = putProperty(Property.stringProperty("buffer_d_ichannel0_input", GlslShaderRuntime.ChannelInput.NONE.serializedName(), false, false, "fancymenu.elements.glsl.buffer_d_ichannel0_input"));
    public final Property.StringProperty bufferDIChannel1Input = putProperty(Property.stringProperty("buffer_d_ichannel1_input", GlslShaderRuntime.ChannelInput.NONE.serializedName(), false, false, "fancymenu.elements.glsl.buffer_d_ichannel1_input"));
    public final Property.StringProperty bufferDIChannel2Input = putProperty(Property.stringProperty("buffer_d_ichannel2_input", GlslShaderRuntime.ChannelInput.NONE.serializedName(), false, false, "fancymenu.elements.glsl.buffer_d_ichannel2_input"));
    public final Property.StringProperty bufferDIChannel3Input = putProperty(Property.stringProperty("buffer_d_ichannel3_input", GlslShaderRuntime.ChannelInput.NONE.serializedName(), false, false, "fancymenu.elements.glsl.buffer_d_ichannel3_input"));

    public final Property.StringProperty compileMode = putProperty(Property.stringProperty("compile_mode", "auto", false, false, "fancymenu.elements.glsl.compile_mode"));
    public final Property.BooleanProperty forceShadertoyCompatibility = putProperty(Property.booleanProperty("force_shadertoy_compatibility", true, "fancymenu.elements.glsl.force_shadertoy_compatibility"));
    public final Property.BooleanProperty freezeTime = putProperty(Property.booleanProperty("freeze_time", false, "fancymenu.elements.glsl.freeze_time"));
    public final Property.FloatProperty timeScale = putProperty(Property.floatProperty("time_scale", 1.0F, "fancymenu.elements.glsl.time_scale"));
    public final Property.BooleanProperty enableBlending = putProperty(Property.booleanProperty("enable_blending", true, "fancymenu.elements.glsl.enable_blending"));
    public final Property.BooleanProperty useInput = putProperty(Property.booleanProperty("use_input", true, "fancymenu.elements.glsl.use_input"));
    public final Property.BooleanProperty mousePositionRequiresHold = putProperty(Property.booleanProperty("mouse_position_requires_hold", false, "fancymenu.elements.glsl.mouse_position_requires_hold"));
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
                        this.mousePositionRequiresHold.getBoolean(),
                        resolvedOpacity,
                        this.iChannel0Source.get(),
                        this.iChannel1Source.get(),
                        this.iChannel2Source.get(),
                        this.iChannel3Source.get(),
                        this.bufferAInlineSource.get(),
                        this.bufferBInlineSource.get(),
                        this.bufferCInlineSource.get(),
                        this.bufferDInlineSource.get(),
                        this.resolveRouting(this.imageIChannel0Input, this.imageIChannel1Input, this.imageIChannel2Input, this.imageIChannel3Input),
                        this.resolveRouting(this.bufferAIChannel0Input, this.bufferAIChannel1Input, this.bufferAIChannel2Input, this.bufferAIChannel3Input),
                        this.resolveRouting(this.bufferBIChannel0Input, this.bufferBIChannel1Input, this.bufferBIChannel2Input, this.bufferBIChannel3Input),
                        this.resolveRouting(this.bufferCIChannel0Input, this.bufferCIChannel1Input, this.bufferCIChannel2Input, this.bufferCIChannel3Input),
                        this.resolveRouting(this.bufferDIChannel0Input, this.bufferDIChannel1Input, this.bufferDIChannel2Input, this.bufferDIChannel3Input)
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

        if (inlineSource != null && !inlineSource.isBlank()) {
            return inlineSource;
        }

        return null;
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

    @NotNull
    public GlslShaderRuntime.ChannelInput getChannelInput(@NotNull Property.StringProperty property,
                                                          @NotNull GlslShaderRuntime.ChannelInput fallback) {
        return GlslShaderRuntime.ChannelInput.fromSerialized(property.get(), fallback);
    }

    public void setChannelInput(@NotNull Property.StringProperty property,
                                @NotNull GlslShaderRuntime.ChannelInput input) {
        property.set(input.serializedName());
    }

    @NotNull
    private GlslShaderRuntime.ChannelRouting resolveRouting(@NotNull Property.StringProperty c0,
                                                            @NotNull Property.StringProperty c1,
                                                            @NotNull Property.StringProperty c2,
                                                            @NotNull Property.StringProperty c3) {
        return new GlslShaderRuntime.ChannelRouting(
                this.getChannelInput(c0, GlslShaderRuntime.ChannelInput.NONE),
                this.getChannelInput(c1, GlslShaderRuntime.ChannelInput.NONE),
                this.getChannelInput(c2, GlslShaderRuntime.ChannelInput.NONE),
                this.getChannelInput(c3, GlslShaderRuntime.ChannelInput.NONE)
        );
    }

}
