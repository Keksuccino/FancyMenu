package de.keksuccino.fancymenu.customization.element.elements.browser;

import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class BrowserEditorElement extends AbstractEditorElement<BrowserEditorElement, BrowserElement> {

    public BrowserEditorElement(@NotNull BrowserElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
    }

    @Override
    public void init() {

        super.init();

        this.addStringInputContextMenuEntryTo(this.rightClickMenu, "url", BrowserEditorElement.class,
                        consumes -> consumes.element.url,
                        (element1, s) -> element1.element.url = s,
                        null, false, true, Component.translatable("fancymenu.elements.browser.url"),
                        true, "https://docs.fancymenu.net", null, null)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.browser.url.desc")));

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "interactable", BrowserEditorElement.class,
                        consumes -> consumes.element.interactable,
                        (element, aBoolean) -> element.element.interactable = aBoolean,
                        "fancymenu.elements.browser.interactable")
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.browser.interactable.desc")))
                .setStackable(true);

        this.rightClickMenu.addSeparatorEntry("separator_after_interactable");

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "hide_video_controls", BrowserEditorElement.class,
                        consumes -> consumes.element.hideVideoControls,
                        (element, aBoolean) -> element.element.hideVideoControls = aBoolean,
                        "fancymenu.elements.browser.hide_video_controls")
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.browser.hide_video_controls.desc")))
                .setStackable(true);

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "loop_videos", BrowserEditorElement.class,
                        consumes -> consumes.element.loopVideos,
                        (element, aBoolean) -> element.element.loopVideos = aBoolean,
                        "fancymenu.elements.browser.loop_videos")
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.browser.loop_videos.desc")))
                .setStackable(true);

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "mute_media", BrowserEditorElement.class,
                        consumes -> consumes.element.muteMedia,
                        (element, aBoolean) -> element.element.muteMedia = aBoolean,
                        "fancymenu.elements.browser.mute_media")
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.browser.mute_media.desc")))
                .setStackable(true);

        this.addFloatInputContextMenuEntryTo(this.rightClickMenu, "media_volume", BrowserEditorElement.class,
                        consumes -> consumes.element.mediaVolume,
                        (element1, aFloat) -> element1.element.mediaVolume = aFloat,
                        Component.translatable("fancymenu.elements.browser.media_volume"), true, 1.0F, null, null)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.browser.media_volume.desc")))
                .setStackable(true);

    }


}
