package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.PipelineCache;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.pipeline.ShaderSource;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.global.SeamlessWorldLoadingHandler;
import de.keksuccino.fancymenu.util.rendering.EarlyShaderSource;
import de.keksuccino.fancymenu.util.rendering.GuiBlurRenderer;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.GlobalSettingsUniform;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {

    @Shadow @Final Minecraft minecraft;
    @Shadow @Final private GlobalSettingsUniform globalSettingsUniform;

    /**
     * @reason Vanilla creates the Globals uniform in the first render call, but atlas uploads and texture ticks draw with it too. When the
     * initial resource reload completes before the first frame (mod initialization in the Minecraft constructor can block the render thread
     * until all background reload work is done), these draws run without it: the dev-only uniform validation then fails the reload, which
     * removes all selected resource packs, or crashes the game with "Missing uniform Globals". The first frame overwrites these initial values.
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void after_init_FancyMenu(CallbackInfo info) {
        Options options = this.minecraft.options;
        this.globalSettingsUniform.update(this.minecraft.getWindow().getWidth(), this.minecraft.getWindow().getHeight(), options.glintStrength().get(), 0L, 0.0F, options.getMenuBackgroundBlurriness(), Vec3.ZERO, false);
    }

    @Inject(method = "processBlurEffect", at = @At("HEAD"), cancellable = true)
    private void head_processBlurEffect_FancyMenu(CallbackInfo info) {
        if (RenderingUtils.isVanillaMenuBlurringBlocked()) info.cancel();
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void before_render_FancyMenu(CallbackInfo info) {
        ScreenCustomization.onPreGameRenderTick();
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;render3dHud(Lnet/minecraft/client/renderer/state/level/CameraRenderState;Lnet/minecraft/client/renderer/state/level/PlayerRenderState;Lnet/minecraft/client/renderer/state/OptionsRenderState;Z)V", shift = At.Shift.BEFORE))
    private void before_render3dHud_FancyMenu(CallbackInfo info) {
        if (this.minecraft != null && this.minecraft.level != null) {
            SeamlessWorldLoadingHandler.captureFrameIfNeeded(this.minecraft.gameRenderer.mainRenderTarget());
        }
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;renderLevel()V", shift = At.Shift.AFTER))
    private void afterRenderLevel_FancyMenu(CallbackInfo info) {
        if (this.minecraft != null && this.minecraft.level != null) {
            SeamlessWorldLoadingHandler.captureFrameIfNeeded(this.minecraft.gameRenderer.mainRenderTarget());
        }
    }

    @Inject(method = "close", at = @At("TAIL"))
    private void after_close_FancyMenu(CallbackInfo info) {
        GuiBlurRenderer.close_FancyMenu();
    }

    /** @reason The fallback pipeline cache only reads the vanilla pack, but FancyMenu's GUI pipelines can get compiled through it before the first resource reload applied the shader manager. */
    @WrapOperation(method = "preloadUiShader", at = @At(value = "NEW", target = "(Lcom/mojang/renderpearl/api/device/GpuDevice;Lcom/mojang/renderpearl/api/pipeline/ShaderSource;)Lcom/mojang/blaze3d/pipeline/PipelineCache;"))
    private static PipelineCache wrap_newFallbackPipelineCache_FancyMenu(GpuDevice device, ShaderSource shaderSource, Operation<PipelineCache> original) {
        return original.call(device, EarlyShaderSource.forClientResources(shaderSource));
    }

}
