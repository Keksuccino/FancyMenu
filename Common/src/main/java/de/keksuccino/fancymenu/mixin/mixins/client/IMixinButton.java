package de.keksuccino.fancymenu.mixin.mixins.client;

import net.minecraft.client.gui.components.Button;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Button.class)
public interface IMixinButton {

    @Accessor("onPress") void setPressActionFancyMenu(Button.OnPress pressAction);

}
