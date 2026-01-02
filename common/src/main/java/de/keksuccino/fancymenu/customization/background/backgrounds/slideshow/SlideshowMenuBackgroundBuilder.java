package de.keksuccino.fancymenu.customization.background.backgrounds.slideshow;

import de.keksuccino.fancymenu.customization.background.MenuBackgroundBuilder;
import de.keksuccino.fancymenu.customization.background.SerializedMenuBackground;
import de.keksuccino.fancymenu.customization.layout.editor.ChooseSlideshowScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.function.Consumer;

public class SlideshowMenuBackgroundBuilder extends MenuBackgroundBuilder<SlideshowMenuBackground> {

    public SlideshowMenuBackgroundBuilder() {
        super("slideshow");
    }

    @Override
    public void buildNewOrEditInstance(Screen currentScreen, @Nullable SlideshowMenuBackground backgroundToEdit, @NotNull Consumer<SlideshowMenuBackground> backgroundConsumer) {
        ChooseSlideshowScreen s = new ChooseSlideshowScreen((backgroundToEdit != null) ? backgroundToEdit.slideshowName : null, (call) -> {
            if (call != null) {
                if (backgroundToEdit != null) {
                    backgroundToEdit.slideshowName = call;
                    backgroundConsumer.accept(backgroundToEdit);
                } else {
                    SlideshowMenuBackground b = new SlideshowMenuBackground(this);
                    b.slideshowName = call;
                    backgroundConsumer.accept(b);
                }
            } else {
                backgroundConsumer.accept(backgroundToEdit);
            }
            Minecraft.getInstance().setScreen(currentScreen);
        });
        Minecraft.getInstance().setScreen(s);
    }

    @Override
    public SlideshowMenuBackground deserializeBackground(SerializedMenuBackground serializedMenuBackground) {

        SlideshowMenuBackground b = new SlideshowMenuBackground(this);

        b.slideshowName = serializedMenuBackground.getValue("slideshow_name");

        return b;

    }

    @Override
    public SerializedMenuBackground serializeBackground(SlideshowMenuBackground background) {

        SerializedMenuBackground serialized = new SerializedMenuBackground();

        if (background.slideshowName != null) {
            serialized.putProperty("slideshow_name", background.slideshowName);
        }

        return serialized;

    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.backgrounds.slideshow");
    }

    @Override
    public @Nullable Component[] getDescription() {
        return LocalizationUtils.splitLocalizedLines("fancymenu.backgrounds.slideshow.desc");
    }

}
