package de.keksuccino.fancymenu.customization.placeholder.placeholders.world;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.language.I18n;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public class WorldDifficultyPlaceholder extends Placeholder {

    private static final Logger LOGGER = LogManager.getLogger();

    public WorldDifficultyPlaceholder() {
        super("world_difficulty");
    }

    @Override
    public boolean canRunAsync() {
        return false;
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {

        try {
            ClientLevel level = Minecraft.getInstance().level;
            if (level != null) {
                return level.getDifficulty().getKey();
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to get replacement for '" + this.getIdentifier() + "' placeholder.", ex);
        }

        return "";

    }

    @Override
    public @Nullable List<String> getValueNames() {
        return null;
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.placeholders.world.difficulty");
    }

    @Override
    public List<String> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.placeholders.world.difficulty.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.placeholders.categories.world");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        return new DeserializedPlaceholderString(this.getIdentifier(), null, "");
    }

}
