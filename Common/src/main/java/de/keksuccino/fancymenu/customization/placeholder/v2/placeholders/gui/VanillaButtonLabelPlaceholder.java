package de.keksuccino.fancymenu.customization.placeholder.v2.placeholders.gui;

import de.keksuccino.fancymenu.customization.button.ButtonData;
import de.keksuccino.fancymenu.customization.button.ButtonMimeHandler;
import de.keksuccino.fancymenu.customization.placeholder.v2.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.v2.Placeholder;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VanillaButtonLabelPlaceholder extends Placeholder {

    private static final Logger LOGGER = LogManager.getLogger();

    public VanillaButtonLabelPlaceholder() {
        super("vanillabuttonlabel");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String buttonLocator = dps.values.get("locator");
        if (buttonLocator != null) {
            ButtonData d = ButtonMimeHandler.getButton(buttonLocator);
            if (d != null) {
                return d.getButton().getMessage().getString();
            }
        }
        return null;
    }

    @Override
    public @Nullable List<String> getValueNames() {
        List<String> l = new ArrayList<>();
        l.add("locator");
        return l;
    }

    @Override
    public String getDisplayName() {
        return Locals.localize("fancymenu.helper.ui.dynamicvariabletextfield.variables.vanillabuttonlabel");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(StringUtils.splitLines(Locals.localize("fancymenu.helper.ui.dynamicvariabletextfield.variables.vanillabuttonlabel.desc"), "%n%"));
    }

    @Override
    public String getCategory() {
        return Locals.localize("fancymenu.helper.ui.dynamicvariabletextfield.categories.gui");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        DeserializedPlaceholderString dps = new DeserializedPlaceholderString();
        dps.placeholder = this.getIdentifier();
        dps.values.put("locator", "some.menu.identifier:505280");
        return dps;
    }

}
