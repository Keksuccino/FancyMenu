package de.keksuccino.fancymenu.menu.button.identification.identificationcontext;

import de.keksuccino.fancymenu.menu.button.ButtonData;

import javax.annotation.Nullable;

public abstract class MenuButtonsIdentificationContext {

    public abstract Class getMenu();

    @Nullable
    protected abstract String getRawCompatibilityIdentifierForButton(ButtonData data);

    //TODO übernehmen 2.7.2
    public String getCompatibilityIdentifierForButton(ButtonData data) {
        String s = this.getRawCompatibilityIdentifierForButton(data);
        if (s != null) {
            return "button_compatibility_id:" + s;
        }
        return null;
    }

}
