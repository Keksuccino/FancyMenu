package de.keksuccino.fancymenu.customization.background.backgrounds.animation;

import de.keksuccino.fancymenu.customization.background.MenuBackgroundBuilder;
import de.keksuccino.fancymenu.customization.background.SerializedMenuBackground;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.text.Components;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.function.Consumer;

//TODO übernehmen (annotation)
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
    public void buildNewOrEditInstance(Screen currentScreen, @Nullable AnimationMenuBackground backgroundToEdit, @NotNull Consumer<AnimationMenuBackground> backgroundConsumer) {
        AnimationMenuBackground back = (backgroundToEdit != null) ? (AnimationMenuBackground) backgroundToEdit.copy() : null;
        if (back == null) {
            back = new AnimationMenuBackground(this);
        }
        AnimationMenuBackgroundConfigScreen s = new AnimationMenuBackgroundConfigScreen(currentScreen, back, (call) -> {
            if (call != null) {
                backgroundConsumer.accept(call);
            } else {
                backgroundConsumer.accept(backgroundToEdit);
            }
        });
        Minecraft.getInstance().setScreen(s);
    }

    @Override
    public AnimationMenuBackground deserializeBackground(SerializedMenuBackground serializedMenuBackground) {

        AnimationMenuBackground b = new AnimationMenuBackground(this);

        b.animationName = serializedMenuBackground.getValue("animation_name");

        String restartOnLoad = serializedMenuBackground.getValue("restart_on_load");
        if ((restartOnLoad != null) && restartOnLoad.equals("true")) {
            b.restartOnMenuLoad = true;
        }

        return b;

    }

    @Override
    public SerializedMenuBackground serializedBackground(AnimationMenuBackground background) {

        SerializedMenuBackground serialized = new SerializedMenuBackground();

        if (background.animationName != null) {
            serialized.putProperty("animation_name", background.animationName);
        }

        serialized.putProperty("restart_on_load", "" + background.restartOnMenuLoad);

        return serialized;

    }

    @Override
    public @NotNull Component getDisplayName() {
        return Components.translatable("fancymenu.background.animation");
    }

    @Override
    public @Nullable Component[] getDescription() {
        return LocalizationUtils.splitLocalizedLines("fancymenu.background.animation.desc");
    }

}
