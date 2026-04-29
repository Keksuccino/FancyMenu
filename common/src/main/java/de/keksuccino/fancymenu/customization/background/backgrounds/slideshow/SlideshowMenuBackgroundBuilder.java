package de.keksuccino.fancymenu.customization.background.backgrounds.slideshow;

import de.keksuccino.fancymenu.customization.background.MenuBackgroundBuilder;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SlideshowMenuBackgroundBuilder extends MenuBackgroundBuilder<SlideshowMenuBackground> {

    public SlideshowMenuBackgroundBuilder() {
        super("slideshow");
    }

    @Override
    public @NotNull SlideshowMenuBackground buildDefaultInstance() {
        return new SlideshowMenuBackground(this);
    }

    @Override
    public void deserializeBackground(@NotNull PropertyContainer serializedBackground, @NotNull SlideshowMenuBackground deserializeTo) {
    }

    @Override
    public void serializeBackground(@NotNull SlideshowMenuBackground background, @NotNull PropertyContainer serializeTo) {
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.backgrounds.slideshow");
    }

    @Override
    public @Nullable Component getDescription() {
        return Component.translatable("fancymenu.backgrounds.slideshow.desc");
    }

}