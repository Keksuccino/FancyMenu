package de.keksuccino.fancymenu.mixin.mixins.fabric.client;

import net.fabricmc.fabric.impl.resource.loader.ModNioResourcePack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.nio.file.Path;
import java.util.List;

@Mixin(value = ModNioResourcePack.class, remap = false)
public interface IMixinFabricModNioResourcePack {

    @Accessor(value = "basePaths", remap = false)
    List<Path> getBasePaths_FancyMenu();

}
