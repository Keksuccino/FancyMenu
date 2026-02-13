package de.keksuccino.fancymenu.customization.element.elements.video.nativevideo;

import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.video.IVideo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class NativeVideoEditorElement extends AbstractEditorElement<NativeVideoEditorElement, NativeVideoElement> {

    public NativeVideoEditorElement(@NotNull NativeVideoElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
    }

    @Override
    public void init() {

        super.init();

        this.element.videoSupplier.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setTooltipSupplier((m, entry) -> {
                    ResourceSupplier<IVideo> supplier = this.element.videoSupplier.get();
                    if (supplier == null) {
                        return UITooltip.of(Component.translatable("fancymenu.elements.video.configure.no_video"));
                    }
                    return null;
                })
                .setIcon(MaterialIcons.MOVIE)
                .setStackable(false);

        this.rightClickMenu.addSeparatorEntry("separator_after_video_source");

        this.element.loop.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setIcon(MaterialIcons.REPEAT)
                .setStackable(false);

        this.element.preserveAspectRatio.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setIcon(MaterialIcons.ASPECT_RATIO)
                .setStackable(false);

        this.element.volume.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setIcon(MaterialIcons.VOLUME_UP)
                .setStackable(false);

        this.addCycleContextMenuEntryTo(this.rightClickMenu, "sound_source",
                        Arrays.asList(SoundSource.values()),
                        NativeVideoEditorElement.class,
                        consumes -> consumes.element.getSoundSourceOrDefault(),
                        (editorElement, source) -> {
                            if (source != null) {
                                editorElement.element.soundSource.set(source.getName());
                            }
                        },
                        (menu, entry, switcherValue) -> Component.translatable("fancymenu.elements.video_mcef.sound_channel",
                                Component.translatable("soundCategory." + switcherValue.getName()).setStyle(Style.EMPTY.withColor(UIBase.getUITheme().warning_color.getColorInt()))))
                .setStackable(false)
                .setIcon(MaterialIcons.SPEAKER);

        this.rightClickMenu.addSeparatorEntry("separator_before_play_in_editor");

        this.element.playInEditor.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setIcon(MaterialIcons.EDIT)
                .setStackable(false);
    }
}
