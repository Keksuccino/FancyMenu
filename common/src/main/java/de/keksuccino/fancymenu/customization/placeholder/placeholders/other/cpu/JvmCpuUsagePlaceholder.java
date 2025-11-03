package de.keksuccino.fancymenu.customization.placeholder.placeholders.other.cpu;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.MathUtils;
import de.keksuccino.fancymenu.util.PerformanceUtils;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class JvmCpuUsagePlaceholder extends Placeholder {

    public JvmCpuUsagePlaceholder() {
        super("jvmcpu");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        double d = PerformanceUtils.getJvmCpuUsage();
        if (d < 0.0D) d = 0.0D;
        d = d * 100.0D;
        d = MathUtils.round(d, 1);
        return "" + d;
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return null;
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.placeholders.jvm_cpu_usage");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("fancymenu.placeholders.jvm_cpu_usage.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.requirements.categories.other");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        return new DeserializedPlaceholderString(this.getIdentifier(), null, "");
    }

}
