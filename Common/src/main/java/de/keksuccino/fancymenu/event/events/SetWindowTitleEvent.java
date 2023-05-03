package de.keksuccino.fancymenu.event.events;

import de.keksuccino.fancymenu.event.acara.EventBase;

/** Fired before the window title gets set/updated. **/
public class SetWindowTitleEvent extends EventBase {

    protected String customTitle = null;

    public void setCustomTitle(String title) {
        this.customTitle = title;
    }

    public String getCustomTitle() {
        return this.customTitle;
    }

    @Override
    public boolean isCancelable() {
        return false;
    }

}
