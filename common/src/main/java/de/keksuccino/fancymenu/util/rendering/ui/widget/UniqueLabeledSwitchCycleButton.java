package de.keksuccino.fancymenu.util.rendering.ui.widget;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.worldselection.SwitchGrid.SwitchBuilder;
import net.minecraft.client.gui.components.StringWidget;

/**
 * Gets applied to {@link CycleButton}s via Mixin.<br>
 * It is used to make {@link CycleButton}s built in {@link SwitchBuilder}s unique, so we can identify them later for customizations.
 */
public interface UniqueLabeledSwitchCycleButton {

    void setLabeledSwitchComponentLabel_FancyMenu(@Nullable Component label);

    /**
     * This is the label of the LabeledSwitch. Normally this is only used for the {@link StringWidget} of the switch,
     * but this makes it almost impossible to identify the actual {@link CycleButton} later, so we connect the two by storing the label in the {@link CycleButton}.<br><br>
     *
     * This should only NOT return NULL if the {@link CycleButton} is part of a LabeledSwitch.
     */
    @Nullable
    Component getLabeledSwitchComponentLabel_FancyMenu();

}
