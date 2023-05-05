package de.keksuccino.fancymenu.customization.placeholder.v2.placeholders.client;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.placeholder.v2.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.v2.Placeholder;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class MinecraftVersionPlaceholder extends Placeholder {

    public MinecraftVersionPlaceholder() {
        super("mcversion");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        return FancyMenu.getMinecraftVersion();
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return Locals.localize("helper.ui.dynamicvariabletextfield.variables.mcversion");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(StringUtils.splitLines(Locals.localize("helper.ui.dynamicvariabletextfield.variables.mcversion.desc"), "%n%"));
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
