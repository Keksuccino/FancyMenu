package de.keksuccino.fancymenu.menu.action.actions.layout;

import com.google.common.io.Files;
import de.keksuccino.fancymenu.api.buttonaction.ButtonActionContainer;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomizationProperties;
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

            List<MenuCustomizationProperties.LayoutProperties> enabled = MenuCustomizationProperties.getAsLayoutProperties(MenuCustomizationProperties.getProperties());
            List<MenuCustomizationProperties.LayoutProperties> disabled = MenuCustomizationProperties.getAsLayoutProperties(MenuCustomizationProperties.getDisabledProperties());

            for (MenuCustomizationProperties.LayoutProperties l : enabled) {
                if (l.path != null) {
                    String name = Files.getNameWithoutExtension(l.path);
                    if (name.equals(value)) {
                        MenuCustomization.disableLayout(l);
                        return;
                    }
                }
            }

            for (MenuCustomizationProperties.LayoutProperties l : disabled) {
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
