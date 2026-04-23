package de.keksuccino.fancymenu.util.rendering.ui.dialog.message;

import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcon;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum MessageDialogStyle {

    GENERIC("generic", Component.translatable("fancymenu.ui.dialog.title.message"), null),
    INFO("info", Component.translatable("fancymenu.ui.dialog.title.info"), MaterialIcons.INFO),
    WARNING("warning", Component.translatable("fancymenu.ui.dialog.title.warning"), MaterialIcons.WARNING),
    ERROR("error", Component.translatable("fancymenu.ui.dialog.title.error"), MaterialIcons.ERROR);

    @NotNull
    private final String name;
    @NotNull
    private final Component title;
    @Nullable
    private final MaterialIcon icon;

    MessageDialogStyle(@NotNull String name, @NotNull Component title, @Nullable MaterialIcon icon) {
        this.name = name;
        this.title = title;
        this.icon = icon;
    }

    public @NotNull String getName() {
        return name;
    }

    public @NotNull Component getTitle() {
        return title;
    }

    public @Nullable MaterialIcon getIcon() {
        return icon;
    }

}
