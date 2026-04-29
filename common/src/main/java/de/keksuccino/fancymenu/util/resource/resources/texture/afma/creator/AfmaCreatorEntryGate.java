package de.keksuccino.fancymenu.util.resource.resources.texture.afma.creator;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class AfmaCreatorEntryGate {

    private AfmaCreatorEntryGate() {
    }

    public static void open(@NotNull Screen parentScreen) {
        Objects.requireNonNull(parentScreen);
        Minecraft.getInstance().setScreen(new AfmaCreatorScreen(parentScreen));
    }

}
