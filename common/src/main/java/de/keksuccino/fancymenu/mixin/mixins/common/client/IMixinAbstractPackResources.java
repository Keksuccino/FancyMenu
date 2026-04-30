package de.keksuccino.fancymenu.mixin.mixins.common.client;

import net.minecraft.server.packs.AbstractPackResources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.io.File;

@Mixin(AbstractPackResources.class)
public interface IMixinAbstractPackResources {

    @Accessor("file")
    File getFile_FancyMenu();

}
