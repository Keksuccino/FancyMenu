package de.keksuccino.fancymenu.events;

import de.keksuccino.fancymenu.mixin.client.IMixinScreen;
import de.keksuccino.konkrete.events.EventBase;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;
import java.util.Objects;

//TODO übernehmen 1.19.4 (neue klasse)
public class InitOrResizeScreenEvent extends EventBase {

    private final Screen screen;

    protected InitOrResizeScreenEvent(Screen screen) {
        this.screen = Objects.requireNonNull(screen);
    }

    @Override
    public boolean isCancelable() {
        return false;
    }

    public Screen getScreen() {
        return screen;
    }

    public List<Renderable> getRenderables() {
        return ((IMixinScreen)this.screen).getRenderablesFancyMenu();
    }

    public static class Pre extends InitOrResizeScreenEvent {

        public Pre(Screen screen) {
            super(screen);
        }

    }

    public static class Post extends InitOrResizeScreenEvent {

        public Post(Screen screen) {
            super(screen);
        }

    }

}
