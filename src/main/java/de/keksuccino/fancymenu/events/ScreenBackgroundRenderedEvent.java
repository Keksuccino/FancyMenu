package de.keksuccino.fancymenu.events;

import de.keksuccino.konkrete.events.EventBase;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;

public class ScreenBackgroundRenderedEvent extends EventBase {

    private final Screen screen;
    private final GuiGraphics graphics;

    public ScreenBackgroundRenderedEvent(@NotNull Screen screen, @NotNull GuiGraphics graphics) {
        this.screen = Objects.requireNonNull(screen);
        this.graphics = Objects.requireNonNull(graphics);
    }

    @Override
    public void setCanceled(boolean b) {
        throw new RuntimeException("[FANCYMENU] Tried to cancel non-cancelable ScreenBackgroundRenderedEvent!");
    }

    @Override
    public boolean isCancelable() {
        return false;
    }

    @NotNull
    public Screen getScreen() {
        return this.screen;
    }

    @NotNull
    public GuiGraphics getGuiGraphics() {
        return this.graphics;
    }

}
