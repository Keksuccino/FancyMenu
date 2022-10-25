package de.keksuccino.fancymenu.menu.placeholder.v2.placeholders;

import de.keksuccino.fancymenu.menu.placeholder.v2.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.menu.placeholder.v2.Placeholder;

import java.util.List;

public class TestPlaceholder1 extends Placeholder {

    public TestPlaceholder1() {
        super("test_placeholder_1");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString ps) {
        String value1 = ps.values.get("value_1");
        String value2 = ps.values.get("value_2");
        return "This is value 1: " + value1 + " and this is value 2: " + value2 + "!";
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public List<String> getDescription() {
        return null;
    }

    @Override
    public String getCategory() {
        return null;
    }
}
