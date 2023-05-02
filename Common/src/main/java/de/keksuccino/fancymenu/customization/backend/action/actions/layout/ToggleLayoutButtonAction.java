package de.keksuccino.fancymenu.customization.backend.action.actions.layout;

import com.google.common.io.Files;
import de.keksuccino.fancymenu.api.buttonaction.ButtonActionContainer;
import de.keksuccino.fancymenu.customization.MenuCustomization;
import de.keksuccino.fancymenu.customization.backend.LayoutHandler;
import de.keksuccino.konkrete.localization.Locals;

import java.util.List;

public class ToggleLayoutButtonAction extends ButtonActionContainer {

    public ToggleLayoutButtonAction() {
        super("fancymenu_buttonaction_toggle_layout");
    }

    @Override
    public String getAction() {
        return "toggle_layout";
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public void execute(String value) {

        if (value != null) {

            List<LayoutHandler.LayoutProperties> enabled = LayoutHandler.getAsLayoutProperties(LayoutHandler.getEnabledLayouts());
            List<LayoutHandler.LayoutProperties> disabled = LayoutHandler.getAsLayoutProperties(LayoutHandler.getDisabledLayouts());

            for (LayoutHandler.LayoutProperties l : enabled) {
                if (l.path != null) {
                    String name = Files.getNameWithoutExtension(l.path);
                    if (name.equals(value)) {
                        MenuCustomization.disableLayout(l);
                        return;
                    }
                }
            }

            for (LayoutHandler.LayoutProperties l : disabled) {
                if (l.path != null) {
                    String name = Files.getNameWithoutExtension(l.path);
                    if (name.equals(value)) {
                        MenuCustomization.enableLayout(l);
                        return;
                    }
                }
            }

        }

    }

    @Override
    public String getActionDescription() {
        return Locals.localize("fancymenu.helper.buttonaction.toggle_layout.desc");
    }

    @Override
    public String getValueDescription() {
        return Locals.localize("fancymenu.helper.buttonaction.toggle_layout.value.desc");
    }

    @Override
    public String getValueExample() {
        return "my_cool_main_menu_layout";
    }

}
