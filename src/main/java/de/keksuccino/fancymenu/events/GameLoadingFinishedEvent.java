package de.keksuccino.fancymenu.events;

import net.minecraftforge.fml.common.eventhandler.Event;

public class GameLoadingFinishedEvent extends Event {

    public GameLoadingFinishedEvent() {
    }

    @Override
    public boolean isCancelable() {
        return false;
    }

}
