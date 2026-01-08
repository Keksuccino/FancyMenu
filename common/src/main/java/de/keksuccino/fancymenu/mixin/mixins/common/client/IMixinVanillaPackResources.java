package de.keksuccino.fancymenu.mixin.mixins.common.client;

import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.VanillaPackResources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.nio.file.Path;
import java.util.Map;
import java.util.List;

@Mixin(VanillaPackResources.class)
public interface IMixinVanillaPackResources {

    @Accessor("pathsForType")
    Map<PackType, List<Path>> getPathsForType_FancyMenu();

    @Accessor("rootPaths")
    List<Path> getRootPaths_FancyMenu();

}
