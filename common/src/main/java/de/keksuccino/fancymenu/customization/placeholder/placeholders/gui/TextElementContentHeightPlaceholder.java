package de.keksuccino.fancymenu.customization.placeholder.placeholders.gui;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.elements.text.v2.TextElement;
import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class TextElementContentHeightPlaceholder extends Placeholder {

    @NotNull
    private final Function<String, AbstractElement> elementLookup;

    public TextElementContentHeightPlaceholder() {
        this(AbstractElement::getElementByInstanceIdentifier);
    }

    TextElementContentHeightPlaceholder(@NotNull Function<String, AbstractElement> elementLookup) {
        super("text_element_content_height");
        this.elementLookup = Objects.requireNonNull(elementLookup);
    }

    @Override
    public boolean canRunAsync() {
        return false;
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String elementIdentifier = dps.values.get("element_identifier");
        if ((elementIdentifier != null) && !elementIdentifier.isBlank()) {
            if (this.elementLookup.apply(elementIdentifier) instanceof TextElement textElement) {
                return "" + textElement.getContentHeight();
            }
        }
        return "0";
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return List.of("element_identifier");
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.placeholders.text_element_content_height");
    }

    @Override
    public List<String> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.placeholders.text_element_content_height.desc"));
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
