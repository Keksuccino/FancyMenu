
package de.keksuccino.fancymenu.menu.placeholder.v2.placeholders.client;

import de.keksuccino.fancymenu.menu.placeholder.v2.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.menu.placeholder.v2.Placeholder;
import de.keksuccino.konkrete.Konkrete;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.ModList;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
            return ModList.get().getMods().size() + i;
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
        return Locals.localize("helper.ui.dynamicvariabletextfield.variables.totalmods");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(StringUtils.splitLines(Locals.localize("helper.ui.dynamicvariabletextfield.variables.totalmods.desc"), "%n%"));
    }

    @Override
    public String getCategory() {
        return Locals.localize("fancymenu.helper.ui.dynamicvariabletextfield.categories.client");
    }

    @Override
    public @Nonnull DeserializedPlaceholderString getDefaultPlaceholderString() {
        DeserializedPlaceholderString dps = new DeserializedPlaceholderString();
        dps.placeholder = this.getIdentifier();
        return dps;
    }

}
