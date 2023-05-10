package de.keksuccino.fancymenu.customization.action.actions.layout;

import com.google.common.io.Files;
import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.layout.LayoutHandler;
import de.keksuccino.konkrete.localization.Locals;

import java.util.List;

public class ToggleLayoutAction extends Action {

    public ToggleLayoutAction() {
        super("toggle_layout");
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
                        LayoutHandler.disableLayout(l);
                        return;
                    }
                }
            }

            for (LayoutHandler.LayoutProperties l : disabled) {
                if (l.path != null) {
                    String name = Files.getNameWithoutExtension(l.path);
                    if (name.equals(value)) {
                        LayoutHandler.enableLayout(l);
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
