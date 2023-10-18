package de.keksuccino.drippyloadingscreen.mixin.mixins.client;

import de.keksuccino.drippyloadingscreen.mixin.MixinCache;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

//TODO übernehmen 2.1.1
@Mixin(Minecraft.class)
public class MixinMinecraft {

    @Inject(method = "runTick", at = @At("HEAD"))
    private void onRunTick(boolean b, CallbackInfo info) {
        try {
            List<Runnable> l = new ArrayList<>();
            l.addAll(MixinCache.gameThreadRunnables);
            for (Runnable r : l) {
                try {
                    r.run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                MixinCache.gameThreadRunnables.remove(r);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
