package de.keksuccino.fancymenu.mixin.mixins.common.client;

import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.Map;
import java.util.UUID;

//TODO übernehmen
@Mixin(BossHealthOverlay.class)
public interface IMixinBossHealthOverlay {

    @Accessor("events") Map<UUID, LerpingBossEvent> get_events_FancyMenu();

}
