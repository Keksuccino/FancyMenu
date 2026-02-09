package de.keksuccino.fancymenu.customization.element.elements.glsl;

import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.rendering.glsl.GlslShaderRuntime;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
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
                .setIcon(MaterialIcons.TEXT_FIELDS);

        this.element.inlineShaderSource.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setIcon(MaterialIcons.CODE);

        this.element.preferInlineShaderSource.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setIcon(MaterialIcons.SWAP_HORIZ);

        this.rightClickMenu.addSeparatorEntry("separator_after_source_selection");

        this.addCycleContextMenuEntryTo(this.rightClickMenu,
                        "compile_mode",
                        COMPILE_MODES,
                        GlslEditorElement.class,
                        editorElement -> editorElement.element.getCompileMode(),
                        (editorElement, mode) -> editorElement.element.setCompileMode(mode),
                        (menu, entry, mode) -> Component.translatable("fancymenu.elements.glsl.compile_mode", getCompileModeDisplay(mode)))
                .setIcon(MaterialIcons.TUNE);

        this.element.forceShadertoyCompatibility.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setIcon(MaterialIcons.CODE);

        this.element.freezeTime.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setIcon(MaterialIcons.PAUSE);

        this.element.timeScale.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setIcon(MaterialIcons.SPEED);

        this.rightClickMenu.addSeparatorEntry("separator_before_render_settings");

        this.element.enableBlending.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setIcon(MaterialIcons.BLUR_ON);

        this.element.useInput.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setIcon(MaterialIcons.MOUSE);

        this.element.opacityMultiplier.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setIcon(MaterialIcons.PALETTE);

        this.element.showCompileErrors.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
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

}
