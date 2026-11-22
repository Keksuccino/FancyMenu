package de.keksuccino.fancymenu.mixin.mixins.common.client;

import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.Tickable;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.Map;
import java.util.Set;

@Mixin(TextureManager.class)
public interface AccessorMixinTextureManager {

    @Accessor("byPath") Map<ResourceLocation, AbstractTexture> get_byPath_FancyMenu();

    @Accessor("tickableTextures") Set<Tickable> get_tickableTextures_FancyMenu();

}
