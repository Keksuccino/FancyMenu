package de.keksuccino.fancymenu.mixin.mixins.client;

import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CommandSuggestions.SuggestionsList.class)
public interface IMixinSuggestionsList {

    @Accessor("offset") int getOffsetFancyMenu();

    @Accessor("lastMouse") Vec2 getLastMouseFancyMenu();
    @Accessor("lastMouse") void setLastMouseFancyMenu(Vec2 lastMouse);

    @Accessor("rect") Rect2i getRectFancyMenu();

    @Accessor("current") int getCurrentFancyMenu();

}
