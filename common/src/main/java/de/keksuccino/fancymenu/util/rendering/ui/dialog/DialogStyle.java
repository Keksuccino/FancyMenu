package de.keksuccino.fancymenu.util.rendering.ui.dialog;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum DialogStyle {

    GENERIC("generic", Component.translatable("fancymenu.ui.dialog.title.message"), null),
    INFO("info", Component.translatable("fancymenu.ui.dialog.title.info"), ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/dialog/icons/info.png")),
    WARNING("warning", Component.translatable("fancymenu.ui.dialog.title.warning"), ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/dialog/icons/warning.png")),
    ERROR("error", Component.translatable("fancymenu.ui.dialog.title.error"), ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/dialog/icons/error.png"));

    @NotNull
    private final String name;
    @NotNull
    private final Component title;
    @Nullable
    private final ResourceLocation icon;

    DialogStyle(@NotNull String name, @NotNull Component title, @Nullable ResourceLocation icon) {
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

    public @Nullable ResourceLocation getIcon() {
        return icon;
    }

}
