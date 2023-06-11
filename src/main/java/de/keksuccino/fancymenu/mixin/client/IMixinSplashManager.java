package de.keksuccino.fancymenu.mixin.client;

import net.minecraft.client.resources.SplashManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(SplashManager.class)
public interface IMixinSplashManager {

    @Accessor("splashes") List<String> getSplashesFancyMenu();

}
