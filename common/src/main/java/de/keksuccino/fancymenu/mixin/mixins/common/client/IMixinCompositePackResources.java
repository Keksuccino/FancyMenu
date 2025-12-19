package de.keksuccino.fancymenu.mixin.mixins.common.client;

import net.minecraft.server.packs.CompositePackResources;
import net.minecraft.server.packs.PackResources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(CompositePackResources.class)
public interface IMixinCompositePackResources {

    @Accessor("packResourcesStack")
    List<PackResources> getPackResourcesStack_FancyMenu();

    @Accessor("primaryPackResources")
    PackResources getPrimaryPackResources_FancyMenu();

}
