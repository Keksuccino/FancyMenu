package de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry;

import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.ScrollArea;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class TextScrollAreaEntry extends ScrollAreaEntry {

    protected Component text;
    protected int textWidth;
    /** How far the text sits to the right of where it normally starts. */
    protected float textIndent = 0.0F;
    protected Consumer<TextScrollAreaEntry> onClickCallback;
    protected int textBaseColor = UIBase.getUITheme().ui_interface_generic_text_color.getColorInt();

    public TextScrollAreaEntry(ScrollArea parent, @NotNull Component text, @NotNull Consumer<TextScrollAreaEntry> onClick) {
        super(parent, 0, 14);
        this.onClickCallback = onClick;
        this.setText(text);
    }

    @Override
    public void renderEntry(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        float centerY = this.getY() + (this.getHeight() / 2f);
        UIBase.renderText(graphics, this.text, this.getX() + 5f + this.textIndent, centerY - (UIBase.getUITextHeightNormal() / 2f), this.textBaseColor);
    }

    @Override
    public void onClick(ScrollAreaEntry entry, double mouseX, double mouseY, int button) {
        this.onClickCallback.accept((TextScrollAreaEntry) entry);
    }

    public void setText(@NotNull Component text) {
        this.text = text;
        this.textWidth = (int)UIBase.getUITextWidthNormal(this.text);
        this.setWidth(5 + this.textIndent + this.textWidth + 5);
    }

    public Component getText() {
        return this.text;
    }

    public int getTextWidth() {
        return this.textWidth;
    }

    public float getTextIndent() {
        return this.textIndent;
    }

    /**
     * Moves the text to the right, like to indent the wrapped lines of a list item below the text of its first line.
     */
    public void setTextIndent(float textIndent) {
        this.textIndent = Math.max(0.0F, textIndent);
        this.setText(this.text);
    }

    public int getTextBaseColor() {
        return textBaseColor;
    }

    public void setTextBaseColor(int textBaseColor) {
        this.textBaseColor = textBaseColor;
    }

}
