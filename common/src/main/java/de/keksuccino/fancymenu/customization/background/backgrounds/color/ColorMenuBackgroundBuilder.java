package de.keksuccino.fancymenu.customization.background.backgrounds.color;

import de.keksuccino.fancymenu.customization.background.MenuBackgroundBuilder;
import de.keksuccino.fancymenu.customization.background.SerializedMenuBackground;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.input.TextValidators;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.text.Components;
import de.keksuccino.fancymenu.util.rendering.ui.screen.TextInputScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.function.Consumer;

public class ColorMenuBackgroundBuilder extends MenuBackgroundBuilder<ColorMenuBackground> {

    public ColorMenuBackgroundBuilder() {
        super("color_fancymenu");
    }

    @Override
    public void buildNewOrEditInstance(Screen currentScreen, @Nullable ColorMenuBackground backgroundToEdit, @NotNull Consumer<ColorMenuBackground> backgroundConsumer) {

        ColorMenuBackground back = (backgroundToEdit != null) ? (ColorMenuBackground) backgroundToEdit.copy() : null;
        if (back == null) {
            back = new ColorMenuBackground(this);
        }
        final ColorMenuBackground backFinal = back;

        TextInputScreen s = TextInputScreen.build(Components.translatable("fancymenu.backgrounds.color.hex"), null, callback -> {
            if (callback != null) {
                backFinal.color = DrawableColor.of(callback);
                backgroundConsumer.accept(backFinal);
            }
            Minecraft.getInstance().setScreen(currentScreen);
        }).setTextValidator(consumes -> TextValidators.HEX_COLOR_TEXT_VALIDATOR.get(consumes.getText()));
        s.setText(back.color.getHex());
        Minecraft.getInstance().setScreen(s);

    }

    @Override
    public ColorMenuBackground deserializeBackground(SerializedMenuBackground serializedMenuBackground) {

        ColorMenuBackground b = new ColorMenuBackground(this);

        String color = serializedMenuBackground.getValue("color");
        if (color != null) b.color = DrawableColor.of(color);

        return b;

    }

    @Override
    public SerializedMenuBackground serializedBackground(ColorMenuBackground background) {

        SerializedMenuBackground serialized = new SerializedMenuBackground();

        serialized.putProperty("color", background.color.getHex());

        return serialized;

    }

    @Override
    public @NotNull Component getDisplayName() {
        return Components.translatable("fancymenu.backgrounds.color");
    }

    @Override
    public @Nullable Component[] getDescription() {
        return LocalizationUtils.splitLocalizedLines("fancymenu.backgrounds.color.desc");
    }

}
