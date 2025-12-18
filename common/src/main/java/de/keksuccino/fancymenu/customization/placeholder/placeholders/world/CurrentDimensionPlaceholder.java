package de.keksuccino.fancymenu.customization.placeholder.placeholders.world;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public class CurrentDimensionPlaceholder extends AbstractWorldPlaceholder {

    public CurrentDimensionPlaceholder() {
        super("current_dimension");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        ClientLevel level = this.getLevel();
        if (level == null) return "";

        ResourceLocation dimensionId = level.dimension().location();
        String asKeyString = dps.values.get("as_key");
        boolean asKey = true;
        if ((asKeyString != null) && asKeyString.equalsIgnoreCase("false")) {
            asKey = false;
        }

        if (!asKey) {
            // Vanilla doesn't ship dimension name translations in the language files,
            // so we provide our own for the vanilla dimensions.
            if ("minecraft".equals(dimensionId.getNamespace())) {
                if ("overworld".equals(dimensionId.getPath()) || "the_nether".equals(dimensionId.getPath()) || "the_end".equals(dimensionId.getPath())) {
                    String fancyMenuKey = "fancymenu.dimensions.minecraft." + dimensionId.getPath();
                    if (I18n.exists(fancyMenuKey)) {
                        return I18n.get(fancyMenuKey);
                    }
                }
            }

            // Fallback: if some environment provides dimension translations, use them.
            String fallbackTranslationKey = "dimension." + dimensionId.getNamespace() + "." + dimensionId.getPath();
            if (I18n.exists(fallbackTranslationKey)) {
                return I18n.get(fallbackTranslationKey);
            }
        }

        return dimensionId.toString();
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return List.of("as_key");
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "fancymenu.placeholders.world.current_dimension";
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        HashMap<String, String> values = new LinkedHashMap<>();
        values.put("as_key", "true");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }
}
