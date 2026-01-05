package de.keksuccino.fancymenu.customization.element.elements.audio;

import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.screen.NotificationScreen;
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
                .setStackable(false);

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "loop", AudioEditorElement.class,
                consumes -> consumes.element.isLooping(),
                (audioEditorElement, aBoolean) -> audioEditorElement.element.setLooping(aBoolean, true),
                "fancymenu.elements.audio.looping");

        this.rightClickMenu.addClickableEntry("manage_tracks", Component.translatable("fancymenu.elements.audio.manage_audios"),
                        (menu, entry) -> this.openContextMenuScreen(new ManageAudiosScreen(this.element, this.element.audios, this.editor)))
                .setStackable(false)
                .setIcon(ContextMenu.IconFactory.getIcon("sound"));

        this.rightClickMenu.addSeparatorEntry("separator_after_manage_tracks").setStackable(false);

        this.addCycleContextMenuEntryTo(this.rightClickMenu, "sound_channel",
                        Arrays.asList(SoundSource.values()),
                        AudioEditorElement.class,
                        consumes -> consumes.element.soundSource,
                        (audioEditorElement, soundSource) -> audioEditorElement.element.setSoundSource(soundSource),
                        (menu, entry, switcherValue) -> Component.translatable("fancymenu.elements.audio.sound_channel", Component.translatable("soundCategory." + switcherValue.getName()).setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().warning_text_color.getColorInt()))))
                .setStackable(false);

        this.rightClickMenu.addClickableEntry("volume", Component.translatable("fancymenu.elements.audio.set_volume"), (menu, entry) -> {
            this.openContextMenuScreen(new SetAudioVolumeScreen(this.element.volume, aFloat -> {
                if (aFloat != null) {
                    this.element.setVolume(aFloat);
                }
                this.openContextMenuScreen(this.editor);
            }));
        });

        this.rightClickMenu.addSeparatorEntry("separator_after_volume");

        this.rightClickMenu.addClickableEntry("disable_vanilla_music", Component.translatable("fancymenu.elements.audio.disable_vanilla_music"), (menu, entry) -> {
                    this.openContextMenuScreen(NotificationScreen.notificationWithHeadline(
                            aBoolean -> this.openContextMenuScreen(this.editor),
                            LocalizationUtils.splitLocalizedLines("fancymenu.elements.audio.disable_vanilla_music.desc")));
                })
                .setStackable(true);

    }


}
