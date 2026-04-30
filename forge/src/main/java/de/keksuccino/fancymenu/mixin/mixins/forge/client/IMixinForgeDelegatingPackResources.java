package de.keksuccino.fancymenu.mixin.mixins.forge.client;

import net.minecraft.server.packs.PackResources;
import net.minecraftforge.resource.DelegatingPackResources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(value = DelegatingPackResources.class, remap = false)
public interface IMixinForgeDelegatingPackResources {

    @Accessor(value = "delegates", remap = false)
    List<PackResources> getDelegates_FancyMenu();

}
