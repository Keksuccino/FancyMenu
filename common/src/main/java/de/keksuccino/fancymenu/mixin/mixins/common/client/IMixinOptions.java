package de.keksuccino.fancymenu.mixin.mixins.common.client;

import net.minecraft.client.Options;
import net.minecraft.world.entity.player.PlayerModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import java.util.Set;

@Mixin(Options.class)
public interface IMixinOptions {

    @Accessor("modelParts") Set<PlayerModelPart> getModelPartsFancyMenu();

    @Invoker("processOptions") void invokeProcessOptionsFancyMenu(Options.FieldAccess fieldAccess);

}
