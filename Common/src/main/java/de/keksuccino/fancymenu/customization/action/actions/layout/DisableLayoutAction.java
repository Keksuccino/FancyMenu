package de.keksuccino.fancymenu.customization.action.actions.layout;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.layout.Layout;
import de.keksuccino.fancymenu.customization.layout.LayoutHandler;
import net.minecraft.client.resources.language.I18n;

public class DisableLayoutAction extends Action {

    public DisableLayoutAction() {
        super("disable_layout");
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public void execute(String value) {

        if (value != null) {

            Layout l = LayoutHandler.getLayout(value);
            if ((l != null) && l.isEnabled()) {
                l.setEnabled(false);
            }

        }

    }

    @Override
    public String getActionDescription() {
        return I18n.get("fancymenu.helper.buttonaction.disable_layout.desc");
    }

    @Override
    public String getValueDescription() {
        return I18n.get("fancymenu.helper.buttonaction.disable_layout.value.desc");
    }

    @Override
    public String getValueExample() {
        return "my_cool_main_menu_layout";
    }

}
