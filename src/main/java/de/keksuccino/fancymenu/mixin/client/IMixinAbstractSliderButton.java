package de.keksuccino.fancymenu.mixin.client;

import net.minecraft.client.gui.components.AbstractSliderButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractSliderButton.class)
public interface IMixinAbstractSliderButton {

    @Accessor("canChangeValue") boolean getCanChangeValueFancyMenu();

}
