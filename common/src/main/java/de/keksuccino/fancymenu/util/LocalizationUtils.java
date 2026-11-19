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

    /** The translation of what separates the groups of three digits of long numbers, like "," in English or "." in German. */
    public static final String DIGIT_GROUP_SEPARATOR_KEY = "fancymenu.general.number.digit_group_separator";
    private static final String DEFAULT_DIGIT_GROUP_SEPARATOR = ",";

    /**
     * Formats a whole number with its digits in groups of three, like "100,000" in English or "100.000" in German.<br>
     * The separator is a translation of the given language ({@link #DIGIT_GROUP_SEPARATOR_KEY}) instead of something that depends on the
     * locale of the system or the code of the game language, so the number always fits the translation it goes into. That includes game
     * languages without translations of FancyMenu, which show the English translations with the English separator.
     */
    @NotNull
    public static String formatWholeNumber(long number, @NotNull Language language) {
        String separator = language.has(DIGIT_GROUP_SEPARATOR_KEY) ? language.getOrDefault(DIGIT_GROUP_SEPARATOR_KEY) : DEFAULT_DIGIT_GROUP_SEPARATOR;
        String digits = Long.toString(number);
        int firstDigit = (number < 0L) ? 1 : 0;
        int digitCount = digits.length() - firstDigit;
        if (digitCount <= 3) return digits;
        StringBuilder builder = new StringBuilder(digits.length() + (((digitCount - 1) / 3) * separator.length()));
        // The first group has one to three digits, all others have three
        int groupEnd = firstDigit + ((digitCount - 1) % 3) + 1;
        builder.append(digits, 0, groupEnd);
        for (int groupStart = groupEnd; groupStart < digits.length(); groupStart += 3) {
            builder.append(separator).append(digits, groupStart, groupStart + 3);
        }
        return builder.toString();
    }

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
