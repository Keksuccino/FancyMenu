package de.keksuccino.fancymenu.mixin.mixins.common.client;

import net.minecraft.server.packs.FilePackResources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.io.IOException;
import java.util.zip.ZipFile;

@Mixin(FilePackResources.class)
public interface IMixinFilePackResources {

    @Invoker("getOrCreateZipFile")
    ZipFile getOrCreateZipFile_FancyMenu() throws IOException;

}
