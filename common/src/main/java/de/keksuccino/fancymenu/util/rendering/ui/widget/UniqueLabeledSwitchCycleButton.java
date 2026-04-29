package de.keksuccino.fancymenu.util.rendering.ui.widget;

import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * Gets applied to {@link CycleButton}s via Mixin.<br>
 * It is used to connect labels with generated {@link CycleButton}s, so we can identify them later for customizations.
 */
public interface UniqueLabeledSwitchCycleButton {

    void setLabeledSwitchComponentLabel_FancyMenu(@Nullable Component label);

    /**
     * This is the label of the generated switch. We store it on the {@link CycleButton} so it can be identified later.<br><br>
     *
     * This should only NOT return NULL if the {@link CycleButton} is part of a LabeledSwitch.
     */
    @Nullable
    Component getLabeledSwitchComponentLabel_FancyMenu();

}
