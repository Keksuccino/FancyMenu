package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.util.rendering.entity.FancyPlayerWidgetBridge;
import it.crystalnest.fancy_entity_renderer.api.entity.player.mock.FancyPlayerMock;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Exposes FER's retained mock player state needed by FancyMenu's wrapper.
 */
@Pseudo
@Mixin(targets = "it.crystalnest.fancy_entity_renderer.api.entity.player.FancyPlayerWidget", remap = false)
public abstract class MixinFancyPlayerWidget implements FancyPlayerWidgetBridge {

    @Shadow(remap = false)
    @Final
    protected FancyPlayerMock player;

    @Override
    public void setCape_FancyMenu(@Nullable ResourceLocation cape) {
        this.player.cape = cape;
    }

}
