package de.keksuccino.fancymenu.customization.background.backgrounds.animation;

import de.keksuccino.fancymenu.customization.background.MenuBackgroundBuilder;
import de.keksuccino.fancymenu.customization.background.SerializedMenuBackground;
import de.keksuccino.fancymenu.customization.layout.editor.ChooseAnimationScreen;
import de.keksuccino.fancymenu.utils.LocalizationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class AnimationMenuBackgroundBuilder extends MenuBackgroundBuilder<AnimationMenuBackground> {

    public AnimationMenuBackgroundBuilder() {
        super("animation");
    }

    @Override
    public void buildNewOrEditInstance(@NotNull Screen currentScreen, @Nullable AnimationMenuBackground backgroundToEdit, @NotNull Consumer<AnimationMenuBackground> backgroundConsumer) {
        ChooseAnimationScreen s = new ChooseAnimationScreen(currentScreen, (backgroundToEdit != null) ? backgroundToEdit.animationName : null, (call) -> {
            if (call != null) {
                if (backgroundToEdit != null) {
                    backgroundToEdit.animationName = call;
                    backgroundConsumer.accept(backgroundToEdit);
                } else {
                    AnimationMenuBackground b = new AnimationMenuBackground(this);
                    b.animationName = call;
                    backgroundConsumer.accept(b);
                }
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

        return b;

    }

    @Override
    public SerializedMenuBackground serializedBackground(AnimationMenuBackground background) {

        SerializedMenuBackground serialized = new SerializedMenuBackground();

        if (background.animationName != null) {
            serialized.putProperty("animation_name", background.animationName);
        }

        return serialized;

    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.background.animation");
    }

    @Override
    public @Nullable Component[] getDescription() {
        return LocalizationUtils.splitLocalizedLines("fancymenu.background.animation.desc");
    }

}
