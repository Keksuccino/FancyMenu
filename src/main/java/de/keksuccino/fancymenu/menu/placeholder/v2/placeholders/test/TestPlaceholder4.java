package de.keksuccino.fancymenu.menu.placeholder.v2.placeholders.test;

import de.keksuccino.fancymenu.menu.placeholder.v2.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.menu.placeholder.v2.Placeholder;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class TestPlaceholder4 extends Placeholder {

    public TestPlaceholder4() {
        super("test_placeholder_4");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        return " [This is value_3: " + dps.values.get("value_3") + " | and this is value_4: " + dps.values.get("value_4") + "!]";
    }

    @Override
    public  List<String> getValueNames() {
        List<String> l = new ArrayList<>();
        l.add("value_3");
        l.add("value_4");
        return l;
    }

    @Override
    public @Nonnull String getDisplayName() {
        return "Test Placeholder 4";
    }

    @Override
    public  List<String> getDescription() {
        return null;
    }

    @Override
    public String getCategory() {
        return null;
    }

    @Override
    public @Nonnull DeserializedPlaceholderString getDefaultPlaceholderString() {
        DeserializedPlaceholderString dps = new DeserializedPlaceholderString();
        dps.placeholder = this.getIdentifier();
        dps.values.put("value_3", "val");
        dps.values.put("value_4", "val");
        return dps;
    }

}
