package de.keksuccino.fancymenu.customization.placeholder.placeholders.test;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    public @NotNull String getDisplayName() {
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
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        DeserializedPlaceholderString dps = new DeserializedPlaceholderString();
        dps.placeholder = this.getIdentifier();
        return dps;
    }

}
