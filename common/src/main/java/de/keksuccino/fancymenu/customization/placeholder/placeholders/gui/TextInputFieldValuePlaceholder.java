package de.keksuccino.fancymenu.customization.placeholder.placeholders.gui;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.element.elements.button.vanillawidget.VanillaWidgetElement;
import de.keksuccino.fancymenu.customization.element.elements.inputfield.InputFieldElement;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.HashMap;
import java.util.List;

public class TextInputFieldValuePlaceholder extends Placeholder {

    public TextInputFieldValuePlaceholder() {
        super("text_input_field_value");
    }

    @Override
    public boolean canRunAsync() {
        return false;
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        if (Minecraft.getInstance().screen == null) {
            return "";
        }

        String elementIdentifier = dps.values.get("element_identifier");
        if (elementIdentifier == null || elementIdentifier.isBlank()) {
            return "";
        }

        AbstractElement element = findElement(elementIdentifier);
        if (element == null) {
            return "";
        }

        if (element instanceof InputFieldElement inputFieldElement) {
            if (inputFieldElement.editBox == null) {
                return "";
            }
            return inputFieldElement.editBox.getValue();
        }

        if (element instanceof VanillaWidgetElement widgetElement) {
            if (widgetElement.getWidget() instanceof EditBox editBox) {
                return editBox.getValue();
            }
        }

        return "";
    }

    @Nullable
    private AbstractElement findElement(@NotNull String id) {
        if (Minecraft.getInstance().screen == null) {
            return null;
        }

        if (Minecraft.getInstance().screen instanceof LayoutEditorScreen editor) {
            AbstractEditorElement e = editor.getElementByInstanceIdentifier(id);
            return e != null ? e.element : null;
        }

        ScreenCustomizationLayer layer = ScreenCustomizationLayerHandler.getLayerOfScreen(Minecraft.getInstance().screen);
        if (layer == null) {
            return null;
        }
        return layer.getElementByInstanceIdentifier(id);
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return List.of("element_identifier");
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.placeholders.text_input_field_value");
    }

    @Override
    public List<String> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.placeholders.text_input_field_value.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.requirements.categories.gui");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        HashMap<String, String> values = new HashMap<>();
        values.put("element_identifier", "some.element.id");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }

}
