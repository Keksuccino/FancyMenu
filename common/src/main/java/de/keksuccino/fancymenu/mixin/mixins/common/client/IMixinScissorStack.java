package de.keksuccino.fancymenu.mixin.mixins.common.client;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import javax.annotation.Nullable;


@Mixin(targets = "net.minecraft.client.gui.GuiGraphicsExtractor$ScissorStack")
public interface IMixinScissorStack {

    @Invoker("peek") @Nullable ScreenRectangle invoke_peek_FancyMenu();

}
