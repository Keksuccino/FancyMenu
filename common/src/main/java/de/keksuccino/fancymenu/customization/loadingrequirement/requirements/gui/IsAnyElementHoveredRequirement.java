package de.keksuccino.fancymenu.customization.loadingrequirement.requirements.gui;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.customization.loadingrequirement.LoadingRequirement;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import de.keksuccino.konkrete.input.MouseInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Arrays;
import java.util.List;

public class IsAnyElementHoveredRequirement extends LoadingRequirement {

    private static final Logger LOGGER = LogManager.getLogger();

    public IsAnyElementHoveredRequirement() {
        super("fancymenu_visibility_requirement_is_any_element_hovered");
    }

    @Override
    public boolean canRunAsync() {
        return false;
    }

    @Override
    public boolean hasValue() {
        return false;
    }

    @Override
    public boolean isRequirementMet(@Nullable String value) {
        Screen s = Minecraft.getInstance().screen;
        if (s != null) {
            ScreenCustomizationLayer handler = ScreenCustomizationLayerHandler.getLayerOfScreen(s);
            if (handler != null) {
                for (AbstractElement e : handler.allElements) {
                    int mX = MouseInput.getMouseX();
                    int mY = MouseInput.getMouseY();
                    int iX = e.getAbsoluteX();
                    int iY = e.getAbsoluteY();
                    int iW = e.getAbsoluteWidth();
                    int iH = e.getAbsoluteHeight();
                    if ((mX >= iX) && (mX <= (iX + iW)) && (mY >= iY) && (mY <= (iY + iH))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.helper.editor.items.visibilityrequirements.is_any_element_hovered");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("fancymenu.helper.editor.items.visibilityrequirements.is_any_element_hovered.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.editor.loading_requirement.category.gui");
    }

    @Override
    public String getValueDisplayName() {
        return null;
    }

    @Override
    public String getValuePreset() {
        return null;
    }

    @Override
    public List<TextEditorFormattingRule> getValueFormattingRules() {
        return null;
    }

}
