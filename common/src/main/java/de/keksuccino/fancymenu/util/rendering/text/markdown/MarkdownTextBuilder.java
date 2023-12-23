package de.keksuccino.fancymenu.util.rendering.text.markdown;

import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Objects;

public class MarkdownTextBuilder {

    protected StringBuilder builder = new StringBuilder();

    @NotNull
    public static MarkdownTextBuilder create() {
        return new MarkdownTextBuilder();
    }

    public MarkdownTextBuilder addLine(@NotNull String line) {
        this.builder.append(Objects.requireNonNull(line)).append("\n");
        return this;
    }

    public MarkdownTextBuilder addLocalizedLine(@NotNull String key, @Nullable Object... placeholders) {
        return this.addLine(I18n.get(key, placeholders));
    }

    public MarkdownTextBuilder addHeadline(@NotNull MarkdownTextFragment.HeadlineType headlineType, @NotNull String headline) {
        Objects.requireNonNull(headlineType);
        if (headlineType == MarkdownTextFragment.HeadlineType.BIG) {
            headline = "### " + headline;
        } else if (headlineType == MarkdownTextFragment.HeadlineType.BIGGER) {
            headline = "## " + headline;
        } else if (headlineType == MarkdownTextFragment.HeadlineType.BIGGEST) {
            headline = "# " + headline;
        }
        return this.addLine(headline);
    }

    public MarkdownTextBuilder addLocalizedHeadline(@NotNull MarkdownTextFragment.HeadlineType headlineType, @NotNull String key, @Nullable Object... placeholders) {
        return this.addHeadline(headlineType, I18n.get(key, placeholders));
    }

    public MarkdownTextBuilder addEmptyLine() {
        return this.addLine("");
    }

    @NotNull
    public String build() {
        return this.builder.toString();
    }

    @Override
    public String toString() {
        return this.build();
    }

}
