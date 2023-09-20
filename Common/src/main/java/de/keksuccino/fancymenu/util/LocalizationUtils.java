package de.keksuccino.fancymenu.util;

import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinClientLanguage;
import de.keksuccino.konkrete.input.StringUtils;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;

public class LocalizationUtils {

    @NotNull
    public static Component[] splitLocalizedLines(@NotNull String localizationKey, String... placeholderReplacements) {
        List<Component> l = new ArrayList<>();
        for (String s : splitLocalizedStringLines(localizationKey, placeholderReplacements)) {
            l.add(Component.literal(s));
        }
        return l.toArray(new Component[]{});
    }

    @NotNull
    public static String[] splitLocalizedStringLines(@NotNull String localizationKey, String... placeholderReplacements) {
        return StringUtils.splitLines(I18n.get(localizationKey, (Object[]) placeholderReplacements), "\n");
    }

    @NotNull
    public static List<String> getLocalizationKeys() {
        List<String> keys = new ArrayList<>();
        Language lang = Language.getInstance();
        if (lang instanceof ClientLanguage c) {
            keys.addAll(((IMixinClientLanguage)c).getStorageFancyMenu().keySet());
        }
        return keys;
    }

    public static boolean isLocalizationKey(String key) {
        if (key == null) return false;
        return I18n.exists(key);
    }

    @Nullable
    public static String getComponentLocalizationKey(@NotNull Component component) {
        if (component instanceof MutableComponent m) {
            ComponentContents cc = m.getContents();
            if (cc instanceof TranslatableContents t) {
                return t.getKey();
            }
        }
        return null;
    }

}
