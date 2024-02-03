package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.util.rendering.ui.widget.UniqueLabeledSwitchCycleButton;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

//TODO übernehmen
@Mixin(CycleButton.class)
public class MixinCycleButton implements UniqueLabeledSwitchCycleButton {

    @Unique private Component labeledSwitchComponentLabel_FancyMenu = null;

    @Unique
    @Override
    public void setLabeledSwitchComponentLabel_FancyMenu(@Nullable Component label) {
        this.labeledSwitchComponentLabel_FancyMenu = label;
    }

    @Unique
    @Override
    public @Nullable Component getLabeledSwitchComponentLabel_FancyMenu() {
        return this.labeledSwitchComponentLabel_FancyMenu;
    }

}
