//TODO übernehmenn
package de.keksuccino.fancymenu.mixin.client;

import com.google.common.base.Predicate;
import net.minecraft.client.gui.GuiPageButtonList;
import net.minecraft.client.gui.GuiTextField;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GuiTextField.class)
public interface IMixinGuiTextField {

    @Accessor("lineScrollOffset") int getDisplayPosFancyMenu();

    @Accessor("lineScrollOffset") void setDisplayPosFancyMenu(int displayPos);

    @Accessor("text") void setValueFieldFancyMenu(String value);

    @Accessor("enableBackgroundDrawing") boolean getBorderedFancyMenu();

    @Accessor("maxStringLength") int getMaxLengthFancyMenu();

    @Accessor("guiResponder") GuiPageButtonList.GuiResponder getGuiResponderFancyMenu();

    @Accessor("validator") Predicate<String> getValidatorFancyMenu();

    @Accessor("selectionEnd") int getHighlightPosFancyMenu();

    @Accessor("selectionEnd") void setHighlightPosFancyMenu(int pos);

    @Accessor("cursorPosition") void setCursorPositionFieldFancyMenu(int pos);

    @Accessor("enabledColor") int getTextColorFancyMenu();

    @Accessor("disabledColor") int getTextColorUneditableFancyMenu();

    @Accessor("cursorCounter") int getFrameFancyMenu();

    @Accessor("cursorCounter") void setFrameFancyMenu(int frame);

    @Invoker("drawSelectionBox") void invokeRenderHighlightFancyMenu(int xStart, int yStart, int xEnd, int yEnd);

}
