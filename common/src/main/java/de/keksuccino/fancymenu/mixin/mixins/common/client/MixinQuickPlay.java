package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.customization.element.elements.audio.AudioElementBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.quickplay.QuickPlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(QuickPlay.class)
public abstract class MixinQuickPlay {

    @Inject(method = "joinSingleplayerWorld(Lnet/minecraft/client/Minecraft;Ljava/lang/String;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/worldselection/WorldOpenFlows;openWorld(Ljava/lang/String;Ljava/lang/Runnable;)V"))
    private static void beforeQuickPlayStarts(Minecraft minecraft, String levelName, CallbackInfo ci) {
        AudioElementBuilder.stopAllActiveAudios();
    }

}
