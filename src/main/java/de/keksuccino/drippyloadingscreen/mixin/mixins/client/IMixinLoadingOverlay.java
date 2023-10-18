package de.keksuccino.drippyloadingscreen.mixin.mixins.client;

import net.minecraft.client.gui.screens.LoadingOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.IntSupplier;

@Mixin(LoadingOverlay.class)
public interface IMixinLoadingOverlay {

    @Accessor("BRAND_BACKGROUND")
    static IntSupplier getBrandBackgroundDrippy() {
        return null;
    }

    @Accessor("currentProgress") float getCurrentProgressDrippy();

}
