package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.mojang.blaze3d.shaders.BlendMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = BlendMode.class)
public interface IMixinBlendMode {

    @Accessor("lastApplied")
    static BlendMode get_lastApplied_FancyMenu() {
        throw new AssertionError();
    }

    @Accessor("lastApplied")
    static void set_lastApplied_FancyMenu(BlendMode blendMode) {
        throw new AssertionError();
    }

}
