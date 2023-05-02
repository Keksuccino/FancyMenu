package de.keksuccino.fancymenu.event.events.ticking;

import de.keksuccino.fancymenu.event.acara.EventBase;

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
