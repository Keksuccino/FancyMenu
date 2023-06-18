package de.keksuccino.fancymenu.util.rendering.text.color;

import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class TextColorFormatter {

    protected final char code;
    protected final DrawableColor color;

    public TextColorFormatter(char code, @NotNull DrawableColor color) {
        Objects.requireNonNull(color);
        this.code = code;
        this.color = color;
    }

    public char getCode() {
        return this.code;
    }

    public String getCodeString() {
        return "" + this.code;
    }

    public DrawableColor getColor() {
        return this.color;
    }

    public Style getStyle() {
        return Style.EMPTY.withColor(this.getColor().getColorInt());
    }

}
