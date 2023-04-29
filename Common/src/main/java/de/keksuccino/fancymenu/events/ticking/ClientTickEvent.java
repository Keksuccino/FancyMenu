package de.keksuccino.fancymenu.events.ticking;

import de.keksuccino.fancymenu.events.acara.EventBase;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class ClientTickEvent extends EventBase {

    protected ClientTickEvent() {
    }

    @Override
    public boolean isCancelable() {
        return false;
    }

    public static class Pre extends ClientTickEvent {

        public Pre() {
        }

    }

    public static class Post extends ClientTickEvent {

        public Post() {
        }

    }

}
