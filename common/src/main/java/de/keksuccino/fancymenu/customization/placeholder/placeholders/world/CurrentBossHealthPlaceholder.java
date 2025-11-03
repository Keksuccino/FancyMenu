package de.keksuccino.fancymenu.customization.placeholder.placeholders.world;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinBossHealthOverlay;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CurrentBossHealthPlaceholder extends Placeholder {

    public CurrentBossHealthPlaceholder() {
        super("current_boss_health");
    }

    @Override
    public boolean canRunAsync() {
        return false;
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        LocalPlayer player = Minecraft.getInstance().player;
        ClientLevel level = Minecraft.getInstance().level;
        String indexString = dps.values.get("boss_index");
        if ((player != null) && (level != null) && MathUtils.isInteger(indexString)) {
            int index = Integer.parseInt(indexString);
            Map<UUID, LerpingBossEvent> bosses = ((IMixinBossHealthOverlay)Minecraft.getInstance().gui.getBossOverlay()).get_events_FancyMenu(); // this is a linked hash map, so indexes should work fine
            if (bosses.size() >= (index + 1)) {
                int i = 0;
                for (Map.Entry<UUID, LerpingBossEvent> m : bosses.entrySet()) {
                    if (index == i) {
                        int progress = (int) (m.getValue().getProgress() * 100.0F);
                        return "" + progress;
                    }
                    i++;
                }
            }
        }
        return "0";
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return List.of("boss_index");
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.placeholders.world.current_boss_health");
    }

    @Override
    public @Nullable List<String> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.placeholders.world.current_boss_health.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.placeholders.categories.world");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        HashMap<String, String> values = new HashMap<>();
        values.put("boss_index", "0");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }

}
