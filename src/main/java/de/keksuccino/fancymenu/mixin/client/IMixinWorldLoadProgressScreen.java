package de.keksuccino.fancymenu.mixin.client;

import net.minecraft.client.gui.screen.WorldLoadProgressScreen;
import net.minecraft.world.chunk.listener.TrackingChunkStatusListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WorldLoadProgressScreen.class)
public interface IMixinWorldLoadProgressScreen {

    @Accessor("progressListener") public TrackingChunkStatusListener getProgressListenerFancyMenu();

    @Accessor("progressListener") public void setProgressListenerFancyMenu(TrackingChunkStatusListener l);

}
