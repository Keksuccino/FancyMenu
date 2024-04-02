package de.keksuccino.fancymenu.customization.element.elements.musiccontroller;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import org.jetbrains.annotations.NotNull;

//TODO übernehmen
public class MusicControllerEditorElement extends AbstractEditorElement {

    public MusicControllerEditorElement(@NotNull AbstractElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
    }

    @Override
    public void init() {

        super.init();

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "play_menu_music", MusicControllerEditorElement.class,
                        consumes -> consumes.getElement().playMenuMusic,
                        (musicControllerEditorElement, aBoolean) -> musicControllerEditorElement.getElement().playMenuMusic = aBoolean,
                        "fancymenu.elements.music_controller.play_menu_music")
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.music_controller.play_menu_music.desc")));

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "play_world_music", MusicControllerEditorElement.class,
                        consumes -> consumes.getElement().playWorldMusic,
                        (musicControllerEditorElement, aBoolean) -> musicControllerEditorElement.getElement().playWorldMusic = aBoolean,
                        "fancymenu.elements.music_controller.play_world_music")
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.music_controller.play_world_music.desc")));

    }

    public MusicControllerElement getElement() {
        return (MusicControllerElement) this.element;
    }

}
