package de.keksuccino.fancymenu.util.rendering.ui.screen;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.function.Consumer;

public abstract class StringBuilderScreen extends ConfiguratorScreen {

    //TODO nutzen, um builder screens für alle Loading Requirement values zu adden
    //TODO nutzen, um builder screens für alle Loading Requirement values zu adden
    //TODO nutzen, um builder screens für alle Loading Requirement values zu adden
    //TODO nutzen, um builder screens für alle Loading Requirement values zu adden

    protected final Consumer<String> callback;

    protected StringBuilderScreen(@NotNull Component title, @NotNull Consumer<String> callback) {
        super(title);
        this.callback = callback;
    }

    @Override
    protected void onCancel() {
        this.callback.accept(null);
    }

    @Override
    protected void onDone() {
        this.callback.accept(this.buildString());
    }

    @NotNull
    public abstract String buildString();

}
