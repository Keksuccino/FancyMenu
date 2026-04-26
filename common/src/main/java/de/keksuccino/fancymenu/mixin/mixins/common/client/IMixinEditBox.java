package de.keksuccino.fancymenu.mixin.mixins.common.client;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.List;

@Mixin(EditBox.class)
public interface IMixinEditBox {

    @Accessor("isEditable") boolean getIsEditableFancyMenu();

    @Accessor("displayPos") int getDisplayPosFancyMenu();

    @Accessor("displayPos") void setDisplayPosFancyMenu(int displayPos);

    @Accessor("bordered") boolean getBorderedFancyMenu();

    @Accessor("maxLength") int getMaxLengthFancyMenu();

    @Accessor("formatters") List<EditBox.TextFormatter> getFormattersFancyMenu();

    @Accessor("highlightPos") int getHighlightPosFancyMenu();

    @Accessor("textColor") int getTextColorFancyMenu();

    @Accessor("textColorUneditable") int getTextColorUneditableFancyMenu();

    @Accessor("focusedTime") long getFocusedTimeFancyMenu();

    @Accessor("hint") Component getHintFancyMenu();

    @Accessor("suggestion") String getSuggestionFancyMenu();

    @Accessor("invertHighlightedTextColor") boolean getInvertHighlightedTextColorFancyMenu();

}
