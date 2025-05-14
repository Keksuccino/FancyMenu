package de.keksuccino.fancymenu.customization.element.elements.video.mcef;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.MathUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.screen.resource.ResourceChooserScreen;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.resource.ResourceSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.NotNull;
import java.util.Arrays;

public class McefVideoEditorElement extends AbstractEditorElement {

    public McefVideoEditorElement(@NotNull AbstractElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
    }

    @Override
    public void init() {

        super.init();

        this.rightClickMenu.addClickableEntry("set_source", Component.translatable("fancymenu.elements.video_mcef.set_source"), (menu, entry) -> {
                    Minecraft.getInstance().setScreen(ResourceChooserScreen.video(null, source -> {
                        if (source != null) {
                            this.editor.history.saveSnapshot();
                            this.getElement().rawVideoUrlSource = ResourceSource.of(source);
                        }
                        Minecraft.getInstance().setScreen(this.editor);
                    }).setSource((this.getElement().rawVideoUrlSource != null) ? this.getElement().rawVideoUrlSource.getSourceWithPrefix() : null, false));
                }).setIcon(ContextMenu.IconFactory.getIcon("image"))
                .setStackable(false);

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "toggle_loop", McefVideoEditorElement.class,
                        element -> element.getElement().loop,
                        (element, aBoolean) -> element.getElement().loop = aBoolean,
                        "fancymenu.elements.video_mcef.loop")
                .setStackable(false);

        this.rightClickMenu.addSeparatorEntry("separator_after_toggle_loop");

        this.addFloatInputContextMenuEntryTo(this.rightClickMenu, "set_volume", McefVideoEditorElement.class,
                consumes -> consumes.getElement().volume,
                (element1, aFloat) -> element1.getElement().volume = aFloat,
                Component.translatable("fancymenu.elements.video_mcef.volume"), true, 1.0F,
                McefVideoEditorElement::validVolume,
                consumes -> validVolume(consumes) ? null : Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.video_mcef.volume.invalid_value")));

        this.addCycleContextMenuEntryTo(this.rightClickMenu, "sound_channel",
                        Arrays.asList(SoundSource.values()),
                        McefVideoEditorElement.class,
                        consumes -> consumes.getElement().soundSource,
                        (audioEditorElement, soundSource) -> audioEditorElement.getElement().soundSource = soundSource,
                        (menu, entry, switcherValue) -> Component.translatable("fancymenu.elements.video_mcef.sound_channel", Component.translatable("soundCategory." + switcherValue.getName()).setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().warning_text_color.getColorInt()))))
                .setStackable(false);

    }

    public McefVideoElement getElement() {
        return (McefVideoElement) this.element;
    }

    private static boolean validVolume(String volume) {
        if (volume == null) return false;
        if (!MathUtils.isFloat(volume)) return false;
        float vol = Float.parseFloat(volume);
        return (vol <= 1.0F) && (vol >= 0.0F);
    }

}
