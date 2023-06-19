package de.keksuccino.fancymenu.customization.action.actions.layout;

import com.google.common.io.Files;
import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.layout.Layout;
import de.keksuccino.fancymenu.customization.layout.LayoutHandler;
import net.minecraft.client.resources.language.I18n;

import java.util.List;

public class EnableLayoutAction extends Action {

    public EnableLayoutAction() {
        super("enable_layout");
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public void execute(String value) {

        if (value != null) {

            Layout l = LayoutHandler.getLayout(value);
            if ((l != null) && !l.isEnabled()) {
                l.setEnabled(true, true);
            }

        }

    }

    @Override
    public String getActionDescription() {
        return I18n.get("fancymenu.helper.buttonaction.enable_layout.desc");
    }

    @Override
    public String getValueDescription() {
        return I18n.get("fancymenu.helper.buttonaction.enable_layout.value.desc");
    }

    @Override
    public String getValueExample() {
        return "my_cool_main_menu_layout";
    }

}
