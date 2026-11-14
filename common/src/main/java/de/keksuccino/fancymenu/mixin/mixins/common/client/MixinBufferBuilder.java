package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import de.keksuccino.fancymenu.util.rendering.scissor.ScissorShapes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BufferBuilder.class)
public class MixinBufferBuilder {

    /** @reason Cuts GUI meshes rendered in a shaped scissor (like the scissor of a rotated element) to the shape, see {@link ScissorShapes}. */
    @Inject(method = "build", at = @At("HEAD"))
    private void before_build_FancyMenu(CallbackInfoReturnable<MeshData> info) {
        ScissorShapes.clipToActiveShape((BufferBuilder)(Object)this);
    }

}
