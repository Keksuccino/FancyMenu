package de.keksuccino.fancymenu.menu.button.identification.identificationcontext;

import de.keksuccino.fancymenu.menu.button.ButtonData;

import javax.annotation.Nullable;

public abstract class MenuButtonsIdentificationContext {

    public abstract Class getMenu();

    @Nullable
    protected abstract String getRawCompatibilityIdentifierForButton(ButtonData data);

    public String getCompatibilityIdentifierForButton(ButtonData data) {
        return "button_compatibility_id:" + this.getRawCompatibilityIdentifierForButton(data);
    }

}
