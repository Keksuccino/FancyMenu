//TODO übernehmen
package de.keksuccino.fancymenu.mixin.client;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.function.BiFunction;

@Mixin(EditBox.class)
public interface IMixinEditBox {

    @Accessor("displayPos") int getDisplayPosFancyMenu();

    @Accessor("displayPos") void setDisplayPosFancyMenu(int displayPos);

    @Accessor("bordered") boolean getBorderedFancyMenu();

    @Accessor("maxLength") int getMaxLengthFancyMenu();

    @Accessor("formatter") BiFunction<String, Integer, FormattedCharSequence> getFormatterFancyMenu();

    @Accessor("shiftPressed") void setShiftPressedFancyMenu(boolean b);

    @Accessor("shiftPressed") boolean getShiftPressedFancyMenu();

    @Accessor("highlightPos") int getHighlightPosFancyMenu();

    @Invoker("deleteText") void invokeDeleteTextFancyMenu(int i);

    @Accessor("textColor") int getTextColorFancyMenu();

    @Accessor("textColorUneditable") int getTextColorUneditableFancyMenu();

    @Accessor("frame") int getFrameFancyMenu();

    @Accessor("hint") Component getHintFancyMenu();

    @Accessor("suggestion") String getSuggestionFancyMenu();

    @Invoker("renderHighlight") void invokeRenderHighlightFancyMenu(int xStart, int yStart, int xEnd, int yEnd);

}
