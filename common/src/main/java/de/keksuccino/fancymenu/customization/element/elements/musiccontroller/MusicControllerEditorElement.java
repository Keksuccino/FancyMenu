package de.keksuccino.fancymenu.customization.element.elements.musiccontroller;

import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import org.jetbrains.annotations.NotNull;

public class MusicControllerEditorElement extends AbstractEditorElement<MusicControllerEditorElement, MusicControllerElement> {

    public MusicControllerEditorElement(@NotNull MusicControllerElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
        this.settings.setInEditorColorSupported(true);
    }

    @Override
    public void init() {

        super.init();

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "play_menu_music", MusicControllerEditorElement.class,
                        consumes -> consumes.element.playMenuMusic,
                        (musicControllerEditorElement, aBoolean) -> musicControllerEditorElement.element.playMenuMusic = aBoolean,
                        "fancymenu.elements.music_controller.play_menu_music")
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.music_controller.play_menu_music.desc")));

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "play_world_music", MusicControllerEditorElement.class,
                        consumes -> consumes.element.playWorldMusic,
                        (musicControllerEditorElement, aBoolean) -> musicControllerEditorElement.element.playWorldMusic = aBoolean,
                        "fancymenu.elements.music_controller.play_world_music")
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.music_controller.play_world_music.desc")));

    }


}
