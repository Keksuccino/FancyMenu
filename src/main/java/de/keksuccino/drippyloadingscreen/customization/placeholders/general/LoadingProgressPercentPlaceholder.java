
package de.keksuccino.drippyloadingscreen.customization.placeholders.general;

import de.keksuccino.drippyloadingscreen.mixin.MixinCache;
import de.keksuccino.fancymenu.menu.placeholder.v2.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.menu.placeholder.v2.Placeholder;
import de.keksuccino.konkrete.input.StringUtils;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class LoadingProgressPercentPlaceholder extends Placeholder {

    public LoadingProgressPercentPlaceholder() {
        super("drippy_gl_percent");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        return "" + (int)(MixinCache.cachedCurrentLoadingScreenProgress * 100.0F);
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return I18n.get("drippyloadingscreen.placeholders.general.loading_progress_percent");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(StringUtils.splitLines(I18n.get("drippyloadingscreen.placeholders.general.loading_progress_percent.desc"), "\n"));
    }

    @Override
    public String getCategory() {
        return "Drippy Loading Screen";
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        DeserializedPlaceholderString dps = new DeserializedPlaceholderString();
        dps.placeholder = this.getIdentifier();
        return dps;
    }

}
