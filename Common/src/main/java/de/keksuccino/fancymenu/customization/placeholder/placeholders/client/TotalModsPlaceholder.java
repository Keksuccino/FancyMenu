package de.keksuccino.fancymenu.customization.placeholder.placeholders.client;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.platform.Services;
import de.keksuccino.konkrete.Konkrete;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class TotalModsPlaceholder extends Placeholder {

    private static final File MOD_DIRECTORY = new File(Minecraft.getInstance().gameDirectory, "mods");

    private static int cachedTotalMods = -10;

    public TotalModsPlaceholder() {
        super("totalmods");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        int loaded = getLoadedMods();
        int total = getTotalMods();
        if (total < loaded) {
            total = loaded;
        }
        return "" + total;
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

    private static int getTotalMods() {
        if (cachedTotalMods == -10) {
            if (MOD_DIRECTORY.exists()) {
                int i = 0;
                for (File f : MOD_DIRECTORY.listFiles()) {
                    if (f.isFile() && f.getName().toLowerCase().endsWith(".jar")) {
                        i++;
                    }
                }
                cachedTotalMods = i+2;
            } else {
                cachedTotalMods = -1;
            }
        }
        return cachedTotalMods;
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return I18n.get("fancymenu.editor.dynamicvariabletextfield.variables.totalmods");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.editor.dynamicvariabletextfield.variables.totalmods.desc")));
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
