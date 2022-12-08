//TODO übernehmen
package de.keksuccino.fancymenu.menu.placeholder.v2.placeholders.client;

import de.keksuccino.fancymenu.menu.placeholder.v2.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.menu.placeholder.v2.Placeholder;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.*;

public class ModVersionPlaceholder extends Placeholder {

    public ModVersionPlaceholder() {
        super("modversion");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        if (!dps.values.containsKey("modid")) {
            return null;
        }
        return getModVersion(dps.values.get("modid"));
    }

    private String getModVersion(String modid) {
        if (Loader.isModLoaded(modid)) {
            ModContainer c = getModContainerById(modid);
            if (c != null) {
                return c.getVersion();
            }
        }
        return null;
    }

    private static ModContainer getModContainerById(String modid) {
        try {
            for (ModContainer c : Loader.instance().getActiveModList()) {
                if (c.getModId().equals(modid)) {
                    return c;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public @Nullable List<String> getValueNames() {
        List<String> l = new ArrayList<>();
        l.add("modid");
        return l;
    }

    @Override
    public String getDisplayName() {
        return Locals.localize("helper.ui.dynamicvariabletextfield.variables.modversion");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(StringUtils.splitLines(Locals.localize("helper.ui.dynamicvariabletextfield.variables.modversion.desc"), "%n%"));
    }

    @Override
    public String getCategory() {
        return Locals.localize("fancymenu.helper.ui.dynamicvariabletextfield.categories.client");
    }

    @Override
    public @Nonnull DeserializedPlaceholderString getDefaultPlaceholderString() {
        Map<String, String> m = new HashMap<>();
        m.put("modid", "some_mod_id");
        return DeserializedPlaceholderString.build(this.getIdentifier(), m);
    }

}
