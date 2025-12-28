package de.keksuccino.fancymenu.customization.element.elements.browser;

import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
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
                        consumes -> consumes.getElement().url,
                        (element1, s) -> element1.getElement().url = s,
                        null, false, true, Component.translatable("fancymenu.elements.browser.url"),
                        true, "https://docs.fancymenu.net", null, null)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.browser.url.desc")));

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "interactable", BrowserEditorElement.class,
                        consumes -> consumes.getElement().interactable,
                        (element, aBoolean) -> element.getElement().interactable = aBoolean,
                        "fancymenu.elements.browser.interactable")
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.browser.interactable.desc")))
                .setStackable(true);

        this.rightClickMenu.addSeparatorEntry("separator_after_interactable");

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "hide_video_controls", BrowserEditorElement.class,
                        consumes -> consumes.getElement().hideVideoControls,
                        (element, aBoolean) -> element.getElement().hideVideoControls = aBoolean,
                        "fancymenu.elements.browser.hide_video_controls")
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.browser.hide_video_controls.desc")))
                .setStackable(true);

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "loop_videos", BrowserEditorElement.class,
                        consumes -> consumes.getElement().loopVideos,
                        (element, aBoolean) -> element.getElement().loopVideos = aBoolean,
                        "fancymenu.elements.browser.loop_videos")
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.browser.loop_videos.desc")))
                .setStackable(true);

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "mute_media", BrowserEditorElement.class,
                        consumes -> consumes.getElement().muteMedia,
                        (element, aBoolean) -> element.getElement().muteMedia = aBoolean,
                        "fancymenu.elements.browser.mute_media")
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.browser.mute_media.desc")))
                .setStackable(true);

        this.addFloatInputContextMenuEntryTo(this.rightClickMenu, "media_volume", BrowserEditorElement.class,
                        consumes -> consumes.getElement().mediaVolume,
                        (element1, aFloat) -> element1.getElement().mediaVolume = aFloat,
                        Component.translatable("fancymenu.elements.browser.media_volume"), true, 1.0F, null, null)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.browser.media_volume.desc")))
                .setStackable(true);

    }

    public BrowserElement getElement() {
        return (BrowserElement) this.element;
    }

}
