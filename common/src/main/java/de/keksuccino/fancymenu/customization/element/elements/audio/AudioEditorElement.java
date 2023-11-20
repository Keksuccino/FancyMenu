package de.keksuccino.fancymenu.customization.element.elements.audio;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class AudioEditorElement extends AbstractEditorElement {

    public AudioEditorElement(@NotNull AbstractElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
    }

    @Override
    public void init() {

        super.init();

        this.rightClickMenu.addValueCycleEntry("play_mode",
                        AudioElement.PlayMode.NORMAL.cycle(this.getElement().getPlayMode())
                                .addCycleListener(playMode -> this.getElement().setPlayMode(playMode)))
                .setStackable(false);

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "loop", AudioEditorElement.class,
                consumes -> consumes.getElement().isLooping(),
                (audioEditorElement, aBoolean) -> audioEditorElement.getElement().setLooping(aBoolean),
                "fancymenu.elements.audio.looping");

        this.rightClickMenu.addClickableEntry("manage_tracks", Component.translatable("fancymenu.elements.audio.manage_audios"),
                        (menu, entry) -> Minecraft.getInstance().setScreen(new ManageAudiosScreen(this.getElement().audios, this.editor)))
                .setStackable(false)
                .setIcon(ContextMenu.IconFactory.getIcon("sound"));

        //TODO add sound channel cycle

        //TODO add volume setter

    }

    public AudioElement getElement() {
        return (AudioElement) this.element;
    }

}
