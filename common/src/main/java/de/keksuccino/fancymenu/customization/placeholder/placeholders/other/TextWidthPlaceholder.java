package de.keksuccino.fancymenu.customization.placeholder.placeholders.other;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.text.TextFormattingUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.ToIntFunction;

public class TextWidthPlaceholder extends Placeholder {

    /** Measures a text with Minecraft's formatting codes ({@code §}), like the font does. */
    @NotNull
    private final ToIntFunction<String> widthFunction;

    public TextWidthPlaceholder() {
        this(text -> Minecraft.getInstance().font.width(text));
    }

    TextWidthPlaceholder(@NotNull ToIntFunction<String> widthFunction) {
        super("text_width");
        this.widthFunction = Objects.requireNonNull(widthFunction);
    }

    /**
     * Measures the text the way it gets rendered: texts get their {@code &} formatting codes converted to {@code §} codes before rendering,
     * so they get converted before measuring too. The font only knows {@code §} codes and would measure {@code &} codes as visible text.
     */
    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String text = dps.values.get("text");
        if (text != null) {
            return "" + this.widthFunction.applyAsInt(TextFormattingUtils.replaceFormattingCodes(text, "&", "§"));
        }
        return null;
    }

    @Override
    public @Nullable List<String> getValueNames() {
        List<String> l = new ArrayList<>();
        l.add("text");
        return l;
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.placeholders.text_width");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("fancymenu.placeholders.text_width.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.requirements.categories.other");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        DeserializedPlaceholderString dps = new DeserializedPlaceholderString();
        dps.placeholderIdentifier = this.getIdentifier();
        dps.values.put("text", "This is an example text.");
        return dps;
    }

}
