package de.keksuccino.fancymenu.events.screen;

import de.keksuccino.fancymenu.util.event.acara.EventBase;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

public class ScreenMouseMoveEvent extends EventBase {

    @Nullable
    private final Screen screen;
    private final double mouseX;
    private final double mouseY;
    private final double deltaX;
    private final double deltaY;

    public ScreenMouseMoveEvent(@Nullable Screen screen, double mouseX, double mouseY, double deltaX, double deltaY) {
        this.screen = screen;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.deltaX = deltaX;
        this.deltaY = deltaY;
    }

    @Nullable
    public Screen getScreen() {
        return this.screen;
    }

    public double getMouseX() {
        return this.mouseX;
    }

    public double getMouseY() {
        return this.mouseY;
    }

    public double getDeltaX() {
        return this.deltaX;
    }

    public double getDeltaY() {
        return this.deltaY;
    }

    @Override
    public boolean isCancelable() {
        return false;
    }

}

