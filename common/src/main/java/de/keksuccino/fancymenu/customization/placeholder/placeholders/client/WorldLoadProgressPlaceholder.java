package de.keksuccino.fancymenu.customization.placeholder.placeholders.client;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinLevelLoadingScreen;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinProgressScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Arrays;
import java.util.List;

public class WorldLoadProgressPlaceholder extends Placeholder {

    public WorldLoadProgressPlaceholder() {
        super("world_load_progress");
    }

    @Override
    public boolean canRunAsync() {
        return false;
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        Screen s = Minecraft.getInstance().screen;
        if (s instanceof ProgressScreen p) {
            return "" + ((IMixinProgressScreen)p).getProgressFancyMenu();
        }
        if (s instanceof LevelLoadingScreen l) {
            return "" + (int)(((IMixinLevelLoadingScreen)l).get_loadTracker_FancyMenu().serverProgress() * 100.0F);
        }
        return "0";
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return null;
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.placeholders.world_load_progress");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("fancymenu.placeholders.world_load_progress.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.requirements.categories.client");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        DeserializedPlaceholderString dps = new DeserializedPlaceholderString();
        dps.placeholderIdentifier = this.getIdentifier();
        return dps;
    }

}
