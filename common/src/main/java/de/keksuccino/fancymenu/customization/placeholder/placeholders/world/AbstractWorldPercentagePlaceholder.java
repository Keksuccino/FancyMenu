package de.keksuccino.fancymenu.customization.placeholder.placeholders.world;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;

//TODO übernehmen
public abstract class AbstractWorldPercentagePlaceholder extends AbstractWorldPlaceholder {

    private static final Logger LOGGER = LogManager.getLogger();

    public AbstractWorldPercentagePlaceholder(@NotNull String identifier) {
        super(identifier);
    }

    protected abstract float getCurrentFloatValue(@NotNull LocalPlayer player, @NotNull ClientLevel level);

    protected abstract float getMaxFloatValue(@NotNull LocalPlayer player, @NotNull ClientLevel level);

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {

        try {
            ClientLevel level = this.getLevel();
            LocalPlayer player = this.getPlayer();
            if ((level != null) && (player != null)) {
                if (this.getMaxFloatValue(player, level) == 0.0F) return "0";
                if (this.getCurrentFloatValue(player, level) == 0.0F) return "0";
                float f = (this.getCurrentFloatValue(player, level) / this.getMaxFloatValue(player, level)) * 100.0F;
                return "" + ((int)f);
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to get replacement for '" + this.getIdentifier() + "' placeholder.", ex);
        }

        return "0";

    }

    @Override
    public @Nullable List<String> getValueNames() {
        return null;
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        return new DeserializedPlaceholderString(this.getIdentifier(), null, "");
    }

}
