package de.keksuccino.fancymenu.mixin.client;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.GridWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

//TODO übernehmen
@Mixin(GridWidget.class)
public interface IMixinGridWidget {

    @Invoker("getContainedChildren") List<? extends AbstractWidget> invokeGetContainedChildrenFancyMenu();

}
