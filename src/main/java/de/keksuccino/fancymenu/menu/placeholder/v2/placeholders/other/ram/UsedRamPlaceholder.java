
package de.keksuccino.fancymenu.menu.placeholder.v2.placeholders.other.ram;

import de.keksuccino.fancymenu.menu.placeholder.v2.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.menu.placeholder.v2.Placeholder;
import de.keksuccino.konkrete.localization.Locals;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;

public class UsedRamPlaceholder extends Placeholder {

    public UsedRamPlaceholder() {
        super("usedram");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        long j = Runtime.getRuntime().totalMemory();
        long k = Runtime.getRuntime().freeMemory();
        long l = j - k;
        return "" + bytesToMb(l);
    }

    private static long bytesToMb(long bytes) {
        return bytes / 1024L / 1024L;
    }

    @Override
    public  List<String> getValueNames() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return Locals.localize("fancymenu.helper.ui.dynamicvariabletextfield.variables.usedram");
    }

    @Override
    public List<String> getDescription() {
        return null;
    }

    @Override
    public String getCategory() {
        return Locals.localize("fancymenu.helper.ui.dynamicvariabletextfield.categories.other");
    }

    @Override
    public @Nonnull DeserializedPlaceholderString getDefaultPlaceholderString() {
        DeserializedPlaceholderString dps = new DeserializedPlaceholderString();
        dps.placeholder = this.getIdentifier();
        return dps;
    }

}
