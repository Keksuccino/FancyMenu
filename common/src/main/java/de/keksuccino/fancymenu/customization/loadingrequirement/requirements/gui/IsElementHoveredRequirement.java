package de.keksuccino.fancymenu.customization.loadingrequirement.requirements.gui;

import de.keksuccino.fancymenu.customization.loadingrequirement.LoadingRequirement;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class IsElementHoveredRequirement extends LoadingRequirement {

    private static final Logger LOGGER = LogManager.getLogger();

    public IsElementHoveredRequirement() {
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
                ScreenCustomizationLayer handler = ScreenCustomizationLayerHandler.getLayerOfScreen(s);
                if (handler != null) {
                    AbstractElement i = handler.getElementByInstanceIdentifier(value);
                    if (i != null) {
                        int mX = MouseInput.getMouseX();
                        int mY = MouseInput.getMouseY();
                        int iX = i.getAbsoluteX();
                        int iY = i.getAbsoluteY();
                        int iW = i.getAbsoluteWidth();
                        int iH = i.getAbsoluteHeight();
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
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.helper.editor.items.visibilityrequirements.is_element_hovered");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.helper.editor.items.visibilityrequirements.is_element_hovered.desc")));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.editor.loading_requirement.category.gui");
    }

    @Override
    public String getValueDisplayName() {
        return I18n.get("fancymenu.helper.editor.items.visibilityrequirements.is_element_hovered.valuename");
    }

    @Override
    public String getValuePreset() {
        return "some_element_ID";
    }

    @Override
    public List<TextEditorFormattingRule> getValueFormattingRules() {
        return null;
    }

}
