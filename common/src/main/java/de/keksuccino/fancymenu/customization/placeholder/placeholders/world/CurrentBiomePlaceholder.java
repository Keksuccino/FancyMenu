package de.keksuccino.fancymenu.customization.placeholder.placeholders.world;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import net.minecraft.Util;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

public class CurrentBiomePlaceholder extends AbstractWorldPlaceholder {

    public CurrentBiomePlaceholder() {
        super("current_biome");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        ClientLevel level = this.getLevel();
        LocalPlayer player = this.getPlayer();
        if (level == null || player == null) return "";

        Holder<Biome> biomeHolder = level.getBiome(player.blockPosition());
        Optional<ResourceKey<Biome>> biomeKey = biomeHolder.unwrapKey();
        if (biomeKey.isEmpty()) return "";

        ResourceLocation biomeId = biomeKey.get().location();
        String asKeyString = dps.values.get("as_key");
        boolean asKey = true;
        if ((asKeyString != null) && asKeyString.equalsIgnoreCase("false")) {
            asKey = false;
        }

        if (!asKey) {
            String translationKey = Util.makeDescriptionId("biome", biomeId);
            if (I18n.exists(translationKey)) {
                return I18n.get(translationKey);
            }
        }

        return biomeId.toString();
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return List.of("as_key");
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "fancymenu.placeholders.world.current_biome";
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        HashMap<String, String> values = new LinkedHashMap<>();
        values.put("as_key", "true");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }
}

