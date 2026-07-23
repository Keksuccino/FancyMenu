package de.keksuccino.fancymenu.mixin.mixins.common.client;

import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockModel.class)
public interface AccessorMixinBlockModel {

    @Accessor("parentLocation") @Nullable ResourceLocation getParentLocation_FancyMenu();

}
