package de.keksuccino.fancymenu.mixin.mixins.common.client;

import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import java.util.List;

@Mixin(Screen.class)
public interface IMixinScreen {

    @Accessor("children") List<GuiEventListener> getChildrenFancyMenu();

    @Accessor("renderables") List<Renderable> getRenderablesFancyMenu();

    @Accessor("narratables") List<NarratableEntry> getNarratablesFancyMenu();

    @Invoker("removeWidget") void invokeRemoveWidgetFancyMenu(GuiEventListener widget);

    @Accessor("initialized") boolean get_initialized_FancyMenu();

    @Invoker("clearFocus") void invoke_clearFocus_FancyMenu();

}
