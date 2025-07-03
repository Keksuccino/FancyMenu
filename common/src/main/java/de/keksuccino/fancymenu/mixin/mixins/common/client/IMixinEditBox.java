package de.keksuccino.fancymenu.mixin.mixins.common.client;

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

    @Accessor("highlightPos") int getHighlightPosFancyMenu();

    @Invoker("deleteText") void invokeDeleteTextFancyMenu(int i);

    @Accessor("textColor") int getTextColorFancyMenu();

    @Accessor("textColorUneditable") int getTextColorUneditableFancyMenu();

    @Accessor("focusedTime") long getFocusedTimeFancyMenu();

    @Accessor("hint") Component getHintFancyMenu();

    @Accessor("suggestion") String getSuggestionFancyMenu();

}
