package de.keksuccino.fancymenu.customization.element.elements.audio;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.text.Components;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.screen.NotificationScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.NotNull;
import java.util.Arrays;

public class AudioEditorElement extends AbstractEditorElement {

    public AudioEditorElement(@NotNull AbstractElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
        this.settings.setInEditorColorSupported(true);
    }

    @Override
    public void init() {

        super.init();

        this.rightClickMenu.addValueCycleEntry("play_mode",
                        AudioElement.PlayMode.NORMAL.cycle(this.getElement().getPlayMode())
                                .addCycleListener(playMode -> this.getElement().setPlayMode(playMode, true)))
                .setStackable(false);

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "loop", AudioEditorElement.class,
                consumes -> consumes.getElement().isLooping(),
                (audioEditorElement, aBoolean) -> audioEditorElement.getElement().setLooping(aBoolean, true),
                "fancymenu.elements.audio.looping");

        this.rightClickMenu.addClickableEntry("manage_tracks", Components.translatable("fancymenu.elements.audio.manage_audios"),
                        (menu, entry) -> Minecraft.getInstance().setScreen(new ManageAudiosScreen(this.getElement(), this.getElement().audios, this.editor)))
                .setStackable(false)
                .setIcon(ContextMenu.IconFactory.getIcon("sound"));

        this.rightClickMenu.addSeparatorEntry("separator_after_manage_tracks").setStackable(false);

        this.addCycleContextMenuEntryTo(this.rightClickMenu, "sound_channel",
                        Arrays.asList(SoundSource.values()),
                        AudioEditorElement.class,
                        consumes -> consumes.getElement().soundSource,
                        (audioEditorElement, soundSource) -> audioEditorElement.getElement().setSoundSource(soundSource),
                        (menu, entry, switcherValue) -> Components.translatable("fancymenu.elements.audio.sound_channel", Components.translatable("soundCategory." + switcherValue.getName()).setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().warning_text_color.getColorInt()))))
                .setStackable(false);

        this.rightClickMenu.addClickableEntry("volume", Components.translatable("fancymenu.elements.audio.set_volume"), (menu, entry) -> {
            Minecraft.getInstance().setScreen(new SetAudioVolumeScreen(this.getElement().volume, aFloat -> {
                if (aFloat != null) {
                    this.getElement().setVolume(aFloat);
                }
                Minecraft.getInstance().setScreen(this.editor);
            }));
        });

        this.rightClickMenu.addSeparatorEntry("separator_after_volume");

        this.rightClickMenu.addClickableEntry("disable_vanilla_music", Components.translatable("fancymenu.elements.audio.disable_vanilla_music"), (menu, entry) -> {
                    Minecraft.getInstance().setScreen(NotificationScreen.notificationWithHeadline(
                            aBoolean -> Minecraft.getInstance().setScreen(this.editor),
                            LocalizationUtils.splitLocalizedLines("fancymenu.elements.audio.disable_vanilla_music.desc")));
                })
                .setStackable(true);

    }

    public AudioElement getElement() {
        return (AudioElement) this.element;
    }

}
