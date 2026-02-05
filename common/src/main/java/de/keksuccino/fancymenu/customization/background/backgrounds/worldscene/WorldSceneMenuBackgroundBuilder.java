package de.keksuccino.fancymenu.customization.background.backgrounds.worldscene;

import de.keksuccino.fancymenu.customization.background.MenuBackgroundBuilder;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WorldSceneMenuBackgroundBuilder extends MenuBackgroundBuilder<WorldSceneMenuBackground> {

    public WorldSceneMenuBackgroundBuilder() {
        super("world_scene");
    }

    @Override
    public @NotNull WorldSceneMenuBackground buildDefaultInstance() {
        return new WorldSceneMenuBackground(this);
    }

    @Override
    public void deserializeBackground(@NotNull PropertyContainer serializedBackground, @NotNull WorldSceneMenuBackground deserializeTo) {
    }

    @Override
    public void serializeBackground(@NotNull WorldSceneMenuBackground background, @NotNull PropertyContainer serializeTo) {
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.backgrounds.world_scene");
    }

    @Override
    public @Nullable Component getDescription() {
        return Component.translatable("fancymenu.backgrounds.world_scene.desc");
    }
}
