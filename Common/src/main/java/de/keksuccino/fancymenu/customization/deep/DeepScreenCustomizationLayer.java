package de.keksuccino.fancymenu.customization.deep;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;

public class DeepScreenCustomizationLayer {

    private static final Logger LOGGER = LogManager.getLogger();

    protected String targetScreenClassPath;
    protected final Map<String, DeepElementBuilder<?,?,?>> builders = new LinkedHashMap<>();

    public DeepScreenCustomizationLayer(@NotNull String targetScreenClassPath) {
        this.targetScreenClassPath = targetScreenClassPath;
    }

    public void registerBuilder(@NotNull DeepElementBuilder<?,?,?> builder) {
        Objects.requireNonNull(builder.getIdentifier(), "[FANCYMENU] Failed to register DeepElementBuilder! Identifier was NULL!");
        if (builders.containsKey(builder.getIdentifier())) {
            LOGGER.warn("[FANCYMENU] DeepElementBuilder with identifier '" + builder.getIdentifier() + "' already registered! Overriding builder!");
        }
        this.builders.put(builder.getIdentifier(), builder);
    }

    public void unregisterBuilder(@NotNull String identifier) {
        this.builders.remove(identifier);
    }

    @NotNull
    public List<DeepElementBuilder<?,?,?>> getBuilders() {
        return new ArrayList<>(this.builders.values());
    }

    @Nullable
    public DeepElementBuilder<?,?,?> getBuilder(@NotNull String identifier) {
        return this.builders.get(identifier);
    }

    public boolean hasBuilder(@NotNull String identifier) {
        return builders.containsKey(identifier);
    }

    @NotNull
    public String getTargetScreenClassPath() {
        return this.targetScreenClassPath;
    }

}
