
package de.keksuccino.fancymenu.customization.element.elements.splash;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.file.FileFilter;
import de.keksuccino.fancymenu.util.input.TextValidators;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.ListUtils;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class SplashTextEditorElement extends AbstractEditorElement {

    public SplashTextEditorElement(@NotNull AbstractElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
    }

    @Override
    public void init() {

        super.init();

        this.addGenericCycleContextMenuEntryTo(this.rightClickMenu, "set_mode",
                ListUtils.build(SplashTextElement.SourceMode.VANILLA, SplashTextElement.SourceMode.DIRECT_TEXT, SplashTextElement.SourceMode.TEXT_FILE),
                consumes -> (consumes instanceof SplashTextEditorElement),
                consumes -> ((SplashTextElement)consumes.element).sourceMode,
                (element1, sourceMode) -> {
                    ((SplashTextElement)element1.element).sourceMode = sourceMode;
                    ((SplashTextElement)element1.element).source = null;
                },
                (menu, entry, switcherValue) -> {
                    if (switcherValue == SplashTextElement.SourceMode.VANILLA) {
                        return Component.translatable("fancymenu.elements.splash.source_mode.vanilla");
                    }
                    if (switcherValue == SplashTextElement.SourceMode.DIRECT_TEXT) {
                        return Component.translatable("fancymenu.elements.splash.source_mode.direct");
                    }
                    return Component.translatable("fancymenu.elements.splash.source_mode.text_file");
                });

        this.addFileChooserContextMenuEntryTo(this.rightClickMenu, "set_source_file",
                        consumes -> (consumes instanceof SplashTextEditorElement),
                        null,
                        consumes -> ((SplashTextElement)consumes.element).source,
                        (element1, s) -> {
                            ((SplashTextElement)element1.element).source = s;
                            ((SplashTextElement)element1.element).updateSplash();
                        },
                        Component.translatable("fancymenu.elements.splash.source_mode.text_file.set_source"),
                        false, FileFilter.TXT_FILE_FILTER)
                .setIsVisibleSupplier((menu, entry) -> ((SplashTextElement)this.element).sourceMode == SplashTextElement.SourceMode.TEXT_FILE);

        this.addGenericStringInputContextMenuEntryTo(this.rightClickMenu, "input_direct",
                        consumes -> (consumes instanceof SplashTextEditorElement),
                        consumes -> ((SplashTextElement)consumes.element).source,
                        (element1, s) -> {
                            ((SplashTextElement)element1.element).source = s;
                            ((SplashTextElement)element1.element).updateSplash();
                        },
                        null, false, true, Component.translatable("fancymenu.elements.splash.source_mode.direct.set_source"),
                        false, null, TextValidators.NO_EMPTY_STRING_TEXT_VALIDATOR, null)
                .setIsVisibleSupplier((menu, entry) -> ((SplashTextElement)this.element).sourceMode == SplashTextElement.SourceMode.DIRECT_TEXT);

        this.rightClickMenu.addSeparatorEntry("splash_separator_1");

        this.addGenericFloatInputContextMenuEntryTo(this.rightClickMenu, "set_scale",
                        consumes -> (consumes instanceof SplashTextEditorElement),
                        consumes -> ((SplashTextElement)consumes.element).scale,
                        (element, scale) -> {
                            ((SplashTextElement)element.element).scale = Math.max(0.2F, scale);
                            ((SplashTextElement)element.element).updateSplash();
                        },
                        Component.translatable("fancymenu.elements.splash.set_scale"),
                        true, 1.0F, null, null)
                .setStackable(true);

        this.addGenericFloatInputContextMenuEntryTo(this.rightClickMenu, "set_rotation",
                        consumes -> (consumes instanceof SplashTextEditorElement),
                        consumes -> ((SplashTextElement)consumes.element).rotation,
                        (element, rot) -> {
                            ((SplashTextElement)element.element).rotation = rot;
                            ((SplashTextElement)element.element).updateSplash();
                        },
                        Component.translatable("fancymenu.editor.items.splash.rotation"),
                        true, 20.0F, null, null)
                .setStackable(true);

        this.addGenericStringInputContextMenuEntryTo(this.rightClickMenu, "set_color",
                        consumes -> (consumes instanceof SplashTextEditorElement),
                        consumes -> ((SplashTextElement)consumes.element).baseColor.getHex(),
                        (element1, s) -> {
                            ((SplashTextElement)element1.element).baseColor = DrawableColor.of(s);
                            ((SplashTextElement)element1.element).updateSplash();
                        },
                        null, false, true, Component.translatable("fancymenu.editor.items.splash.basecolor"),
                        true, DrawableColor.of(255, 255, 0).getHex(), TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
                .setStackable(true);

        this.addGenericBooleanSwitcherContextMenuEntryTo(this.rightClickMenu, "shadow",
                        consumes -> (consumes instanceof SplashTextEditorElement),
                        consumes -> ((SplashTextElement)consumes.element).shadow,
                        (element1, s) -> ((SplashTextElement)element1.element).shadow = s,
                        "fancymenu.elements.splash.shadow")
                .setStackable(true);

        this.addGenericBooleanSwitcherContextMenuEntryTo(this.rightClickMenu, "bouncing",
                        consumes -> (consumes instanceof SplashTextEditorElement),
                        consumes -> ((SplashTextElement)consumes.element).bounce,
                        (element1, s) -> ((SplashTextElement)element1.element).bounce = s,
                        "fancymenu.editor.items.splash.bounce")
                .setStackable(true);

        this.addGenericBooleanSwitcherContextMenuEntryTo(this.rightClickMenu, "refresh_on_load",
                        consumes -> (consumes instanceof SplashTextEditorElement),
                        consumes -> ((SplashTextElement)consumes.element).refreshOnMenuReload,
                        (element1, s) -> ((SplashTextElement)element1.element).refreshOnMenuReload = s,
                        "fancymenu.editor.items.splash.refresh")
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.items.splash.refresh.desc")));

    }

}
