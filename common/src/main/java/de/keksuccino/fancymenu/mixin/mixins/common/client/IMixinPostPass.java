package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.mojang.blaze3d.buffers.GpuBuffer;
import net.minecraft.client.renderer.PostPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(PostPass.class)
public interface IMixinPostPass {

    @Accessor("customUniforms")
    Map<String, GpuBuffer> get_customUniforms_FancyMenu();

}
