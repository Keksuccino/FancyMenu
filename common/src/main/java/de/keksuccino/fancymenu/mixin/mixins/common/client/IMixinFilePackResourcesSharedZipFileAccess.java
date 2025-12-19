package de.keksuccino.fancymenu.mixin.mixins.common.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.zip.ZipFile;

@Mixin(targets = "net.minecraft.server.packs.FilePackResources$SharedZipFileAccess")
public interface IMixinFilePackResourcesSharedZipFileAccess {

    @Invoker("getOrCreateZipFile")
    ZipFile getOrCreateZipFile_FancyMenu();

}
