package de.keksuccino.fancymenu.customization.background.backgrounds.panorama;

import de.keksuccino.fancymenu.customization.background.MenuBackgroundBuilder;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PanoramaMenuBackgroundBuilder extends MenuBackgroundBuilder<PanoramaMenuBackground> {

    public PanoramaMenuBackgroundBuilder() {
        super("panorama");
    }

    @Override
    public @NotNull PanoramaMenuBackground buildDefaultInstance() {
        return new PanoramaMenuBackground(this);
    }

    @Override
    public void deserializeBackground(@NotNull PropertyContainer serializedBackground, @NotNull PanoramaMenuBackground deserializeTo) {
    }

    @Override
    public void serializeBackground(@NotNull PanoramaMenuBackground background, @NotNull PropertyContainer serializeTo) {
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.backgrounds.panorama");
    }

    @Override
    public @Nullable Component getDescription() {
        return Component.translatable("fancymenu.backgrounds.panorama.desc");
    }

}
