
package de.keksuccino.fancymenu.menu.placeholder.v2.placeholders.advanced;

import de.keksuccino.fancymenu.menu.placeholder.v2.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.menu.placeholder.v2.Placeholder;
import de.keksuccino.fancymenu.menu.placeholder.v2.PlaceholderParser;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
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
        return Locals.localize("fancymenu.helper.placeholder.stringify");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(StringUtils.splitLines(Locals.localize("fancymenu.helper.placeholder.stringify.desc"), "%n%"));
    }

    @Override
    public String getCategory() {
        return Locals.localize("fancymenu.helper.ui.dynamicvariabletextfield.categories.advanced");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        Map<String, String> m = new HashMap<>();
        m.put("text", "text to stringify");
        return DeserializedPlaceholderString.build(this.getIdentifier(), m);
    }

}
