package de.keksuccino.fancymenu.customization.placeholder.placeholders.world;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinGui;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.SerializationUtils;
import de.keksuccino.fancymenu.util.rendering.text.TextFormattingUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;

//TODO übernehmen
public class CurrentTitlePlaceholder extends Placeholder {

    public CurrentTitlePlaceholder() {
        super("current_title");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        LocalPlayer player = Minecraft.getInstance().player;
        ClientLevel level = Minecraft.getInstance().level;
        boolean isSubtitle = SerializationUtils.deserializeBoolean(false, dps.values.get("is_subtitle"));
        boolean asJson = SerializationUtils.deserializeBoolean(false, dps.values.get("as_json"));
        if ((player != null) && (level != null)) {
            Component component = isSubtitle ? ((IMixinGui)Minecraft.getInstance().gui).get_subtitle_FancyMenu() : ((IMixinGui)Minecraft.getInstance().gui).get_title_FancyMenu();
            if (component != null) {
                if (asJson) {
                    return Component.Serializer.toJson(component, level.registryAccess());
                } else {
                    return TextFormattingUtils.textComponentToString(component);
                }
            }
        }
        return "";
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return List.of("is_subtitle", "as_json");
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.placeholders.world.current_title");
    }

    @Override
    public @Nullable List<String> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.placeholders.world.current_title.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.placeholders.categories.world");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        HashMap<String, String> values = new LinkedHashMap<>();
        values.put("is_subtitle", "false");
        values.put("as_json", "false");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }

}
