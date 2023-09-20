package de.keksuccino.fancymenu.mixin.mixins.common.client;

import net.minecraft.client.resources.language.ClientLanguage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.Map;

@Mixin(ClientLanguage.class)
public interface IMixinClientLanguage {

    @Accessor("storage") Map<String, String> getStorageFancyMenu();

}
