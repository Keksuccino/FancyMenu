package de.keksuccino.fancymenu.mixin.mixins.common.client;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface IMixinEntity {

    @Accessor("dimensions") void setDimensions_FancyMenu(EntityDimensions dimensions);

    @Accessor("position") void setPosition_FancyMenu(Vec3 position);

    @Final
    @Mutable
    @Accessor("entityData") void setEntityData_FancyMenu(SynchedEntityData entityData);

}
