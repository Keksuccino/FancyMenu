package de.keksuccino.fancymenu.customization.backend.placeholder.v2.placeholders.gui;

import de.keksuccino.fancymenu.customization.backend.placeholder.v2.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.backend.placeholder.v2.Placeholder;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class ScreenHeightPlaceholder extends Placeholder {

    public ScreenHeightPlaceholder() {
        super("guiheight");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        if (Minecraft.getInstance().screen != null) {
            return "" + Minecraft.getInstance().screen.height;
        }
        return null;
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return Locals.localize("fancymenu.helper.ui.dynamicvariabletextfield.variables.guiheight");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(StringUtils.splitLines(Locals.localize("fancymenu.helper.ui.dynamicvariabletextfield.variables.guiheight.desc"), "%n%"));
    }

    @Override
    public String getCategory() {
        return Locals.localize("fancymenu.helper.ui.dynamicvariabletextfield.categories.gui");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        DeserializedPlaceholderString dps = new DeserializedPlaceholderString();
        dps.placeholder = this.getIdentifier();
        return dps;
    }

}
