package de.keksuccino.fancymenu.util.rendering.ui.dialog;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum DialogStyle {

    GENERIC("generic", null),
    INFO("info", ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/dialog/icons/info.png")),
    WARNING("warning", ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/dialog/icons/warning.png")),
    ERROR("error", ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/dialog/icons/error.png"));

    @NotNull
    private final String name;
    @Nullable
    private final ResourceLocation icon;

    DialogStyle(@NotNull String name, @Nullable ResourceLocation icon) {
        this.name = name;
        this.icon = icon;
    }

    public @NotNull String getName() {
        return name;
    }

    public @Nullable ResourceLocation getIcon() {
        return icon;
    }

}
