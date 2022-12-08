package de.keksuccino.fancymenu.menu.placeholder.v2.placeholders.test;

import de.keksuccino.fancymenu.menu.placeholder.v2.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.menu.placeholder.v2.Placeholder;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class TestPlaceholder1 extends Placeholder {

    public TestPlaceholder1() {
        super("test_placeholder_1");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        return "[This is value_1: " + dps.values.get("value_1") + " | and this is value_2: " + dps.values.get("value_2") + "!]";
    }

    @Override
    public @Nullable List<String> getValueNames() {
        List<String> l = new ArrayList<>();
        l.add("value_1");
        l.add("value_2");
        return l;
    }

    @Override
    public @Nonnull String getDisplayName() {
        return "Test Placeholder 1";
    }

    @Override
    public @Nullable List<String> getDescription() {
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
        dps.values.put("value_1", "val");
        dps.values.put("value_2", "val");
        return dps;
    }

}
