package de.keksuccino.fancymenu.mixin.mixins.common.client;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerModel.class)
public interface IMixinPlayerModel {

    //@Accessor("cloak") ModelPart getCloakFancyMenu();

}
