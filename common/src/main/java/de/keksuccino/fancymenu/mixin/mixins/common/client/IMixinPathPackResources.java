package de.keksuccino.fancymenu.mixin.mixins.common.client;

import net.minecraft.server.packs.PathPackResources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.nio.file.Path;

@Mixin(PathPackResources.class)
public interface IMixinPathPackResources {

    @Accessor("root")
    Path getRoot_FancyMenu();

}
