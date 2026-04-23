package de.keksuccino.fancymenu.customization.element.elements.splash;

import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.input.TextValidators;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.ListUtils;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class SplashTextEditorElement extends AbstractEditorElement<SplashTextEditorElement, SplashTextElement> {

    public SplashTextEditorElement(@NotNull SplashTextElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
    }

    @Override
    public void init() {

        super.init();

        this.addGenericCycleContextMenuEntryTo(this.rightClickMenu, "set_mode",
                ListUtils.of(SplashTextElement.SourceMode.VANILLA, SplashTextElement.SourceMode.DIRECT_TEXT, SplashTextElement.SourceMode.TEXT_FILE),
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
                })
                .setIcon(MaterialIcons.TUNE);

        this.addTextResourceChooserContextMenuEntryTo(this.rightClickMenu, "set_source_file",
                        SplashTextEditorElement.class,
                        null,
                        consumes -> consumes.element.textFileSupplier,
                        (splashTextEditorElement, iTextResourceSupplier) -> {
                            splashTextEditorElement.element.textFileSupplier = iTextResourceSupplier;
                            if (iTextResourceSupplier != null) splashTextEditorElement.element.source = iTextResourceSupplier.getSourceWithPrefix();
                            splashTextEditorElement.element.refresh();
                            splashTextEditorElement.element.updateSplash();
                        },
                        Component.translatable("fancymenu.elements.splash.source_mode.text_file.set_source"),
                        false, null, true, true, true)
                .setIsVisibleSupplier((menu, entry) -> ((SplashTextElement)this.element).sourceMode == SplashTextElement.SourceMode.TEXT_FILE)
                .setIcon(MaterialIcons.FILE_OPEN);

        this.addGenericStringInputContextMenuEntryTo(this.rightClickMenu, "input_direct",
                        consumes -> (consumes instanceof SplashTextEditorElement),
                        consumes -> ((SplashTextElement)consumes.element).source,
                        (element1, s) -> {
                            ((SplashTextElement)element1.element).source = s;
                            ((SplashTextElement)element1.element).updateSplash();
                        },
                        null, false, true, Component.translatable("fancymenu.elements.splash.source_mode.direct.set_source"),
                        false, null, TextValidators.NO_EMPTY_STRING_TEXT_VALIDATOR, null)
                .setIsVisibleSupplier((menu, entry) -> ((SplashTextElement)this.element).sourceMode == SplashTextElement.SourceMode.DIRECT_TEXT)
                .setIcon(MaterialIcons.TEXT_FIELDS);

        this.rightClickMenu.addSeparatorEntry("splash_separator_1");

        this.element.scale.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setStackable(true)
                .setIcon(MaterialIcons.FORMAT_SIZE);

        this.element.rotation.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setStackable(true)
                .setIcon(MaterialIcons.ROTATE_RIGHT);

        this.element.baseColor.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setIcon(MaterialIcons.PALETTE);

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "shadow",
                        SplashTextEditorElement.class,
                        element -> element.element.shadow,
                        (element, s) -> element.element.shadow = s,
                        "fancymenu.elements.splash.shadow")
                .setStackable(true)
                .setIcon(MaterialIcons.SHADOW);

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "bouncing",
                        SplashTextEditorElement.class,
                        element -> element.element.bounce,
                        (element, s) -> element.element.bounce = s,
                        "fancymenu.elements.splash.bounce")
                .setStackable(true)
                .setIcon(MaterialIcons.ANIMATION);

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "refresh_on_load",
                        SplashTextEditorElement.class,
                        element -> element.element.refreshOnMenuReload,
                        (element, s) -> element.element.refreshOnMenuReload = s,
                        "fancymenu.elements.splash.refresh")
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.splash.refresh.desc")))
                .setIcon(MaterialIcons.REFRESH);

    }


}
