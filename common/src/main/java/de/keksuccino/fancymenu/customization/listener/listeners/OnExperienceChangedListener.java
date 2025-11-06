package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class OnExperienceChangedListener extends AbstractListener {

    @Nullable
    private String cachedNewExperience;
    @Nullable
    private String cachedOldExperience;
    @Nullable
    private String cachedIsLevelUp;

    public OnExperienceChangedListener() {
        super("experience_changed");
    }

    public void onExperienceChanged(int oldExperience, int newExperience, boolean isLevelUp) {
        this.cachedOldExperience = Integer.toString(Math.max(oldExperience, 0));
        this.cachedNewExperience = Integer.toString(Math.max(newExperience, 0));
        this.cachedIsLevelUp = Boolean.toString(isLevelUp);
        this.notifyAllInstances();
    }

    @Override
    protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {
        list.add(new CustomVariable("new_experience_amount", () -> this.cachedNewExperience != null ? this.cachedNewExperience : "0"));
        list.add(new CustomVariable("old_experience_amount", () -> this.cachedOldExperience != null ? this.cachedOldExperience : "0"));
        list.add(new CustomVariable("is_level_up", () -> this.cachedIsLevelUp != null ? this.cachedIsLevelUp : "false"));
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_experience_changed");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_experience_changed.desc"));
    }
}
