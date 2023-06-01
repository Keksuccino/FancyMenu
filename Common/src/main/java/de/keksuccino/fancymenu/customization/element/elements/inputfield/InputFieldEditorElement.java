package de.keksuccino.fancymenu.customization.element.elements.inputfield;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.utils.ListUtils;
import de.keksuccino.fancymenu.utils.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class InputFieldEditorElement extends AbstractEditorElement {

    public InputFieldEditorElement(@NotNull AbstractElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
    }

    @Override
    public void init() {

        super.init();

        this.addStringInputContextMenuEntryTo(this.rightClickMenu, "set_variable", null,
                        consumes -> (consumes instanceof InputFieldEditorElement),
                        null,
                        consumes -> ((InputFieldElement)consumes.element).linkedVariable,
                        (element, varName) -> ((InputFieldElement)element.element).linkedVariable = varName,
                        false, false, Component.translatable("fancymenu.customization.items.input_field.editor.set_variable"))
                .setTooltipSupplier((menu, entry) -> Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.customization.items.input_field.editor.set_variable.desc")));

        this.rightClickMenu.addSeparatorEntry("input_field_separator_1");

        this.addSwitcherContextMenuEntryTo(this.rightClickMenu, "set_type",
                        ListUtils.build(InputFieldElement.InputFieldType.TEXT, InputFieldElement.InputFieldType.URL, InputFieldElement.InputFieldType.INTEGER_ONLY, InputFieldElement.InputFieldType.DECIMAL_ONLY),
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
                .setTooltipSupplier((menu, entry) -> Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.customization.items.input_field.editor.set_type.desc")));


        this.addIntegerInputContextMenuEntryTo(this.rightClickMenu, "set_max_length",
                        consumes -> (consumes instanceof InputFieldEditorElement),
                        10000,
                        consumes -> ((InputFieldElement)consumes.element).maxTextLength,
                        (element, length) -> ((InputFieldElement)element.element).maxTextLength = length,
                        Component.translatable("fancymenu.customization.items.input_field.editor.set_max_length"))
                .setStackable(true);

    }

}
