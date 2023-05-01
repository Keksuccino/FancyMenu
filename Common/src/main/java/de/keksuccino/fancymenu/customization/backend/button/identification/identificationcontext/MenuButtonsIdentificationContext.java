package de.keksuccino.fancymenu.customization.backend.button.identification.identificationcontext;

import de.keksuccino.fancymenu.customization.backend.button.ButtonData;
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
