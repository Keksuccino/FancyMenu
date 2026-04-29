package de.keksuccino.fancymenu.customization.background.backgrounds.animation;

import de.keksuccino.fancymenu.customization.background.MenuBackgroundBuilder;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.rendering.text.Components;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Deprecated(forRemoval = true)
public class AnimationMenuBackgroundBuilder extends MenuBackgroundBuilder<AnimationMenuBackground> {

    public AnimationMenuBackgroundBuilder() {
        super("animation");
    }

    @Override
    public boolean isDeprecated() {
        return true;
    }

    @Override
    public @NotNull AnimationMenuBackground buildDefaultInstance() {
        return new AnimationMenuBackground(this);
    }

    @Override
    public void deserializeBackground(@NotNull PropertyContainer serializedBackground, @NotNull AnimationMenuBackground deserializeTo) {
        deserializeTo.animationName = serializedBackground.getValue("animation_name");

        String restartOnLoad = serializedBackground.getValue("restart_on_load");
        if ((restartOnLoad != null) && restartOnLoad.equals("true")) {
            deserializeTo.restartOnMenuLoad = true;
        }
    }

    @Override
    public void serializeBackground(@NotNull AnimationMenuBackground background, @NotNull PropertyContainer serializeTo) {
        if (background.animationName != null) {
            serializeTo.putProperty("animation_name", background.animationName);
        }

        serializeTo.putProperty("restart_on_load", "" + background.restartOnMenuLoad);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Components.translatable("fancymenu.background.animation");
    }

    @Override
    public @Nullable Component getDescription() {
        return Component.translatable("fancymenu.background.animation.desc");
    }

}
