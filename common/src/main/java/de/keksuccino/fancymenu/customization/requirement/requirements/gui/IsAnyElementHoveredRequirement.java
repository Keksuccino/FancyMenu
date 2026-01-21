package de.keksuccino.fancymenu.customization.requirement.requirements.gui;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.customization.requirement.Requirement;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import de.keksuccino.konkrete.input.MouseInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import net.minecraft.network.chat.Component;

public class IsAnyElementHoveredRequirement extends Requirement {

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
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.requirements.is_any_element_hovered");
    }

    @Override
    public Component getDescription() {
        return Component.translatable("fancymenu.requirements.is_any_element_hovered.desc");
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.requirements.categories.gui");
    }

    @Override
    public Component getValueDisplayName() {
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
