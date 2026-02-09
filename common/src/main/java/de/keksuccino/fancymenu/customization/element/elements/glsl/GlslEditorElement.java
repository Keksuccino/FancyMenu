package de.keksuccino.fancymenu.customization.element.elements.glsl;

import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.glsl.GlslShaderRuntime;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GlslEditorElement extends AbstractEditorElement<GlslEditorElement, GlslElement> {

    private static final List<GlslShaderRuntime.CompileMode> COMPILE_MODES = List.of(
            GlslShaderRuntime.CompileMode.AUTO,
            GlslShaderRuntime.CompileMode.DIRECT,
            GlslShaderRuntime.CompileMode.SHADERTOY
    );

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

        this.rightClickMenu.addSeparatorEntry("separator_after_source_selection");

        this.addCycleContextMenuEntryTo(this.rightClickMenu,
                        "compile_mode",
                        COMPILE_MODES,
                        GlslEditorElement.class,
                        editorElement -> editorElement.element.getCompileMode(),
                        (editorElement, mode) -> editorElement.element.setCompileMode(mode),
                        (menu, entry, mode) -> Component.translatable("fancymenu.elements.glsl.compile_mode", getCompileModeDisplay(mode)))
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

}
