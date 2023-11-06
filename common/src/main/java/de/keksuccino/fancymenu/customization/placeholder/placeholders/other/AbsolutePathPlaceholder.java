package de.keksuccino.fancymenu.customization.placeholder.placeholders.other;

import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.konkrete.input.StringUtils;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.util.*;

public class AbsolutePathPlaceholder extends Placeholder {

    public AbsolutePathPlaceholder() {
        super("absolute_path");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String path = dps.values.get("short_path");
        if (path != null) {
            path = StringUtils.convertFormatCodes(path, "§", "&");
            if (ScreenCustomization.isExistingGameDirectoryPath(path)) {
                return ScreenCustomization.getAbsoluteGameDirectoryPath(path);
            } else {
                return new File(path).getAbsolutePath();
            }
        }
        return null;
    }

    @Override
    public @Nullable List<String> getValueNames() {
        List<String> l = new ArrayList<>();
        l.add("short_path");
        return l;
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.helper.placeholder.absolute_path");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("fancymenu.helper.placeholder.absolute_path.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.fancymenu.editor.dynamicvariabletextfield.categories.other");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        DeserializedPlaceholderString dps = new DeserializedPlaceholderString();
        dps.placeholderIdentifier = this.getIdentifier();
        dps.values.put("short_path", "short/path/to/make/absolute");
        return dps;
    }

}
