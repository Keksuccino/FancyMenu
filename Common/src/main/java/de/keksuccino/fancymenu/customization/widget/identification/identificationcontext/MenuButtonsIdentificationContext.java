package de.keksuccino.fancymenu.customization.widget.identification.identificationcontext;

import de.keksuccino.fancymenu.customization.widget.WidgetMeta;
import org.jetbrains.annotations.Nullable;

public abstract class MenuButtonsIdentificationContext {

    public abstract Class<?> getMenu();

    @Nullable
    protected abstract String getRawCompatibilityIdentifierForButton(WidgetMeta data);

    public String getCompatibilityIdentifierForButton(WidgetMeta data) {
        String s = this.getRawCompatibilityIdentifierForButton(data);
        if (s != null) {
            return "button_compatibility_id:" + s;
        }
        return null;
    }

}
