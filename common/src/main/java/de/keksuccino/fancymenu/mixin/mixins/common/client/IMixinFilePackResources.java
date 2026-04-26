package de.keksuccino.fancymenu.mixin.mixins.common.client;

import net.minecraft.server.packs.FilePackResources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FilePackResources.class)
public interface IMixinFilePackResources {

    @Accessor("zipFileAccess")
    FilePackResources.SharedZipFileAccess getZipFileAccess_FancyMenu();

    @Accessor("prefix")
    String getPrefix_FancyMenu();

}
