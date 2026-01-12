package de.keksuccino.fancymenu.customization.element.elements.dragger;

import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
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
                consumes -> consumes.element.saveDragOffset,
                (draggerEditorElement, aBoolean) -> {
                    draggerEditorElement.element.saveDragOffset = aBoolean;
                    if (!aBoolean) {
                        DraggerElementHandler.putMeta(draggerEditorElement.element.getInstanceIdentifier(), 0, 0);
                    }
                },
                "fancymenu.elements.dragger.save_offset")
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.dragger.save_offset.desc")));

    }


}
