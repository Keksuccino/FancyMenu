package de.keksuccino.fancymenu.customization.element.elements.video.rinku;

import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.screen.resource.ResourceChooserWindowBody;
import de.keksuccino.fancymenu.util.resource.ResourceSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.NotNull;
import java.util.Arrays;

public class RinkuVideoEditorElement extends AbstractEditorElement<RinkuVideoEditorElement, RinkuVideoElement> {

    public RinkuVideoEditorElement(@NotNull RinkuVideoElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
    }

    @Override
    public void init() {

        super.init();

        this.rightClickMenu.addClickableEntry("set_source", Component.translatable("fancymenu.elements.video_rinku.set_source"), (menu, entry) -> {
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

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "toggle_loop", RinkuVideoEditorElement.class,
                        element -> element.element.loop,
                        (element, aBoolean) -> element.element.loop = aBoolean,
                        "fancymenu.elements.video_rinku.loop")
                .setStackable(false)
                .setIcon(MaterialIcons.REPEAT);

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "preserve_aspect_ratio", RinkuVideoEditorElement.class,
                        element -> element.element.preserveAspectRatio,
                        (element, aBoolean) -> element.element.preserveAspectRatio = aBoolean,
                        "fancymenu.elements.video_rinku.preserve_aspect_ratio")
                .setStackable(false)
                .setIcon(MaterialIcons.ASPECT_RATIO);

        this.rightClickMenu.addSeparatorEntry("separator_after_toggle_loop");

        this.element.volume.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setIcon(MaterialIcons.VOLUME_UP);

        this.addCycleContextMenuEntryTo(this.rightClickMenu, "sound_channel",
                        Arrays.asList(SoundSource.values()),
                        RinkuVideoEditorElement.class,
                        consumes -> consumes.element.soundSource,
                        (audioEditorElement, soundSource) -> audioEditorElement.element.soundSource = soundSource,
                        (menu, entry, switcherValue) -> Component.translatable("fancymenu.elements.video_rinku.sound_channel", Component.translatable("soundCategory." + switcherValue.getName()).setStyle(Style.EMPTY.withColor(UIBase.getUITheme().warning_color.getColorInt()))))
                .setStackable(false)
                .setIcon(MaterialIcons.VOLUME_UP);

    }


}
