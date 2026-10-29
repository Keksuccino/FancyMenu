package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.Locale;

public class OnAchievementUnlockedListener extends AbstractListener {

    @Nullable
    private String cachedAchievementKey;
    @Nullable
    private String cachedAchievementLabel;
    @Nullable
    private String cachedAchievementDescription;
    @Nullable
    private String cachedAchievementIconItemKey;
    @Nullable
    private String cachedAchievementType;

    public OnAchievementUnlockedListener() {
        super("achievement_unlocked");
    }

    public void onAchievementUnlocked(@NotNull Identifier achievementKey, @NotNull DisplayInfo display) {
        if (!this.hasInstancesListening()) {
            return;
        }
        this.cachedAchievementKey = achievementKey.toString();
        this.cachedAchievementLabel = display.getTitle().getString();
        this.cachedAchievementDescription = display.getDescription().getString();
        this.cachedAchievementIconItemKey = BuiltInRegistries.ITEM.getKey(display.getIcon().item().value()).toString();
        this.cachedAchievementType = getTypeName(display.getType());
        this.notifyAllInstances();
    }

    @NotNull
    private static String getTypeName(@NotNull AdvancementType type) {
        return switch (type) {
            // Minecraft calls this type "task" internally, but shows it as "Advancement" in-game
            case TASK -> "advancement";
            case GOAL -> "goal";
            case CHALLENGE -> "challenge";
            // Types added by other mods
            default -> type.name().toLowerCase(Locale.ROOT);
        };
    }

    @Override
    protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {
        list.add(new CustomVariable("achievement_key", () -> (this.cachedAchievementKey != null) ? this.cachedAchievementKey : ""));
        list.add(new CustomVariable("achievement_label", () -> (this.cachedAchievementLabel != null) ? this.cachedAchievementLabel : ""));
        list.add(new CustomVariable("achievement_description", () -> (this.cachedAchievementDescription != null) ? this.cachedAchievementDescription : ""));
        list.add(new CustomVariable("achievement_icon_item_key", () -> (this.cachedAchievementIconItemKey != null) ? this.cachedAchievementIconItemKey : ""));
        list.add(new CustomVariable("achievement_type", () -> (this.cachedAchievementType != null) ? this.cachedAchievementType : ""));
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_achievement_unlocked");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_achievement_unlocked.desc"));
    }

}
