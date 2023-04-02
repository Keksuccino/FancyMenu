package de.keksuccino.fancymenu.menu.fancy.helper.ui.compat;

import net.minecraft.util.ChatAllowedCharacters;

public class SharedConstants {

    public static boolean isAllowedChatCharacter(char character) {
        return ChatAllowedCharacters.isAllowedCharacter(character);
    }

    public static String filterText(String text) {
        return ChatAllowedCharacters.filterAllowedCharacters(text);
    }

}
