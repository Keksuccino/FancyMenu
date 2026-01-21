package de.keksuccino.fancymenu.customization.requirement.requirements.gui;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.elements.button.custombutton.ButtonElement;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.customization.requirement.Requirement;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import net.minecraft.network.chat.Component;

public class IsButtonActiveRequirement extends Requirement {

    private static final Logger LOGGER = LogManager.getLogger();

    public IsButtonActiveRequirement() {
        super("fancymenu_visibility_requirement_is_button_active");
    }

    @Override
    public boolean canRunAsync() {
        return false;
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
                    if (i instanceof ButtonElement b) {
                        return (b.getWidget() != null) && b.getWidget().active;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.requirements.is_button_active");
    }

    @Override
    public Component getDescription() {
        return Component.translatable("fancymenu.requirements.is_button_active.desc");
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.requirements.categories.gui");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.translatable("fancymenu.requirements.is_button_active.value_name");
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
