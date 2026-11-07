package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.SDLEventHandler;
import com.mojang.blaze3d.platform.Window;
import de.keksuccino.fancymenu.util.input.FileDragTracker;
import org.lwjgl.sdl.SDLEvents;
import org.lwjgl.sdl.SDL_Event;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SDLEventHandler.class)
public class MixinSDLEventHandler {

    /** @reason SDL starts every file drag over the window with this event, before any file gets dropped. */
    @Inject(method = "handleDropBeginEvent", at = @At("HEAD"))
    private void before_handleDropBeginEvent_FancyMenu(CallbackInfo info) {
        FileDragTracker.onDragBegin();
    }

    /** @reason SDL ends every file drag with this event, both when the files got dropped and when the drag left the window. */
    @Inject(method = "handleDropCompleteEvent", at = @At("HEAD"))
    private void before_handleDropCompleteEvent_FancyMenu(SDL_Event event, CallbackInfo info) {
        FileDragTracker.onDragEnd();
    }

    /** @reason SDL reports where files get dragged with an event that Minecraft passes on to the window without using it. */
    @WrapOperation(method = "pollEvents", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;handleEvent(Lorg/lwjgl/sdl/SDL_Event;)V"))
    private void wrap_handleEvent_FancyMenu(Window instance, SDL_Event event, Operation<Void> original) {
        if (event.type() == SDLEvents.SDL_EVENT_DROP_POSITION) {
            FileDragTracker.onDragPosition(event.drop().x(), event.drop().y());
        }
        original.call(instance, event);
    }

}
