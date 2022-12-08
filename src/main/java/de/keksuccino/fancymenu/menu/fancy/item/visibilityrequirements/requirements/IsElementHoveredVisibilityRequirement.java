//TODO übernehmen
package de.keksuccino.fancymenu.menu.fancy.item.visibilityrequirements.requirements;

import de.keksuccino.fancymenu.api.visibilityrequirements.VisibilityRequirement;
import de.keksuccino.fancymenu.menu.fancy.item.CustomizationItemBase;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerBase;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerRegistry;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

public class IsElementHoveredVisibilityRequirement extends VisibilityRequirement {

    private static final Logger LOGGER = LogManager.getLogger();

    public IsElementHoveredVisibilityRequirement() {
        super("fancymenu_visibility_requirement_is_element_hovered");
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public boolean isRequirementMet(@Nullable String value) {

        if (value != null) {
            Screen s = Minecraft.getInstance().screen;
            if (s != null) {
                MenuHandlerBase handler = MenuHandlerRegistry.getHandlerFor(s);
                if (handler != null) {
                    CustomizationItemBase i = handler.getItemByActionId(value);
                    if (i != null) {
                        int mX = MouseInput.getMouseX();
                        int mY = MouseInput.getMouseY();
                        int iX = i.getPosX(s);
                        int iY = i.getPosY(s);
                        int iW = i.getWidth();
                        int iH = i.getHeight();
                        if ((mX >= iX) && (mX <= (iX + iW)) && (mY >= iY) && (mY <= (iY + iH))) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;

    }

    @Override
    public String getDisplayName() {
        return Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.is_element_hovered");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.is_element_hovered.desc"), "%n%"));
    }

    @Override
    public String getValueDisplayName() {
        return Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.is_element_hovered.valuename");
    }

    @Override
    public String getValuePreset() {
        return "some_element_ID";
    }

    @Override
    public CharacterFilter getValueInputFieldFilter() {
        return null;
    }

}
