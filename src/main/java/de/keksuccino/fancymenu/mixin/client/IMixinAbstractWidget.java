package de.keksuccino.fancymenu.mixin.client;

import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

//TODO übernehmen 1.19.4 (neue klasse)
@Mixin(AbstractWidget.class)
public interface IMixinAbstractWidget {

    @Accessor("alpha") float getAlphaFancyMenu();

}
