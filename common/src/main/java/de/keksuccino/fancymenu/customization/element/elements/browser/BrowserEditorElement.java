package de.keksuccino.fancymenu.customization.element.elements.browser;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class BrowserEditorElement extends AbstractEditorElement {

    public BrowserEditorElement(@NotNull AbstractElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
    }

    @Override
    public void init() {

        super.init();

        this.addStringInputContextMenuEntryTo(this.rightClickMenu, "url", BrowserEditorElement.class,
                        consumes -> consumes.getElement().url,
                        (element1, s) -> element1.getElement().url = s,
                        CharacterFilter.buildUrlFilter(), false, true, Component.translatable("fancymenu.elements.browser.url"),
                        true, "https://docs.fancymenu.net", null, null)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.browser.url.desc")));

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "interactable", BrowserEditorElement.class,
                        consumes -> consumes.getElement().interactable,
                        (element, aBoolean) -> element.getElement().interactable = aBoolean,
                        "fancymenu.elements.browser.interactable")
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.browser.interactable.desc")))
                .setStackable(true);

    }

    public BrowserElement getElement() {
        return (BrowserElement) this.element;
    }

}
