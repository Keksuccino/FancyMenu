package de.keksuccino.fancymenu.util.rendering.text;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Components {

    @NotNull
    public static MutableComponent empty() {
        return Component.empty();
    }

    @NotNull
    public static MutableComponent translatable(@NotNull String key, @Nullable Object... placeholders) {
        return Component.translatable(key, placeholders);
    }

    @NotNull
    public static MutableComponent literal(@NotNull String text) {
        return Component.literal(text);
    }

}
