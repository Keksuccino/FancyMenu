package de.keksuccino.fancymenu.events;

import de.keksuccino.fancymenu.util.event.acara.EventBase;
import de.keksuccino.fancymenu.util.rendering.ui.colorscheme.UIColorScheme;

public class UIColorSchemeChangedEvent extends EventBase {

    protected final UIColorScheme scheme;

    public UIColorSchemeChangedEvent(UIColorScheme scheme) {
        this.scheme = scheme;
    }

    public UIColorScheme getScheme() {
        return this.scheme;
    }

    @Override
    public boolean isCancelable() {
        return false;
    }

}
