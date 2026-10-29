package de.keksuccino.fancymenu.util.window;

import net.minecraft.ChatFormatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

/**
 * Resolves the custom window title and keeps the window in sync with titles whose placeholders change their value.
 *
 * <p>Vanilla only rebuilds the window title on a few lifecycle events, so {@link #tick()} re-resolves titles that contain
 * placeholders and requests a window title refresh only when the resolved title differs from the one the window shows.
 * Static titles are not resolved again until the raw title changes.</p>
 *
 * <p>Minecraft builds its first window title before FancyMenu registers its placeholders and variables. Titles containing
 * placeholders fall back to the vanilla title until placeholders are ready, so the window never shows raw placeholder syntax.</p>
 */
public final class CustomWindowTitleController {

    private static final Pattern LINE_BREAK_PATTERN = Pattern.compile("\\R");

    @NotNull private final Supplier<String> rawTitleSupplier;
    @NotNull private final Predicate<String> placeholderDetector;
    @NotNull private final BooleanSupplier placeholdersReady;
    @NotNull private final UnaryOperator<String> placeholderResolver;
    @NotNull private final Runnable windowTitleRefresher;
    @Nullable private String lastRawTitle;
    @Nullable private String lastTitle;

    /**
     * @param rawTitleSupplier supplies the configured title, which is empty if no custom title is set
     * @param placeholderDetector checks if a raw title contains placeholders
     * @param placeholdersReady whether placeholders and variables are registered and can get resolved
     * @param placeholderResolver replaces all placeholders in a raw title
     * @param windowTitleRefresher rebuilds the window title, which calls {@link #createTitle()} again
     */
    public CustomWindowTitleController(@NotNull Supplier<String> rawTitleSupplier, @NotNull Predicate<String> placeholderDetector, @NotNull BooleanSupplier placeholdersReady, @NotNull UnaryOperator<String> placeholderResolver, @NotNull Runnable windowTitleRefresher) {
        this.rawTitleSupplier = Objects.requireNonNull(rawTitleSupplier);
        this.placeholderDetector = Objects.requireNonNull(placeholderDetector);
        this.placeholdersReady = Objects.requireNonNull(placeholdersReady);
        this.placeholderResolver = Objects.requireNonNull(placeholderResolver);
        this.windowTitleRefresher = Objects.requireNonNull(windowTitleRefresher);
    }

    /**
     * Resolves the title for Minecraft's window title builder and remembers it as the title the window shows.
     *
     * @return the custom title or {@code null} if the vanilla title should be used
     */
    @Nullable
    public String createTitle() {
        String rawTitle = this.rawTitleSupplier.get();
        String title = this.resolveTitle(rawTitle);
        this.lastRawTitle = rawTitle;
        this.lastTitle = title;
        return title;
    }

    /**
     * Refreshes the window title if the raw title changed or its placeholders resolve to a different title now.
     */
    public void tick() {
        String rawTitle = this.rawTitleSupplier.get();
        if (rawTitle.equals(this.lastRawTitle) && !this.placeholderDetector.test(rawTitle)) return;
        String title = this.resolveTitle(rawTitle);
        this.lastRawTitle = rawTitle;
        if (Objects.equals(title, this.lastTitle)) return;
        // Stored before refreshing, so a refresh that never reaches createTitle() does not get repeated every tick
        this.lastTitle = title;
        this.windowTitleRefresher.run();
    }

    @Nullable
    private String resolveTitle(@NotNull String rawTitle) {
        if (rawTitle.isEmpty()) return null;
        String title = rawTitle;
        if (this.placeholderDetector.test(rawTitle)) {
            if (!this.placeholdersReady.getAsBoolean()) return null;
            title = this.placeholderResolver.apply(rawTitle);
        }
        title = sanitizeTitle(title);
        return title.isBlank() ? null : title;
    }

    /**
     * Window titles are single-line plain text, so formatting codes and line breaks would show up as broken characters.
     */
    @NotNull
    private static String sanitizeTitle(@NotNull String title) {
        String plainTitle = Objects.requireNonNull(ChatFormatting.stripFormatting(title));
        return LINE_BREAK_PATTERN.matcher(plainTitle).replaceAll(" ");
    }

}
