package de.keksuccino.fancymenu.customization.element.elements.glsl;

import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.glsl.GlslShaderRuntime;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GlslEditorElement extends AbstractEditorElement<GlslEditorElement, GlslElement> {

    private static final List<GlslShaderRuntime.CompileMode> COMPILE_MODES = List.of(
            GlslShaderRuntime.CompileMode.AUTO,
            GlslShaderRuntime.CompileMode.DIRECT,
            GlslShaderRuntime.CompileMode.SHADERTOY
    );
    private static final List<GlslShaderRuntime.ChannelInput> CHANNEL_INPUTS = List.of(GlslShaderRuntime.ChannelInput.values());

    public GlslEditorElement(@NotNull GlslElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
    }

    @Override
    public void init() {

        super.init();

        this.element.shaderSource.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setTooltipSupplier((menu, entry) -> tooltip("fancymenu.elements.glsl.shader_source.desc"))
                .setIcon(MaterialIcons.TEXT_FIELDS);

        this.element.inlineShaderSource.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setTooltipSupplier((menu, entry) -> tooltip("fancymenu.elements.glsl.inline_shader_source.desc"))
                .setIcon(MaterialIcons.CODE);

        this.element.preferInlineShaderSource.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setTooltipSupplier((menu, entry) -> tooltip("fancymenu.elements.glsl.prefer_inline_shader_source.desc"))
                .setIcon(MaterialIcons.SWAP_HORIZ);

        this.rightClickMenu.addSeparatorEntry("separator_before_buffer_sources");

        this.element.bufferAInlineSource.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setTooltipSupplier((menu, entry) -> tooltip("fancymenu.elements.glsl.buffer_a_inline_source.desc"))
                .setIcon(MaterialIcons.CODE);

        this.element.bufferBInlineSource.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setTooltipSupplier((menu, entry) -> tooltip("fancymenu.elements.glsl.buffer_b_inline_source.desc"))
                .setIcon(MaterialIcons.CODE);

        this.element.bufferCInlineSource.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setTooltipSupplier((menu, entry) -> tooltip("fancymenu.elements.glsl.buffer_c_inline_source.desc"))
                .setIcon(MaterialIcons.CODE);

        this.element.bufferDInlineSource.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setTooltipSupplier((menu, entry) -> tooltip("fancymenu.elements.glsl.buffer_d_inline_source.desc"))
                .setIcon(MaterialIcons.CODE);

        this.rightClickMenu.addSeparatorEntry("separator_before_channel_selection");

        this.element.iChannel0Source.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setTooltipSupplier((menu, entry) -> tooltip("fancymenu.elements.glsl.ichannel0_source.desc"))
                .setIcon(MaterialIcons.IMAGE);

        this.element.iChannel1Source.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setTooltipSupplier((menu, entry) -> tooltip("fancymenu.elements.glsl.ichannel1_source.desc"))
                .setIcon(MaterialIcons.IMAGE);

        this.element.iChannel2Source.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setTooltipSupplier((menu, entry) -> tooltip("fancymenu.elements.glsl.ichannel2_source.desc"))
                .setIcon(MaterialIcons.IMAGE);

        this.element.iChannel3Source.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setTooltipSupplier((menu, entry) -> tooltip("fancymenu.elements.glsl.ichannel3_source.desc"))
                .setIcon(MaterialIcons.IMAGE);

        this.rightClickMenu.addSeparatorEntry("separator_before_channel_routing");

        this.addChannelInputCycleEntry("image_ichannel0_input", this.element.imageIChannel0Input, "fancymenu.elements.glsl.image_ichannel0_input");
        this.addChannelInputCycleEntry("image_ichannel1_input", this.element.imageIChannel1Input, "fancymenu.elements.glsl.image_ichannel1_input");
        this.addChannelInputCycleEntry("image_ichannel2_input", this.element.imageIChannel2Input, "fancymenu.elements.glsl.image_ichannel2_input");
        this.addChannelInputCycleEntry("image_ichannel3_input", this.element.imageIChannel3Input, "fancymenu.elements.glsl.image_ichannel3_input");

        this.rightClickMenu.addSeparatorEntry("separator_before_buffer_a_routing");

        this.addChannelInputCycleEntry("buffer_a_ichannel0_input", this.element.bufferAIChannel0Input, "fancymenu.elements.glsl.buffer_a_ichannel0_input");
        this.addChannelInputCycleEntry("buffer_a_ichannel1_input", this.element.bufferAIChannel1Input, "fancymenu.elements.glsl.buffer_a_ichannel1_input");
        this.addChannelInputCycleEntry("buffer_a_ichannel2_input", this.element.bufferAIChannel2Input, "fancymenu.elements.glsl.buffer_a_ichannel2_input");
        this.addChannelInputCycleEntry("buffer_a_ichannel3_input", this.element.bufferAIChannel3Input, "fancymenu.elements.glsl.buffer_a_ichannel3_input");

        this.rightClickMenu.addSeparatorEntry("separator_before_buffer_b_routing");

        this.addChannelInputCycleEntry("buffer_b_ichannel0_input", this.element.bufferBIChannel0Input, "fancymenu.elements.glsl.buffer_b_ichannel0_input");
        this.addChannelInputCycleEntry("buffer_b_ichannel1_input", this.element.bufferBIChannel1Input, "fancymenu.elements.glsl.buffer_b_ichannel1_input");
        this.addChannelInputCycleEntry("buffer_b_ichannel2_input", this.element.bufferBIChannel2Input, "fancymenu.elements.glsl.buffer_b_ichannel2_input");
        this.addChannelInputCycleEntry("buffer_b_ichannel3_input", this.element.bufferBIChannel3Input, "fancymenu.elements.glsl.buffer_b_ichannel3_input");

        this.rightClickMenu.addSeparatorEntry("separator_before_buffer_c_routing");

        this.addChannelInputCycleEntry("buffer_c_ichannel0_input", this.element.bufferCIChannel0Input, "fancymenu.elements.glsl.buffer_c_ichannel0_input");
        this.addChannelInputCycleEntry("buffer_c_ichannel1_input", this.element.bufferCIChannel1Input, "fancymenu.elements.glsl.buffer_c_ichannel1_input");
        this.addChannelInputCycleEntry("buffer_c_ichannel2_input", this.element.bufferCIChannel2Input, "fancymenu.elements.glsl.buffer_c_ichannel2_input");
        this.addChannelInputCycleEntry("buffer_c_ichannel3_input", this.element.bufferCIChannel3Input, "fancymenu.elements.glsl.buffer_c_ichannel3_input");

        this.rightClickMenu.addSeparatorEntry("separator_before_buffer_d_routing");

        this.addChannelInputCycleEntry("buffer_d_ichannel0_input", this.element.bufferDIChannel0Input, "fancymenu.elements.glsl.buffer_d_ichannel0_input");
        this.addChannelInputCycleEntry("buffer_d_ichannel1_input", this.element.bufferDIChannel1Input, "fancymenu.elements.glsl.buffer_d_ichannel1_input");
        this.addChannelInputCycleEntry("buffer_d_ichannel2_input", this.element.bufferDIChannel2Input, "fancymenu.elements.glsl.buffer_d_ichannel2_input");
        this.addChannelInputCycleEntry("buffer_d_ichannel3_input", this.element.bufferDIChannel3Input, "fancymenu.elements.glsl.buffer_d_ichannel3_input");

        this.rightClickMenu.addSeparatorEntry("separator_after_channel_routing");

        this.addCycleContextMenuEntryTo(this.rightClickMenu,
                        "compile_mode",
                        COMPILE_MODES,
                        GlslEditorElement.class,
                        editorElement -> editorElement.element.getCompileMode(),
                        (editorElement, mode) -> editorElement.element.setCompileMode(mode),
                        (menu, entry, mode) -> Component.translatable("fancymenu.elements.glsl.compile_mode", warningValue(getCompileModeDisplay(mode))))
                .setTooltipSupplier((menu, entry) -> tooltip("fancymenu.elements.glsl.compile_mode.desc"))
                .setIcon(MaterialIcons.TUNE);

        this.element.forceShadertoyCompatibility.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setTooltipSupplier((menu, entry) -> tooltip("fancymenu.elements.glsl.force_shadertoy_compatibility.desc"))
                .setIcon(MaterialIcons.CODE);

        this.element.freezeTime.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setTooltipSupplier((menu, entry) -> tooltip("fancymenu.elements.glsl.freeze_time.desc"))
                .setIcon(MaterialIcons.PAUSE);

        this.element.timeScale.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setTooltipSupplier((menu, entry) -> tooltip("fancymenu.elements.glsl.time_scale.desc"))
                .setIcon(MaterialIcons.SPEED);

        this.rightClickMenu.addSeparatorEntry("separator_before_render_settings");

        this.element.enableBlending.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setTooltipSupplier((menu, entry) -> tooltip("fancymenu.elements.glsl.enable_blending.desc"))
                .setIcon(MaterialIcons.BLUR_ON);

        this.element.useInput.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setTooltipSupplier((menu, entry) -> tooltip("fancymenu.elements.glsl.use_input.desc"))
                .setIcon(MaterialIcons.MOUSE);

        this.element.opacityMultiplier.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setTooltipSupplier((menu, entry) -> tooltip("fancymenu.elements.glsl.opacity_multiplier.desc"))
                .setIcon(MaterialIcons.PALETTE);

        this.element.showCompileErrors.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setTooltipSupplier((menu, entry) -> tooltip("fancymenu.elements.glsl.show_compile_errors.desc"))
                .setIcon(MaterialIcons.ERROR);

    }

    @NotNull
    private static Component getCompileModeDisplay(@NotNull GlslShaderRuntime.CompileMode mode) {
        return switch (mode) {
            case AUTO -> Component.translatable("fancymenu.elements.glsl.compile_mode.auto");
            case DIRECT -> Component.translatable("fancymenu.elements.glsl.compile_mode.direct");
            case SHADERTOY -> Component.translatable("fancymenu.elements.glsl.compile_mode.shadertoy");
        };
    }

    @NotNull
    private static UITooltip tooltip(@NotNull String key) {
        return UITooltip.of(LocalizationUtils.splitLocalizedLines(key));
    }

    private void addChannelInputCycleEntry(@NotNull String cycleId,
                                           @NotNull Property.StringProperty property,
                                           @NotNull String localizationKey) {
        this.addCycleContextMenuEntryTo(this.rightClickMenu,
                        cycleId,
                        CHANNEL_INPUTS,
                        GlslEditorElement.class,
                        editorElement -> editorElement.element.getChannelInput(property, GlslShaderRuntime.ChannelInput.NONE),
                        (editorElement, input) -> editorElement.element.setChannelInput(property, input),
                        (menu, entry, input) -> Component.translatable(localizationKey, warningValue(getChannelInputDisplay(input))))
                .setTooltipSupplier((menu, entry) -> tooltip(localizationKey + ".desc"))
                .setIcon(MaterialIcons.ROUTE);
    }

    @NotNull
    private static Component getChannelInputDisplay(@NotNull GlslShaderRuntime.ChannelInput input) {
        return Component.translatable("fancymenu.glsl.channel_input." + input.serializedName());
    }

    @NotNull
    private static Component warningValue(@NotNull Component value) {
        return value.copy().withStyle(Style.EMPTY.withColor(UIBase.getUITheme().warning_color.getColorInt()));
    }

}
