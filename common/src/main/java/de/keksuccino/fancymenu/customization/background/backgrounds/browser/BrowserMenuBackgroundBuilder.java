package de.keksuccino.fancymenu.customization.background.backgrounds.browser;

import de.keksuccino.fancymenu.customization.background.MenuBackgroundBuilder;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BrowserMenuBackgroundBuilder extends MenuBackgroundBuilder<BrowserMenuBackground> {

    public BrowserMenuBackgroundBuilder() {
        super("browser");
    }

    @Override
    public @NotNull BrowserMenuBackground buildDefaultInstance() {
        return new BrowserMenuBackground(this);
    }

    @Override
    public void deserializeBackground(@NotNull PropertyContainer serializedBackground, @NotNull BrowserMenuBackground deserializeTo) {
    }

    @Override
    public void serializeBackground(@NotNull BrowserMenuBackground background, @NotNull PropertyContainer serializeTo) {
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.backgrounds.browser");
    }

    @Override
    public @Nullable Component getDescription() {
        return Component.translatable("fancymenu.backgrounds.browser.desc");
    }

}
