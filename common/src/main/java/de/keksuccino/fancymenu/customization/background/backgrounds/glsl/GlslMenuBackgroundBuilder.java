package de.keksuccino.fancymenu.customization.background.backgrounds.glsl;

import de.keksuccino.fancymenu.customization.background.MenuBackgroundBuilder;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GlslMenuBackgroundBuilder extends MenuBackgroundBuilder<GlslMenuBackground> {

    public GlslMenuBackgroundBuilder() {
        super("glsl");
    }

    @Override
    public @NotNull GlslMenuBackground buildDefaultInstance() {
        return new GlslMenuBackground(this);
    }

    @Override
    public void deserializeBackground(@NotNull PropertyContainer serializedBackground, @NotNull GlslMenuBackground deserializeTo) {
    }

    @Override
    public void serializeBackground(@NotNull GlslMenuBackground background, @NotNull PropertyContainer serializeTo) {
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.backgrounds.glsl");
    }

    @Override
    public @Nullable Component getDescription() {
        return Component.translatable("fancymenu.backgrounds.glsl.desc");
    }

}
