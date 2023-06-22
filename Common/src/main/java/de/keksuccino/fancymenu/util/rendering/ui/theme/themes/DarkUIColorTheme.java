package de.keksuccino.fancymenu.util.rendering.ui.theme.themes;

import de.keksuccino.fancymenu.util.rendering.ui.theme.UIColorTheme;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class DarkUIColorTheme extends UIColorTheme {

    public DarkUIColorTheme() {
        super("dark", "Dark");
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.ui.color_scheme.schemes.dark");
    }

}
