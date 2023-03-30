
package de.keksuccino.fancymenu.menu.placeholder.v2.placeholders.gui;

import de.keksuccino.fancymenu.menu.placeholder.v2.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.menu.placeholder.v2.Placeholder;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import net.minecraft.client.Minecraft;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Arrays;
import java.util.List;

public class GuiScalePlaceholder extends Placeholder {

    public GuiScalePlaceholder() {
        super("guiscale");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
       return "" + Minecraft.getInstance().getWindow().getGuiScale();
    }

    @Override
    public  List<String> getValueNames() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return Locals.localize("fancymenu.helper.placeholder.guiscale");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(StringUtils.splitLines(Locals.localize("fancymenu.helper.placeholder.guiscale.desc"), "%n%"));
    }

    @Override
    public String getCategory() {
        return Locals.localize("fancymenu.helper.ui.dynamicvariabletextfield.categories.gui");
    }

    @Override
    public @Nonnull DeserializedPlaceholderString getDefaultPlaceholderString() {
        DeserializedPlaceholderString dps = new DeserializedPlaceholderString();
        dps.placeholder = this.getIdentifier();
        return dps;
    }

}
