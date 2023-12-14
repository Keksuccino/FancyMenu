package de.keksuccino.fancymenu.util.rendering.text;

import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Components {

    @NotNull
    public static TextComponent empty() {
        return new TextComponent("");
    }

    @NotNull
    public static TranslatableComponent translatable(@NotNull String key, @Nullable Object... placeholders) {
        return new TranslatableComponent(key, placeholders);
    }

    @NotNull
    public static TextComponent literal(@NotNull String text) {
        return new TextComponent(text);
    }

}
