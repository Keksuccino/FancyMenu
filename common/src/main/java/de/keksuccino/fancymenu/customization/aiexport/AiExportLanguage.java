package de.keksuccino.fancymenu.customization.aiexport;

import de.keksuccino.fancymenu.mixin.mixins.common.client.AccessorMixinI18n;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.client.resources.language.LanguageInfo;
import net.minecraft.locale.Language;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Builds AI exports in English, no matter which language the user plays the game in, so the AI always gets the names and descriptions it knows from the docs.
 */
public final class AiExportLanguage {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String ENGLISH = "en_us";

    @Nullable
    private static Language cachedEnglish;
    @NotNull
    private static WeakReference<Language> cachedEnglishSource = new WeakReference<>(null);

    private AiExportLanguage() {
    }

    /**
     * Runs the task while the game language is English, so every localized name and description it reads is English.<br><br>
     *
     * This swaps the global language instance of the game for the time of the task, which makes it fragile:
     * <ul>
     *     <li>It only swaps on the main thread, and only for short tasks that don't render anything.</li>
     *     <li>Other threads that read localizations during the swap (like placeholders that get replaced async) get English text for that moment.</li>
     *     <li>The old language gets restored in a finally block, so an exception in the task can't leave the game in English.</li>
     * </ul>
     */
    public static <T> T supplyInEnglish(@NotNull Supplier<T> task) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.isSameThread() || ENGLISH.equals(minecraft.options.languageCode)) return task.get();
        Language current = Language.getInstance();
        Language english = getEnglish(current);
        if (english == null) return task.get();
        // I18n keeps its own reference to the language, so it needs to get swapped too
        Language currentI18n = AccessorMixinI18n.get_language_FancyMenu();
        Language.inject(english);
        AccessorMixinI18n.invoke_setLanguage_FancyMenu(english);
        try {
            return task.get();
        } finally {
            Language.inject(current);
            AccessorMixinI18n.invoke_setLanguage_FancyMenu(currentI18n);
        }
    }

    @Nullable
    private static Language getEnglish(@NotNull Language current) {
        // The game injects a new language instance after every resource reload, so an English instance loaded before the last reload gets loaded again
        if ((cachedEnglish != null) && (cachedEnglishSource.get() == current)) return cachedEnglish;
        try {
            LanguageInfo englishInfo = Objects.requireNonNull(Minecraft.getInstance().getLanguageManager().getLanguage(ENGLISH));
            cachedEnglish = ClientLanguage.loadFrom(Minecraft.getInstance().getResourceManager(), List.of(englishInfo));
            cachedEnglishSource = new WeakReference<>(current);
            return cachedEnglish;
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to load the English localization for an AI export!", ex);
        }
        return null;
    }

}
