package de.keksuccino.fancymenu.rendering.ui.widget;

import de.keksuccino.konkrete.gui.content.AdvancedButton;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class Button extends AdvancedButton {

    public Button(int x, int y, int widthIn, int heightIn, @NotNull String label, @NotNull OnPress onPress) {
        super(x, y, widthIn, heightIn, label, onPress);
    }

    public Button(int x, int y, int widthIn, int heightIn, @NotNull String label, boolean handleClick, @NotNull OnPress onPress) {
        super(x, y, widthIn, heightIn, label, handleClick, onPress);
    }

    public Button(int x, int y, int widthIn, int heightIn, @NotNull Component label, @NotNull OnPress onPress) {
        super(x, y, widthIn, heightIn, "", onPress);
        this.setMessage(label);
    }

    public Button(int x, int y, int widthIn, int heightIn, @NotNull Component label, boolean handleClick, @NotNull OnPress onPress) {
        super(x, y, widthIn, heightIn, "", handleClick, onPress);
        this.setMessage(label);
    }

}
