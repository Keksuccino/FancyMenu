package de.keksuccino.fancymenu.customization.element.elements.video.mcef;

import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.element.elements.video.SetVideoVolumeScreen;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.MathUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.screen.resource.ResourceChooserWindowBody;
import de.keksuccino.fancymenu.util.resource.ResourceSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.NotNull;
import java.util.Arrays;

public class MCEFVideoEditorElement extends AbstractEditorElement<MCEFVideoEditorElement, MCEFVideoElement> {

    public MCEFVideoEditorElement(@NotNull MCEFVideoElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
    }

    @Override
    public void init() {

        super.init();

        this.rightClickMenu.addClickableEntry("set_source", Component.translatable("fancymenu.elements.video_mcef.set_source"), (menu, entry) -> {
                    menu.closeMenuChain();
                    ResourceChooserWindowBody.video(null, source -> {
                        if (source != null) {
                            this.editor.history.saveSnapshot();
                            this.element.rawVideoUrlSource = ResourceSource.of(source);
                        }
                    }).setSource((this.element.rawVideoUrlSource != null) ? this.element.rawVideoUrlSource.getSerializationSource() : null, false)
                            .openInWindow(null);
                }).setIcon(MaterialIcons.VIDEO_FILE)
                .setStackable(false);

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "toggle_loop", MCEFVideoEditorElement.class,
                        element -> element.element.loop,
                        (element, aBoolean) -> element.element.loop = aBoolean,
                        "fancymenu.elements.video_mcef.loop")
                .setStackable(false)
                .setIcon(MaterialIcons.REPEAT);

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "preserve_aspect_ratio", MCEFVideoEditorElement.class,
                        element -> element.element.preserveAspectRatio,
                        (element, aBoolean) -> element.element.preserveAspectRatio = aBoolean,
                        "fancymenu.elements.video_mcef.preserve_aspect_ratio")
                .setStackable(false)
                .setIcon(MaterialIcons.ASPECT_RATIO);

        this.rightClickMenu.addSeparatorEntry("separator_after_toggle_loop");

        this.rightClickMenu.addClickableEntry("set_volume", Component.translatable("fancymenu.elements.video_mcef.volume"), (menu, entry) -> {
            this.openContextMenuScreen(new SetVideoVolumeScreen(this.element.volume, vol -> {
                if (vol != null) {
                    this.editor.history.saveSnapshot();
                    this.element.volume = vol;
                }
                this.openContextMenuScreen(this.editor);
            }));
        }).setStackable(false)
                .setIcon(MaterialIcons.VOLUME_UP);

        this.addCycleContextMenuEntryTo(this.rightClickMenu, "sound_channel",
                        Arrays.asList(SoundSource.values()),
                        MCEFVideoEditorElement.class,
                        consumes -> consumes.element.soundSource,
                        (audioEditorElement, soundSource) -> audioEditorElement.element.soundSource = soundSource,
                        (menu, entry, switcherValue) -> Component.translatable("fancymenu.elements.video_mcef.sound_channel", Component.translatable("soundCategory." + switcherValue.getName()).setStyle(Style.EMPTY.withColor(UIBase.getUITheme().warning_text_color.getColorInt()))))
                .setStackable(false)
                .setIcon(MaterialIcons.VOLUME_UP);

    }


    private static boolean validVolume(String volume) {
        if (volume == null) return false;
        if (!MathUtils.isFloat(volume)) return false;
        float vol = Float.parseFloat(volume);
        return (vol <= 1.0F) && (vol >= 0.0F);
    }

}
