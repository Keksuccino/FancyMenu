package de.keksuccino.fancymenu.mixin.mixins.common.client;

import net.minecraft.client.gui.components.SplashRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SplashRenderer.class)
public interface IMixinSplashRenderer {

    @Accessor("splash") String getSplashFancyMenu();

}
