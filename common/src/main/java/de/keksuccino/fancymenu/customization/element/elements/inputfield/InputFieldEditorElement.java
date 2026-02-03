package de.keksuccino.fancymenu.customization.element.elements.inputfield;

import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.input.TextValidators;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.ListUtils;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class InputFieldEditorElement extends AbstractEditorElement<InputFieldEditorElement, InputFieldElement> {

    public InputFieldEditorElement(@NotNull InputFieldElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
    }

    @Override
    public void init() {

        super.init();

        this.addGenericStringInputContextMenuEntryTo(this.rightClickMenu, "set_variable",
                        consumes -> (consumes instanceof InputFieldEditorElement),
                        consumes -> consumes.element.linkedVariable,
                        (element, varName) -> element.element.linkedVariable = varName,
                        null, false, false, Component.translatable("fancymenu.elements.input_field.editor.set_variable"),
                        true, null, TextValidators.NO_EMPTY_STRING_TEXT_VALIDATOR, null)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.input_field.editor.set_variable.desc")))
                .setIcon(MaterialIcons.VARIABLES);

        this.rightClickMenu.addSeparatorEntry("input_field_separator_1");

        this.addGenericCycleContextMenuEntryTo(this.rightClickMenu, "set_type",
                        ListUtils.of(InputFieldElement.InputFieldType.TEXT, InputFieldElement.InputFieldType.URL, InputFieldElement.InputFieldType.INTEGER_ONLY, InputFieldElement.InputFieldType.DECIMAL_ONLY),
                        consumes -> (consumes instanceof InputFieldEditorElement),
                        consumes -> consumes.element.type,
                        (element, type) -> element.element.type = type,
                        (menu, entry, switcherValue) -> {
                            if (switcherValue == InputFieldElement.InputFieldType.TEXT) {
                                return Component.translatable("fancymenu.elements.input_field.type.text");
                            }
                            if (switcherValue == InputFieldElement.InputFieldType.INTEGER_ONLY) {
                                return Component.translatable("fancymenu.elements.input_field.type.integer");
                            }
                            if (switcherValue == InputFieldElement.InputFieldType.DECIMAL_ONLY) {
                                return Component.translatable("fancymenu.elements.input_field.type.decimal");
                            }
                            return Component.translatable("fancymenu.elements.input_field.type.url");
                        })
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.input_field.editor.set_type.desc")))
                .setIcon(MaterialIcons.TUNE);


        this.element.maxTextLength.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setStackable(true)
                .setIcon(MaterialIcons.SHORT_TEXT);

        this.rightClickMenu.addSeparatorEntry("separator_before_audios");

        this.element.hoverSound.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setIcon(MaterialIcons.VOLUME_UP);

        this.element.unhoverAudio.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setIcon(MaterialIcons.VOLUME_UP);

        this.element.clickSound.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setIcon(MaterialIcons.VOLUME_UP);

        this.rightClickMenu.addSeparatorEntry("separator_before_navigatable");

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "toggle_navigatable", InputFieldEditorElement.class,
                        consumes -> consumes.element.navigatable,
                        (buttonEditorElement, aBoolean) -> buttonEditorElement.element.navigatable = aBoolean,
                        "fancymenu.elements.widgets.generic.navigatable")
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.widgets.generic.navigatable.desc")))
                .setIcon(MaterialIcons.MOUSE);

    }


}
