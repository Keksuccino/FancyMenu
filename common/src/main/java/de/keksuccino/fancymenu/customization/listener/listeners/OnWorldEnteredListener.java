package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class OnWorldEnteredListener extends AbstractListener {
    @Nullable
    private String cachedWorldName;
    @Nullable
    private String cachedWorldSavePath;
    @Nullable
    private String cachedWorldDifficulty;
    @Nullable
    private String cachedWorldCheatsAllowed;
    @Nullable
    private String cachedWorldIconPath;
    @Nullable
    private Boolean cachedWorldIsFirstJoin;

    public OnWorldEnteredListener() {
        super("world_entered");
    }

    public void onWorldEntered(@Nullable String worldName,
                               @Nullable String worldSavePath,
                               @Nullable String worldDifficultyKey,
                               @Nullable String cheatsAllowed,
                               @Nullable String worldIconPath,
                               boolean isFirstJoin) {
        this.cachedWorldName = sanitize(worldName);
        this.cachedWorldSavePath = sanitize(worldSavePath);
        this.cachedWorldDifficulty = sanitize(worldDifficultyKey);
        this.cachedWorldCheatsAllowed = sanitize(cheatsAllowed);
        this.cachedWorldIconPath = sanitize(worldIconPath);
        this.cachedWorldIsFirstJoin = isFirstJoin;
        this.notifyAllInstances();
    }

    @Override
    protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {
        list.add(new CustomVariable("world_name", () -> valueOrError(this.cachedWorldName)));
        list.add(new CustomVariable("world_save_path", () -> valueOrError(this.cachedWorldSavePath)));
        list.add(new CustomVariable("world_difficulty", () -> valueOrError(this.cachedWorldDifficulty)));
        list.add(new CustomVariable("world_cheats_allowed", () -> booleanString(this.cachedWorldCheatsAllowed)));
        list.add(new CustomVariable("world_icon_path", () -> valueOrError(this.cachedWorldIconPath)));
        list.add(new CustomVariable("world_is_first_join", () -> this.cachedWorldIsFirstJoin != null ? Boolean.toString(this.cachedWorldIsFirstJoin) : "false"));
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_world_entered");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_world_entered.desc"));
    }

    private static String sanitize(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String valueOrError(@Nullable String value) {
        return value != null ? value : "ERROR";
    }

    private static String booleanString(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return "false";
        }
        return Boolean.toString(Boolean.parseBoolean(value));
    }
}
