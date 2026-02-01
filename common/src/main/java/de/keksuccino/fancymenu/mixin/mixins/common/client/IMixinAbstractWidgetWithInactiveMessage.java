package de.keksuccino.fancymenu.mixin.mixins.common.client;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractWidget.WithInactiveMessage.class)
public interface IMixinAbstractWidgetWithInactiveMessage {

    @Accessor("inactiveMessage") void setInactiveMessageFancyMenu(Component message);

}
