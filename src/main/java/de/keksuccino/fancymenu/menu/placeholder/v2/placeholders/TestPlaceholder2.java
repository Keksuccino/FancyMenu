package de.keksuccino.fancymenu.menu.placeholder.v2.placeholders;

import de.keksuccino.fancymenu.menu.placeholder.v2.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.menu.placeholder.v2.Placeholder;

import java.util.List;

public class TestPlaceholder2 extends Placeholder {

    public TestPlaceholder2() {
        super("test_placeholder_2");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString ps) {
        return "" + System.currentTimeMillis();
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
