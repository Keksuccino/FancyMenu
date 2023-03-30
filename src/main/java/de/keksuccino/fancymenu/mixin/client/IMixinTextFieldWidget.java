//TODO übernehmenn
package de.keksuccino.fancymenu.mixin.client;

import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.util.IReorderingProcessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.function.BiFunction;

@Mixin(TextFieldWidget.class)
public interface IMixinTextFieldWidget {

    @Accessor("displayPos") int getDisplayPosFancyMenu();

    @Accessor("displayPos") void setDisplayPosFancyMenu(int displayPos);

    @Accessor("bordered") boolean getBorderedFancyMenu();

    @Accessor("maxLength") int getMaxLengthFancyMenu();

    @Accessor("formatter") BiFunction<String, Integer, IReorderingProcessor> getFormatterFancyMenu();

    @Accessor("shiftPressed") void setShiftPressedFancyMenu(boolean b);

    @Accessor("shiftPressed") boolean getShiftPressedFancyMenu();

    @Accessor("highlightPos") int getHighlightPosFancyMenu();

    @Invoker("deleteText") void invokeDeleteTextFancyMenu(int i);

    @Accessor("textColor") int getTextColorFancyMenu();

    @Accessor("textColorUneditable") int getTextColorUneditableFancyMenu();

    @Accessor("frame") int getFrameFancyMenu();

    @Accessor("suggestion") String getSuggestionFancyMenu();

    @Invoker("renderHighlight") void invokeRenderHighlightFancyMenu(int xStart, int yStart, int xEnd, int yEnd);

}
