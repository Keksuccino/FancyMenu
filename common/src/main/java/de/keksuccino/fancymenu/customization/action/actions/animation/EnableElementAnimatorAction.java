package de.keksuccino.fancymenu.customization.action.actions.animation;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.elements.animationcontroller.AnimationControllerElement;
import de.keksuccino.fancymenu.customization.element.elements.animationcontroller.AnimationControllerHandler;
import de.keksuccino.fancymenu.customization.element.elements.animationcontroller.AnimationControllerStateController;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EnableElementAnimatorAction extends Action {

    private static final Logger LOGGER = LogManager.getLogger();

    public EnableElementAnimatorAction() {
        super("enable_element_animator");
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
    public void execute(@Nullable String value) {
        try {
            if ((value == null) || value.isEmpty()) {
                return;
            }

            boolean changed = AnimationControllerStateController.setPlaying(value, true);

            if (changed) {
                ScreenCustomizationLayer layer = ScreenCustomizationLayerHandler.getActiveLayer();
                if (layer != null) {
                    AbstractElement element = layer.getElementByInstanceIdentifier(value);
                    if (element instanceof AnimationControllerElement controller) {
                        AnimationControllerHandler.resetController(controller);
                    }
                }
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to execute EnableElementAnimatorAction!", ex);
        }
    }

    @Override
    public @NotNull Component getActionDisplayName() {
        return Component.translatable("fancymenu.actions.animation.enable_element_animator");
    }

    @Override
    public @NotNull Component[] getActionDescription() {
        return LocalizationUtils.splitLocalizedLines("fancymenu.actions.animation.enable_element_animator.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.translatable("fancymenu.actions.animation.enable_element_animator.value.desc");
    }

    @Override
    public String getValueExample() {
        return "element_animator_identifier";
    }

}
