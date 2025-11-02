package de.keksuccino.fancymenu.customization.placeholder.placeholders.world;

import com.google.common.collect.Ordering;
import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.platform.Services;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.SerializationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;

public class ActiveEffectPlaceholder extends Placeholder {

    public ActiveEffectPlaceholder() {
        super("active_effect");
    }

    @Override
    public boolean canRunAsync() {
        return false;
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        LocalPlayer player = Minecraft.getInstance().player;
        ClientLevel level = Minecraft.getInstance().level;
        int index = SerializationUtils.deserializeNumber(Integer.class, 0, dps.values.get("effect_index"));
        if ((player != null) && (level != null)) {
            List<MobEffectInstance> effects = Ordering.natural().reverse().sortedCopy(player.getActiveEffects());
            if (effects.size() >= (index + 1)) {
                ResourceLocation key = Services.PLATFORM.getEffectKey(effects.get(index).getEffect());
                if (key != null) return key.toString();
            }
        }
        return "";
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return List.of("effect_index");
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.placeholders.world.active_effect");
    }

    @Override
    public @Nullable List<String> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.placeholders.world.active_effect.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.placeholders.categories.world");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        HashMap<String, String> values = new LinkedHashMap<>();
        values.put("effect_index", "0");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }

}
