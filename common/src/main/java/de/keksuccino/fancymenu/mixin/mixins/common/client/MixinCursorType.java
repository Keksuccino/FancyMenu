package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.cursor.CursorType;
import de.keksuccino.fancymenu.util.rendering.ui.cursor.SdlCursorTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CursorType.class)
public class MixinCursorType {

    /** @reason Track vanilla cursors at Minecraft's factory because NeoForge does not transform SDL library classes. */
    @WrapOperation(method = "createStandardCursor", at = @At(value = "INVOKE", target = "Lorg/lwjgl/sdl/SDLMouse;SDL_CreateSystemCursor(I)J", remap = false))
    private static long wrap_createSystemCursor_FancyMenu(int shape, Operation<Long> original) {
        long cursor = original.call(shape);
        SdlCursorTracker.onCreateSystemCursor(shape, cursor);
        return cursor;
    }

}
