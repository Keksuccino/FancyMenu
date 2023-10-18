
package de.keksuccino.drippyloadingscreen.customization.placeholders.general;

import com.mojang.blaze3d.platform.GlUtil;
import de.keksuccino.fancymenu.menu.placeholder.v2.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.menu.placeholder.v2.Placeholder;
import de.keksuccino.konkrete.input.StringUtils;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

//TODO übernehmen
public class GpuInfoPlaceholder extends Placeholder {

    public GpuInfoPlaceholder() {
        super("drippy_gpu_info");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        return GlUtil.getRenderer();
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return I18n.get("drippyloadingscreen.placeholders.general.gpu_info");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(StringUtils.splitLines(I18n.get("drippyloadingscreen.placeholders.general.gpu_info.desc"), "\n"));
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
