
package de.keksuccino.fancymenu.menu.placeholder.v2.placeholders.client;

import de.keksuccino.fancymenu.menu.placeholder.v2.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.menu.placeholder.v2.Placeholder;
import de.keksuccino.konkrete.Konkrete;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

//This is the same as LoadedModsPlaceholder in Fabric, since Fabric does not support disabling mods, so both will always return the same.
//It's basically just for Fabric-Forge compatibility.
public class TotalModsPlaceholder extends Placeholder {

    public TotalModsPlaceholder() {
        super("totalmods");
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
            return FabricLoader.getInstance().getAllMods().size() + i;
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
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        DeserializedPlaceholderString dps = new DeserializedPlaceholderString();
        dps.placeholder = this.getIdentifier();
        return dps;
    }

}
