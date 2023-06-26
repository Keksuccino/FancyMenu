
package de.keksuccino.fancymenu.mixin.mixins.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.function.BiFunction;

@Mixin(EditBox.class)
public interface IMixinEditBox {

    @Accessor("isEditable") boolean getIsEditableFancyMenu();

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

    @Invoker("renderHighlight") void invokeRenderHighlightFancyMenu(PoseStack matrix, int xStart, int yStart, int xEnd, int yEnd);

}
