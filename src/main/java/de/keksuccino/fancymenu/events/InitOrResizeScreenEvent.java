package de.keksuccino.fancymenu.events;

import de.keksuccino.fancymenu.mixin.client.IMixinScreen;
import de.keksuccino.konkrete.events.EventBase;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;
import java.util.List;
import java.util.Objects;

public class InitOrResizeScreenEvent extends EventBase {

    protected final Screen screen;

    protected InitOrResizeScreenEvent(@NotNull Screen screen) {
        this.screen = Objects.requireNonNull(screen);
    }

    @Override
    public boolean isCancelable() {
        return false;
    }

    public Screen getScreen() {
        return screen;
    }

    /** You should not get the widgets list in Pre, because it is fired BEFORE the list get cleared. **/
    public static class Pre extends InitOrResizeScreenEvent {

        public Pre(@NotNull Screen screen) {
            super(screen);
        }

    }

    public static class Post extends InitOrResizeScreenEvent {

        public Post(@NotNull Screen screen) {
            super(screen);
        }

        public List<Renderable> getRenderables() {
            return ((IMixinScreen)this.screen).getRenderablesFancyMenu();
        }

    }

}
