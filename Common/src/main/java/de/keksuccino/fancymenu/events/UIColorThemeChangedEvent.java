package de.keksuccino.fancymenu.events;

import de.keksuccino.fancymenu.util.event.acara.EventBase;
import de.keksuccino.fancymenu.util.rendering.ui.theme.UIColorTheme;

public class UIColorThemeChangedEvent extends EventBase {

    protected final UIColorTheme scheme;

    public UIColorThemeChangedEvent(UIColorTheme scheme) {
        this.scheme = scheme;
    }

    public UIColorTheme getScheme() {
        return this.scheme;
    }

    @Override
    public boolean isCancelable() {
        return false;
    }

}
