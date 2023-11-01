package de.keksuccino.fancymenu.util.resources;

import de.keksuccino.fancymenu.util.enums.LocalizedCycleEnum;
import de.keksuccino.fancymenu.util.input.TextValidators;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Objects;

public enum ResourceSourceType implements LocalizedCycleEnum<ResourceSourceType> {

    LOCATION("location"),
    LOCAL("local"),
    WEB("web");

    private final String name;

    ResourceSourceType(@NotNull String name) {
        this.name = name;
    }

    @NotNull
    public String getSourcePrefix() {
        return "[source:" + this.name + "]";
    }

    public static boolean hasSourcePrefix(@NotNull String resourceSource) {
        if (resourceSource.startsWith(LOCATION.getSourcePrefix())) return true;
        if (resourceSource.startsWith(LOCAL.getSourcePrefix())) return true;
        if (resourceSource.startsWith(WEB.getSourcePrefix())) return true;
        return false;
    }

    @NotNull
    public static String getWithoutSourcePrefix(@NotNull String resourceSource) {
        resourceSource = resourceSource.replace(LOCATION.getSourcePrefix(), "");
        resourceSource = resourceSource.replace(LOCAL.getSourcePrefix(), "");
        resourceSource = resourceSource.replace(WEB.getSourcePrefix(), "");
        return resourceSource;
    }

    /**
     * Tries to find the {@link ResourceSourceType} of a resource's source.
     *
     * @param resourceSource Can be a URL to a web source, a path to a local source or a resource location (namespace:path).
     */
    @NotNull
    public static ResourceSourceType getSourceTypeOf(@NotNull String resourceSource) {

        Objects.requireNonNull(resourceSource);

        //Check for source prefix
        if (resourceSource.startsWith(LOCAL.getSourcePrefix())) return LOCAL;
        if (resourceSource.startsWith(WEB.getSourcePrefix())) return WEB;
        if (resourceSource.startsWith(LOCATION.getSourcePrefix())) return LOCATION;

        //If no prefix, try to get source type the classic way
        if (TextValidators.BASIC_URL_TEXT_VALIDATOR.get(getWithoutSourcePrefix(resourceSource))) return WEB;
        if (resourceSource.contains(":")) {
            if (ResourceLocation.tryParse(getWithoutSourcePrefix(resourceSource)) != null) return LOCATION;
        }

        //Fallback type and no-prefix return, if source is not WEB and not LOCATION
        return LOCAL;

    }

    @Override
    public @NotNull Style getValueComponentStyle() {
        return WARNING_TEXT_STYLE.get();
    }

    @Override
    public @NotNull String getLocalizationKeyBase() {
        return "fancymenu.resources.source_type";
    }

    @Override
    public @NotNull String getName() {
        return this.name;
    }

    @Override
    public @NotNull ResourceSourceType[] getValues() {
        return ResourceSourceType.values();
    }

    @Override
    public @Nullable ResourceSourceType getByNameInternal(@NotNull String name) {
        return getByName(name);
    }

    @Nullable
    public static ResourceSourceType getByName(@NotNull String name) {
        for (ResourceSourceType t : ResourceSourceType.values()) {
            if (t.name.equals(name)) return t;
        }
        return null;
    }

}
