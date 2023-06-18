package de.keksuccino.fancymenu.customization.placeholder.placeholders.advanced;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.customization.variables.VariableHandler;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GetVariablePlaceholder extends Placeholder {

    public GetVariablePlaceholder() {
        super("getvariable");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String name = dps.values.get("name");
        if (name != null) {
            String val = VariableHandler.getVariable(name);
            if (val == null) {
                val = "0";
            }
            return val;
        }
        return null;
    }

    @Override
    public @Nullable List<String> getValueNames() {
        List<String> l = new ArrayList<>();
        l.add("name");
        return l;
    }

    @Override
    public String getDisplayName() {
        return I18n.get("fancymenu.helper.placeholder.get_variable");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.helper.placeholder.get_variable.desc")));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.fancymenu.editor.dynamicvariabletextfield.categories.advanced");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        DeserializedPlaceholderString dps = new DeserializedPlaceholderString();
        dps.placeholder = this.getIdentifier();
        dps.values.put("name", "some_variable");
        return dps;
    }

}
