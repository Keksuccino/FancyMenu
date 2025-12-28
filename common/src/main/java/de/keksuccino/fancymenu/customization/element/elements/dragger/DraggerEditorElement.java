package de.keksuccino.fancymenu.customization.element.elements.dragger;

import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import org.jetbrains.annotations.NotNull;

public class DraggerEditorElement extends AbstractEditorElement<DraggerEditorElement, DraggerElement> {

    public DraggerEditorElement(@NotNull DraggerElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
        this.settings.setInEditorColorSupported(true);
    }

    @Override
    public void init() {

        super.init();

        addToggleContextMenuEntryTo(this.rightClickMenu, "save_drag_offset", DraggerEditorElement.class,
                consumes -> consumes.getElement().saveDragOffset,
                (draggerEditorElement, aBoolean) -> {
                    draggerEditorElement.getElement().saveDragOffset = aBoolean;
                    if (!aBoolean) {
                        DraggerElementHandler.putMeta(draggerEditorElement.getElement().getInstanceIdentifier(), 0, 0);
                    }
                },
                "fancymenu.elements.dragger.save_offset")
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.dragger.save_offset.desc")));

    }

    public DraggerElement getElement() {
        return (DraggerElement) this.element;
    }

}
