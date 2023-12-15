package de.keksuccino.fancymenu.events.screen;

import de.keksuccino.fancymenu.util.event.acara.EventBase;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinScreen;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;
import java.util.List;
import java.util.Objects;

public class InitOrResizeScreenEvent extends EventBase {

    protected final Screen screen;
    protected final InitializationPhase phase;

    protected InitOrResizeScreenEvent(@NotNull Screen screen, @NotNull InitializationPhase phase) {
        this.screen = Objects.requireNonNull(screen);
        this.phase = Objects.requireNonNull(phase);
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
    public InitializationPhase getInitializationPhase() {
        return this.phase;
    }

    /** You should not get the widgets list in Pre, because it is fired BEFORE the list gets cleared. **/
    public static class Pre extends InitOrResizeScreenEvent {

        public Pre(@NotNull Screen screen, @NotNull InitializationPhase phase) {
            super(screen, phase);
        }

    }

    public static class Post extends InitOrResizeScreenEvent {

        public Post(@NotNull Screen screen, @NotNull InitializationPhase phase) {
            super(screen, phase);
        }

        public <T extends GuiEventListener & NarratableEntry> void addWidget(T widget) {
            this.getWidgets().add(widget);
            this.getNarratables().add(widget);
        }

        public <T extends GuiEventListener & NarratableEntry & Widget> void addRenderableWidget(T widget) {
            this.addWidget(widget);
            this.getRenderables().add(widget);
        }

        public List<GuiEventListener> getWidgets() {
            return ((IMixinScreen)this.getScreen()).getChildrenFancyMenu();
        }

        public List<Widget> getRenderables() {
            return ((IMixinScreen)this.getScreen()).getRenderablesFancyMenu();
        }

        public List<NarratableEntry> getNarratables() {
            return ((IMixinScreen)this.getScreen()).getNarratablesFancyMenu();
        }

    }

    public enum InitializationPhase {
        INIT,
        RESIZE
    }

}
