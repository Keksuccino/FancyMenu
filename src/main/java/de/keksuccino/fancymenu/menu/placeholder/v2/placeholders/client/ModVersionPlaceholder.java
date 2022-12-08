//TODO übernehmen
package de.keksuccino.fancymenu.menu.placeholder.v2.placeholders.client;

import de.keksuccino.fancymenu.menu.placeholder.v2.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.menu.placeholder.v2.Placeholder;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
        if (FabricLoader.getInstance().isModLoaded(modid)) {
            Optional<ModContainer> o = FabricLoader.getInstance().getModContainer(modid);
            if (o.isPresent()) {
                ModContainer c = o.get();
                return c.getMetadata().getVersion().getFriendlyString();
            }
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
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        Map<String, String> m = new HashMap<>();
        m.put("modid", "some_mod_id");
        return DeserializedPlaceholderString.build(this.getIdentifier(), m);
    }

}
