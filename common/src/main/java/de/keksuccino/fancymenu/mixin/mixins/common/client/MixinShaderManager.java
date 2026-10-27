package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.shaders.ShaderType;
import de.keksuccino.fancymenu.util.rendering.EarlyShaderSource;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ShaderManager.class)
public class MixinShaderManager {

    /**
     * @reason The GPU device compiles pipelines with these shader sources, which stay empty until the first resource reload applied them,
     * but FancyMenu's GUI pipelines can get compiled before that. After the reload, only shaders that don't exist at all reach the fallback.
     */
    @ModifyReturnValue(method = "getShader", at = @At("RETURN"))
    private @Nullable String after_getShader_FancyMenu(@Nullable String source, Identifier id, ShaderType type) {
        return (source != null) ? source : EarlyShaderSource.CLIENT_RESOURCES.get(id, type);
    }

}
