package de.keksuccino.fancymenu.mixin.mixins.common.client;

import net.minecraft.client.gui.components.Button;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Button.class)
public interface IMixinButton {

    @Mutable
    @Accessor("onPress") void setPressActionFancyMenu(Button.OnPress pressAction);

}
