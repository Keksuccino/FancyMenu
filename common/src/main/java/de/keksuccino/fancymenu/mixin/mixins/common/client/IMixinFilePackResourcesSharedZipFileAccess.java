package de.keksuccino.fancymenu.mixin.mixins.common.client;

import net.minecraft.server.packs.FilePackResources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.zip.ZipFile;

@Mixin(FilePackResources.SharedZipFileAccess.class)
public interface IMixinFilePackResourcesSharedZipFileAccess {

    @Invoker("getOrCreateZipFile")
    ZipFile getOrCreateZipFile_FancyMenu();

}
