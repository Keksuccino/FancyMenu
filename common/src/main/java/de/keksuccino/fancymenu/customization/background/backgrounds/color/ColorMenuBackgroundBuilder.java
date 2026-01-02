package de.keksuccino.fancymenu.customization.background.backgrounds.color;

import de.keksuccino.fancymenu.customization.background.MenuBackgroundBuilder;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ColorMenuBackgroundBuilder extends MenuBackgroundBuilder<ColorMenuBackground> {

    public ColorMenuBackgroundBuilder() {
        super("color_fancymenu");
    }

    @Override
    public @NotNull ColorMenuBackground buildDefaultInstance() {
        return new ColorMenuBackground(this);
    }

    @Override
    public void deserializeBackground(@NotNull PropertyContainer serializedBackground, @NotNull ColorMenuBackground deserializeTo) {
    }

    @Override
    public void serializeBackground(@NotNull ColorMenuBackground background, @NotNull PropertyContainer serializeTo) {
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.backgrounds.color");
    }

    @Override
    public @Nullable Component getDescription() {
        return Component.translatable("fancymenu.backgrounds.color.desc");
    }

}
