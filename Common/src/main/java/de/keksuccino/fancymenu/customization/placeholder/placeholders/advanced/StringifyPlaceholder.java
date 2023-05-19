package de.keksuccino.fancymenu.customization.placeholder.placeholders.advanced;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.utils.LocalizationUtils;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class StringifyPlaceholder extends Placeholder {

    public StringifyPlaceholder() {
        super("stringify");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String text = dps.values.get("text");
        if (text != null) {
            text = PlaceholderParser.replacePlaceholders(text);
            return text.replace("\"", "\\\"").replace("{", "\\{").replace("}", "\\}");
        }
        return null;
    }

    @Override
    public @Nullable List<String> getValueNames() {
        List<String> l = new ArrayList<>();
        l.add("text");
        return l;
    }

    @Override
    public String getDisplayName() {
        return I18n.get("fancymenu.helper.placeholder.stringify");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.helper.placeholder.stringify.desc")));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.fancymenu.editor.dynamicvariabletextfield.categories.advanced");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        Map<String, String> m = new HashMap<>();
        m.put("text", "text to stringify");
        return DeserializedPlaceholderString.build(this.getIdentifier(), m);
    }

}
