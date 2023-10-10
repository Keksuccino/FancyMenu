package de.keksuccino.fancymenu.customization.action;

import org.jetbrains.annotations.NotNull;
import java.util.Map;
import java.util.function.Supplier;

public interface ValuePlaceholderHolder {

    public static final String VALUE_PLACEHOLDER_PREFIX = "$$";

    void addValuePlaceholder(@NotNull String placeholder, @NotNull Supplier<String> replaceWithSupplier);

    @NotNull
    Map<String, Supplier<String>> getValuePlaceholders();

}
