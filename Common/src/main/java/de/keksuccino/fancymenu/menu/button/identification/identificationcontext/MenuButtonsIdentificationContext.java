package de.keksuccino.fancymenu.menu.button.identification.identificationcontext;

import de.keksuccino.fancymenu.menu.button.ButtonData;
import org.jetbrains.annotations.Nullable;

public abstract class MenuButtonsIdentificationContext {

    public abstract Class<?> getMenu();

    @Nullable
    protected abstract String getRawCompatibilityIdentifierForButton(ButtonData data);

    public String getCompatibilityIdentifierForButton(ButtonData data) {
        String s = this.getRawCompatibilityIdentifierForButton(data);
        if (s != null) {
            return "button_compatibility_id:" + s;
        }
        return null;
    }

}
