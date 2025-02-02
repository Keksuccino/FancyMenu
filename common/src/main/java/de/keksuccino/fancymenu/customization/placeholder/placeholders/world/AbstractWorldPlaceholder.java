package de.keksuccino.fancymenu.customization.placeholder.placeholders.world;

import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public abstract class AbstractWorldPlaceholder extends Placeholder {

    public AbstractWorldPlaceholder(@NotNull String identifier) {
        super(identifier);
    }

    @Nullable
    protected LocalPlayer getPlayer() {
        return Minecraft.getInstance().player;
    }

    @Nullable
    protected ClientLevel getLevel() {
        return Minecraft.getInstance().level;
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return List.of();
    }

    @NotNull
    protected abstract String getLocalizationBase();

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get(this.getLocalizationBase());
    }

    @Override
    public List<String> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedStringLines(this.getLocalizationBase() + ".desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.placeholders.categories.world");
    }

}
