package de.keksuccino.fancymenu.menu.placeholder.v2.placeholders.test;

import de.keksuccino.fancymenu.menu.placeholder.v2.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.menu.placeholder.v2.Placeholder;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;

public class TestPlaceholder3 extends Placeholder {

    public TestPlaceholder3() {
        super("test_placeholder_3");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        return "" + System.currentTimeMillis();
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return null;
    }

    @Override
    public @Nonnull String getDisplayName() {
        return "Test Placeholder 3";
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
        return dps;
    }

}
