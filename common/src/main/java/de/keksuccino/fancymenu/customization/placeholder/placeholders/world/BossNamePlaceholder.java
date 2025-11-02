package de.keksuccino.fancymenu.customization.placeholder.placeholders.world;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinBossHealthOverlay;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.SerializationUtils;
import de.keksuccino.fancymenu.util.rendering.text.TextFormattingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.widget.component.ComponentSerialization;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;

public class BossNamePlaceholder extends Placeholder {

    public BossNamePlaceholder() {
        super("boss_name");
    }

    @Override
    public boolean canRunAsync() {
        return false;
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        LocalPlayer player = Minecraft.getInstance().player;
        ClientLevel level = Minecraft.getInstance().level;
        int index = SerializationUtils.deserializeNumber(Integer.class, 0, dps.values.get("boss_index"));
        boolean asJson = SerializationUtils.deserializeBoolean(false, dps.values.get("as_json"));
        if ((player != null) && (level != null)) {
            Map<UUID, LerpingBossEvent> bosses = ((IMixinBossHealthOverlay)Minecraft.getInstance().gui.getBossOverlay()).get_events_FancyMenu(); // this is a linked hash map, so indexes should work fine
            if (bosses.size() >= (index + 1)) {
                int i = 0;
                for (Map.Entry<UUID, LerpingBossEvent> m : bosses.entrySet()) {
                    if (index == i) {
                        if (asJson) {
                            return ComponentSerialization.Serializer.toJson(m.getValue().getName(), level.registryAccess());
                        } else {
                            return TextFormattingUtils.convertComponentToString(m.getValue().getName());
                        }
                    }
                    i++;
                }
            }
        }
        return "";
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return List.of("boss_index", "as_json");
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.placeholders.world.boss_name");
    }

    @Override
    public @Nullable List<String> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.placeholders.world.boss_name.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.placeholders.categories.world");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        HashMap<String, String> values = new LinkedHashMap<>();
        values.put("boss_index", "0");
        values.put("as_json", "false");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }

}
