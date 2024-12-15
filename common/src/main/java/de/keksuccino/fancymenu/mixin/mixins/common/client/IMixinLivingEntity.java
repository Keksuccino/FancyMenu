package de.keksuccino.fancymenu.mixin.mixins.common.client;

import net.minecraft.world.entity.ElytraAnimationState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.WalkAnimationState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface IMixinLivingEntity {

    @Final
    @Mutable
    @Accessor("walkAnimation") void setWalkAnimation_FancyMenu(WalkAnimationState state);

    @Final
    @Mutable
    @Accessor("elytraAnimationState") void setElytraAnimationState_FancyMenu(ElytraAnimationState state);

}
