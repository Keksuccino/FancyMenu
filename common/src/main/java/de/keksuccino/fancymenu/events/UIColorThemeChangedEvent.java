package de.keksuccino.fancymenu.events;

import de.keksuccino.fancymenu.util.event.acara.EventBase;
import de.keksuccino.fancymenu.util.rendering.ui.theme.UITheme;

public class UIColorThemeChangedEvent extends EventBase {

    protected final UITheme scheme;

    public UIColorThemeChangedEvent(UITheme scheme) {
        this.scheme = scheme;
    }

    public UITheme getScheme() {
        return this.scheme;
    }

    @Override
    public boolean isCancelable() {
        return false;
    }

}
