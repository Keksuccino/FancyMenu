
package de.keksuccino.drippyloadingscreen.customization.placeholders.general;

import de.keksuccino.fancymenu.menu.placeholder.v2.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.menu.placeholder.v2.Placeholder;
import de.keksuccino.konkrete.input.StringUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

//TODO übernehmen
public class FpsPlaceholder extends Placeholder {

    public FpsPlaceholder() {
        super("drippy_fps");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String s = "0";
        if (Minecraft.getInstance().fpsString.contains(" ")) {
            s = Minecraft.getInstance().fpsString.split("[ ]",2)[0];
        }
        return s;
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return I18n.get("drippyloadingscreen.placeholders.general.fps");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(StringUtils.splitLines(I18n.get("drippyloadingscreen.placeholders.general.fps.desc"), "\n"));
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
