package de.keksuccino.fancymenu.customization.placeholder.placeholders.gui;

import de.keksuccino.fancymenu.customization.widget.WidgetMeta;
import de.keksuccino.fancymenu.customization.widget.ButtonMimeHandler;
import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.utils.LocalizationUtils;
import net.minecraft.client.resources.language.I18n;
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
            WidgetMeta d = ButtonMimeHandler.getButton(buttonLocator);
            if (d != null) {
                return d.getWidget().getMessage().getString();
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
        return I18n.get("fancymenu.fancymenu.editor.dynamicvariabletextfield.variables.vanillabuttonlabel");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.fancymenu.editor.dynamicvariabletextfield.variables.vanillabuttonlabel.desc")));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.fancymenu.editor.dynamicvariabletextfield.categories.gui");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        DeserializedPlaceholderString dps = new DeserializedPlaceholderString();
        dps.placeholder = this.getIdentifier();
        dps.values.put("locator", "some.menu.identifier:505280");
        return dps;
    }

}
