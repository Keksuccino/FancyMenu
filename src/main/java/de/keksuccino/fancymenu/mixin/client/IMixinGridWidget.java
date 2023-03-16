package de.keksuccino.fancymenu.mixin.client;

import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

//TODO übernehmen 1.19.4 (LÖSCHEN!)
@Mixin(GridLayout.class)
public interface IMixinGridWidget {
//
//    /**
//     * @return Children of the layout as LayoutElements. Every LayoutElement is an AbstractWidget.
//     */
//    @Accessor("children") List<LayoutElement> getChildrenFancyMenu();

}
