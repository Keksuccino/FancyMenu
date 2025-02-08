package de.keksuccino.fancymenu.customization.loadingrequirement.requirements;

import de.keksuccino.fancymenu.customization.loadingrequirement.LoadingRequirement;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import de.keksuccino.konkrete.input.MouseInput;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Arrays;
import java.util.List;

public class MouseClickedRequirement extends LoadingRequirement {

    public MouseClickedRequirement() {
        super("mouse_click");
    }

    @Override
    public boolean hasValue() {
        // The value is used to choose the mouse button ("left" or "right").
        return true;
    }

    @Override
    public boolean isRequirementMet(@Nullable String value) {
        // Determine which button to check.
        boolean isLeft = true;
        if (value != null && value.trim().equalsIgnoreCase("right")) {
            isLeft = false;
        }
        // Check if the configured mouse button is currently pressed.
        boolean isMouseDown = isLeft ? MouseInput.isLeftMouseDown() : MouseInput.isRightMouseDown();
        return isMouseDown;
    }

    @NotNull
    @Override
    public String getDisplayName() {
        return I18n.get("fancymenu.helper.editor.items.visibilityrequirements.mouse_click");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(I18n.get("fancymenu.helper.editor.items.visibilityrequirements.mouse_click.desc"));
    }

    @Nullable
    @Override
    public String getCategory() {
        return null;
    }

    @Nullable
    @Override
    public String getValueDisplayName() {
        return I18n.get("fancymenu.helper.editor.items.visibilityrequirements.mouse_click.valuename");
    }

    @Nullable
    @Override
    public String getValuePreset() {
        return "left";
    }

    @Nullable
    @Override
    public List<TextEditorFormattingRule> getValueFormattingRules() {
        return null;
    }

}
