package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import de.keksuccino.fancymenu.util.rendering.scissor.ScissorShapes;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BufferBuilder.class)
public abstract class MixinBufferBuilder {

    @Shadow private Vector3f[] sortingPoints;

    @Shadow protected abstract Vector3f[] makeQuadSortingPoints();

    /**
     * @reason Cuts GUI meshes rendered in a shaped scissor (like the scissor of a rotated element) to the shape, see {@link ScissorShapes}.
     * Quad sorting points get computed when sorting gets requested, before this, so they get computed again from the cut vertices.
     */
    @Inject(method = "storeRenderedBuffer", at = @At("HEAD"))
    private void before_storeRenderedBuffer_FancyMenu(CallbackInfoReturnable<BufferBuilder.RenderedBuffer> info) {
        if (ScissorShapes.clipToActiveShape((BufferBuilder)(Object)this) && (this.sortingPoints != null)) {
            this.sortingPoints = this.makeQuadSortingPoints();
        }
    }

}
