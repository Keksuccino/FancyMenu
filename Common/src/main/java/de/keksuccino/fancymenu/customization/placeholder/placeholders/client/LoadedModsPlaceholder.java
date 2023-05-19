package de.keksuccino.fancymenu.customization.placeholder.placeholders.client;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.platform.Services;
import de.keksuccino.konkrete.Konkrete;
import de.keksuccino.fancymenu.utils.LocalizationUtils;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class LoadedModsPlaceholder extends Placeholder {

    public LoadedModsPlaceholder() {
        super("loadedmods");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        return "" + getLoadedMods();
    }

    private static int getLoadedMods() {
        try {
            int i = 0;
            if (Konkrete.isOptifineLoaded) {
                i++;
            }
            return Services.PLATFORM.getLoadedModIds().size() + i;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return I18n.get("fancymenu.editor.dynamicvariabletextfield.variables.loadedmods");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.editor.dynamicvariabletextfield.variables.loadedmods.desc")));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.fancymenu.editor.dynamicvariabletextfield.categories.client");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        DeserializedPlaceholderString dps = new DeserializedPlaceholderString();
        dps.placeholder = this.getIdentifier();
        return dps;
    }

}
