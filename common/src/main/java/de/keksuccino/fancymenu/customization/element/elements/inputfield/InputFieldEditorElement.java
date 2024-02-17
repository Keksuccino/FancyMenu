package de.keksuccino.fancymenu.customization.element.elements.inputfield;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.input.TextValidators;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.ListUtils;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class InputFieldEditorElement extends AbstractEditorElement {

    public InputFieldEditorElement(@NotNull AbstractElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
    }

    @Override
    public void init() {

        super.init();

        this.addGenericStringInputContextMenuEntryTo(this.rightClickMenu, "set_variable",
                        consumes -> (consumes instanceof InputFieldEditorElement),
                        consumes -> ((InputFieldElement)consumes.element).linkedVariable,
                        (element, varName) -> ((InputFieldElement)element.element).linkedVariable = varName,
                        null, false, false, Component.translatable("fancymenu.customization.items.input_field.editor.set_variable"),
                        true, null, TextValidators.NO_EMPTY_STRING_TEXT_VALIDATOR, null)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.customization.items.input_field.editor.set_variable.desc")))
                .setIcon(ContextMenu.IconFactory.getIcon("script"));

        this.rightClickMenu.addSeparatorEntry("input_field_separator_1");

        this.addGenericCycleContextMenuEntryTo(this.rightClickMenu, "set_type",
                        ListUtils.of(InputFieldElement.InputFieldType.TEXT, InputFieldElement.InputFieldType.URL, InputFieldElement.InputFieldType.INTEGER_ONLY, InputFieldElement.InputFieldType.DECIMAL_ONLY),
                        consumes -> (consumes instanceof InputFieldEditorElement),
                        consumes -> ((InputFieldElement)consumes.element).type,
                        (element, type) -> ((InputFieldElement)element.element).type = type,
                        (menu, entry, switcherValue) -> {
                            if (switcherValue == InputFieldElement.InputFieldType.TEXT) {
                                return Component.translatable("fancymenu.customization.items.input_field.type.text");
                            }
                            if (switcherValue == InputFieldElement.InputFieldType.INTEGER_ONLY) {
                                return Component.translatable("fancymenu.customization.items.input_field.type.integer");
                            }
                            if (switcherValue == InputFieldElement.InputFieldType.DECIMAL_ONLY) {
                                return Component.translatable("fancymenu.customization.items.input_field.type.decimal");
                            }
                            return Component.translatable("fancymenu.customization.items.input_field.type.url");
                        })
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.customization.items.input_field.editor.set_type.desc")));


        this.addGenericIntegerInputContextMenuEntryTo(this.rightClickMenu, "set_max_length",
                        consumes -> (consumes instanceof InputFieldEditorElement),
                        consumes -> ((InputFieldElement)consumes.element).maxTextLength,
                        (element, length) -> ((InputFieldElement)element.element).maxTextLength = length,
                        Component.translatable("fancymenu.customization.items.input_field.editor.set_max_length"),
                        true, 10000, null, null)
                .setStackable(true);

        this.rightClickMenu.addSeparatorEntry("separator_before_navigatable");

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "toggle_navigatable", InputFieldEditorElement.class,
                        consumes -> consumes.getElement().navigatable,
                        (buttonEditorElement, aBoolean) -> buttonEditorElement.getElement().navigatable = aBoolean,
                        "fancymenu.elements.widgets.generic.navigatable")
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.widgets.generic.navigatable.desc")));

    }

    public InputFieldElement getElement() {
        return (InputFieldElement) this.element;
    }

}
