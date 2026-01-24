package de.keksuccino.fancymenu.customization.element.elements.audio;

import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.Dialogs;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.message.MessageDialogStyle;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.NotNull;
import java.util.Arrays;

public class AudioEditorElement extends AbstractEditorElement<AudioEditorElement, AudioElement> {

    public AudioEditorElement(@NotNull AudioElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
        this.settings.setInEditorColorSupported(true);
    }

    @Override
    public void init() {

        super.init();

        this.rightClickMenu.addValueCycleEntry("play_mode",
                        AudioElement.PlayMode.NORMAL.cycle(this.element.getPlayMode())
                                .addCycleListener(playMode -> this.element.setPlayMode(playMode, true)))
                .setStackable(false)
                .setIcon(MaterialIcons.PLAY_ARROW);

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "loop", AudioEditorElement.class,
                consumes -> consumes.element.isLooping(),
                (audioEditorElement, aBoolean) -> audioEditorElement.element.setLooping(aBoolean, true),
                "fancymenu.elements.audio.looping")
                .setIcon(MaterialIcons.REPEAT);

        this.rightClickMenu.addClickableEntry("manage_tracks", Component.translatable("fancymenu.elements.audio.manage_audios"),
                        (menu, entry) -> this.openContextMenuScreen(new ManageAudiosScreen(this.element, this.element.audios, this.editor)))
                .setStackable(false)
                .setIcon(MaterialIcons.QUEUE_MUSIC);

        this.rightClickMenu.addSeparatorEntry("separator_after_manage_tracks").setStackable(false);

        this.addCycleContextMenuEntryTo(this.rightClickMenu, "sound_channel",
                        Arrays.asList(SoundSource.values()),
                        AudioEditorElement.class,
                        consumes -> consumes.element.soundSource,
                        (audioEditorElement, soundSource) -> audioEditorElement.element.setSoundSource(soundSource),
                        (menu, entry, switcherValue) -> Component.translatable("fancymenu.elements.audio.sound_channel", Component.translatable("soundCategory." + switcherValue.getName()).setStyle(Style.EMPTY.withColor(UIBase.getUITheme().warning_text_color.getColorInt()))))
                .setStackable(false)
                .setIcon(MaterialIcons.VOLUME_UP);

        this.rightClickMenu.addClickableEntry("volume", Component.translatable("fancymenu.elements.audio.set_volume"), (menu, entry) -> {
            this.openContextMenuScreen(new SetAudioVolumeScreen(this.element.volume, aFloat -> {
                if (aFloat != null) {
                    this.element.setVolume(aFloat);
                }
                this.openContextMenuScreen(this.editor);
            }));
        }).setIcon(MaterialIcons.VOLUME_UP);

        this.rightClickMenu.addSeparatorEntry("separator_after_volume");

        this.rightClickMenu.addClickableEntry("disable_vanilla_music", Component.translatable("fancymenu.elements.audio.disable_vanilla_music"), (menu, entry) -> {
                    Dialogs.openMessage(Component.translatable("fancymenu.elements.audio.disable_vanilla_music.desc"), MessageDialogStyle.INFO);
                })
                .setStackable(true)
                .setIcon(MaterialIcons.MUSIC_OFF);

    }


}
