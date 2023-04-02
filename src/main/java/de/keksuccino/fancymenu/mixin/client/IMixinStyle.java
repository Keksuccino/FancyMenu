package de.keksuccino.fancymenu.mixin.client;

import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Style.class)
public interface IMixinStyle {

    @Accessor("parentStyle") Style getParentStyleFancyMenu();

    @Accessor("color") TextFormatting getColorFancyMenu();

    @Accessor("bold") Boolean getBoldFancyMenu();

    @Accessor("italic") Boolean getItalicFancyMenu();

    @Accessor("underlined") Boolean getUnderlinedFancyMenu();

    @Accessor("strikethrough") Boolean getStrikethroughFancyMenu();

    @Accessor("obfuscated") Boolean getObfuscatedFancyMenu();

    @Accessor("clickEvent") ClickEvent getClickEventFancyMenu();

    @Accessor("hoverEvent") HoverEvent getHoverEventFancyMenu();

    @Accessor("insertion") String getInsertionFancyMenu();

}
