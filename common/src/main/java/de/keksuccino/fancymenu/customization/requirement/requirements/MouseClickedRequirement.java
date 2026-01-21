package de.keksuccino.fancymenu.customization.requirement.requirements;

import de.keksuccino.fancymenu.customization.requirement.Requirement;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import de.keksuccino.konkrete.input.MouseInput;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import net.minecraft.network.chat.Component;

public class MouseClickedRequirement extends Requirement {

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
    public Component getDisplayName() {
        return Component.translatable("fancymenu.requirements.mouse_click");
    }

    @Override
    public Component getDescription() {
        return Component.translatable("fancymenu.requirements.mouse_click.desc");
    }

    @Nullable
    @Override
    public String getCategory() {
        return null;
    }

    @Nullable
    @Override
    public Component getValueDisplayName() {
        return Component.translatable("fancymenu.requirements.mouse_click.value_name");
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
