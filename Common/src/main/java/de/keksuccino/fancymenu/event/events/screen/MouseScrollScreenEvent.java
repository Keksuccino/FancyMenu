package de.keksuccino.fancymenu.event.events.screen;

import de.keksuccino.fancymenu.event.acara.EventBase;
import net.minecraft.client.gui.screens.Screen;

public class MouseScrollScreenEvent extends EventBase {

    private final Screen screen;
    private final double scrollDelta;
    private final double mouseX;
    private final double mouseY;

    protected MouseScrollScreenEvent(Screen screen, double mouseX, double mouseY, double scrollDelta) {
        this.screen = screen;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.scrollDelta = scrollDelta;
    }

    public Screen getScreen() {
        return screen;
    }

    public double getScrollDelta() {
        return scrollDelta;
    }

    public double getMouseX() {
        return mouseX;
    }

    public double getMouseY() {
        return mouseY;
    }

    @Override
    public boolean isCancelable() {
        return false;
    }

    public static class Pre extends MouseScrollScreenEvent {

        public Pre(Screen screen, double mouseX, double mouseY, double scrollDelta) {
            super(screen, mouseX, mouseY, scrollDelta);
        }

        @Override
        public boolean isCancelable() {
            return true;
        }

    }

    public static class Post extends MouseScrollScreenEvent {

        public Post(Screen screen, double mouseX, double mouseY, double scrollDelta) {
            super(screen, mouseX, mouseY, scrollDelta);
        }

    }

}
