package de.keksuccino.fancymenu.testing;

import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * A game language with only the given translations, for tests of code that reads translations through {@link Language#getInstance()}.
 * Like the languages of the game, it answers keys it has no translation for with the key itself.
 */
public final class TestLanguage extends Language {

    private final Map<String, String> translations;

    public TestLanguage(@NotNull Map<String, String> translations) {
        this.translations = Map.copyOf(translations);
    }

    /**
     * Makes the language the game language while the task runs, and the language from before the game language again afterwards.
     */
    public void runAsGameLanguage(@NotNull Runnable task) {
        Language previous = Language.getInstance();
        Language.inject(this);
        try {
            task.run();
        } finally {
            Language.inject(previous);
        }
    }

    @Override
    public String getOrDefault(String key) {
        return this.translations.getOrDefault(key, key);
    }

    @Override
    public boolean has(String key) {
        return this.translations.containsKey(key);
    }

    @Override
    public boolean isDefaultRightToLeft() {
        return false;
    }

    @Override
    public FormattedCharSequence getVisualOrder(FormattedText text) {
        throw new UnsupportedOperationException("Test languages only translate keys");
    }

}
